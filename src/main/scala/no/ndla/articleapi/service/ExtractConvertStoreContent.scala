/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration.MigrationApiClient
import no.ndla.articleapi.model.api.{ImportException, NotFoundException}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.{ArticleRepository, ConceptRepository}
import no.ndla.articleapi.service.search.{IndexService, SearchIndexService}
import no.ndla.articleapi.ArticleApiProperties.supportedContentTypes
import no.ndla.articleapi.validation.ArticleValidator

import scala.util.{Failure, Success, Try}

trait ExtractConvertStoreContent {
  this: ExtractService
    with MigrationApiClient
    with ConverterService
    with ArticleRepository
    with ConceptRepository
    with SearchIndexService
    with IndexService
    with ArticleValidator =>

  val extractConvertStoreContent: ExtractConvertStoreContent

  class ExtractConvertStoreContent extends LazyLogging {
    def processNode(externalId: String, importStatus: ImportStatus = ImportStatus(Seq(), Seq())): Try[(Long, ImportStatus)] = {
      if (importStatus.visitedNodes.contains(externalId)) {
        return getMainNodeId(externalId).flatMap(mainNodeId => articleRepository.getIdFromExternalId(mainNodeId)) match {
          case Some(id) => Success(id, importStatus)
          case None => Failure(NotFoundException(s"Content with external id $externalId was not found"))
        }
      }

      val importedArticle = for {
        (node, mainNodeId) <- extract(externalId)
        (convertedContent, updatedImportStatus) <- convert(node, importStatus)
        _ <- articleValidator.validate(convertedContent)
        newId <- store(convertedContent, mainNodeId)
        _ <- indexContent(convertedContent, newId)
      } yield (newId, updatedImportStatus ++ ImportStatus(Seq(s"Successfully imported node $externalId: $newId")))

      if (importedArticle.isFailure) {
        deleteArticleByExternalId(externalId)
      }

      importedArticle
    }

    private def deleteArticleByExternalId(externalId: String) = {
      articleRepository.getIdFromExternalId(externalId).map(articleId => {
        logger.info(s"Deleting previously imported article (id=$articleId, external id=$externalId) from database because the article could not be re-imported")
        articleRepository.delete(articleId)
        indexService.deleteDocument(articleId)
      })
    }

    private def getMainNodeId(externalId: String): Option[String] = {
      extract(externalId) map { case (_, mainNodeId) => mainNodeId } toOption
    }

    private def extract(externalId: String): Try[(NodeToConvert, String)] = {
      val node = extractService.getNodeData(externalId)
      node.contents.find(_.isMainNode) match {
        case None => Failure(NotFoundException(s"$externalId is a translation; Could not find main node"))
        case Some(mainNode) =>
          if (supportedContentTypes.contains(node.nodeType.toLowerCase) || supportedContentTypes.contains(node.contentType.toLowerCase))
            Success(node, mainNode.nid)
          else
            Failure(ImportException(s"Tried to import node of unsupported type '${node.nodeType.toLowerCase}/${node.contentType.toLowerCase()}'"))
      }
    }

    private def convert(nodeToConvert: NodeToConvert, importStatus: ImportStatus): Try[(Content, ImportStatus)] =
      converterService.toDomainArticle(nodeToConvert, importStatus)

    private def store(content: Content, mainNodeId: String): Try[Long] = {
      content match {
        case article: Article => storeArticle(article, mainNodeId)
        case concept: Concept => storeConcept(concept, mainNodeId)
      }
    }

    private def storeArticle(article: Article, mainNodeNid: String): Try[Long] = {
      val subjectIds = getSubjectIds(mainNodeNid)
      articleRepository.exists(mainNodeNid) match {
        case true => articleRepository.updateWithExternalId(article, mainNodeNid)
        case false => Try(articleRepository.insertWithExternalIds(article, mainNodeNid, subjectIds))
      }
    }

    private def storeConcept(concept: Concept, mainNodeNid: String): Try[Long] = {
      articleRepository.exists(mainNodeNid) match {
        case true => conceptRepository.updateWithExternalId(concept, mainNodeNid)
        case false => Try(conceptRepository.insertWithExternalId(concept, mainNodeNid))
      }
    }

    private def indexContent(content: Content, newId: Long): Try[Content] = {
      content match {
        case a: Article => searchIndexService.indexDocument(a.copy(id = Some(newId)))
        case c: Content => Failure(new NotImplementedError)
      }
    }

    private def getSubjectIds(nodeId: String): Seq[String] =
      migrationApiClient.getSubjectForNode(nodeId) match {
        case Failure(ex) => Seq()
        case Success(subjectMetas) => subjectMetas.map(_.nid)
      }

  }
}
