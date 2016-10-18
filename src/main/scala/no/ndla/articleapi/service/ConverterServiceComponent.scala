/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.maxConvertionRounds
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.articleapi.model._

import scala.annotation.tailrec

trait ConverterServiceComponent {
  this: ConverterModules with ExtractConvertStoreContent with ImageApiClient =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def toArticle(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (Article, ImportStatus) = {
      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)
      val (convertedContent, converterStatus) = convert(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))
      val (postProcessed, postProcessStatus) = postProcess(convertedContent, converterStatus)

      val (article, toArticleStatus) = toArticle(postProcessed)
      (article, postProcessStatus ++ toArticleStatus)
    }

    @tailrec private def convert(nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
      if (maxRoundsLeft == 0) {
        val message = "Maximum number of converter rounds reached; Some content might not be converted"
        logger.warn(message)
        return (nodeToConvert, importStatus.copy(messages=importStatus.messages :+ message))
      }

      val (updatedContent, updatedStatus) = executeConverterModules(nodeToConvert, importStatus)

      // If this converting round did not yield any changes to the content, this node is finished (case true)
      // If changes were made during this convertion, we run the converters again (case false)
      updatedContent == nodeToConvert match {
        case true => (updatedContent, updatedStatus)
        case false => convert(updatedContent, maxRoundsLeft - 1, updatedStatus)
      }
    }

    private def postProcess(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) =
      executePostprocessorModules(nodeToConvert, importStatus)

    private def toArticleIngress(nodeIngress: NodeIngressFromSeparateDBTable): Option[(ArticleIntroduction, ImportStatus)] = {
      val newImageId = nodeIngress.imageNid.flatMap(imageApiClient.importOrGetMetaByExternId).map(_.id)

      val importStatus = (nodeIngress.imageNid, newImageId) match {
        case (Some(imageNid), None) => ImportStatus(s"Failed to import ingress image with external id $imageNid", Seq())
        case _ => ImportStatus(Seq(), Seq())
      }

      nodeIngress.ingressVisPaaSiden == 1 match {
        case true => Some(ArticleIntroduction(nodeIngress.content, newImageId, nodeIngress.language), importStatus)
        case false => None
      }
    }

    private def toArticle(nodeToConvert: NodeToConvert): (Article, ImportStatus) = {
      val requiredLibraries = nodeToConvert.contents.flatMap(_.requiredLibraries).distinct
      val ingressesFromSeparateDatabaseTable = nodeToConvert.ingressesFromSeparateDBTable.flatMap(toArticleIngress)

      val (ingresses, ingressImportStatus) = ingressesFromSeparateDatabaseTable.isEmpty match {
        case false => ingressesFromSeparateDatabaseTable.unzip
        case true => (nodeToConvert.contents.flatMap(x => x.asArticleIntroduction), Seq())
      }

      (Article("0",
        nodeToConvert.titles,
        nodeToConvert.contents.map(_.asContent),
        nodeToConvert.copyright,
        nodeToConvert.tags,
        requiredLibraries,
        nodeToConvert.visualElements,
        ingresses,
        nodeToConvert.created,
        nodeToConvert.updated,
        nodeToConvert.contentType), ImportStatus(ingressImportStatus))
    }

  }
}
