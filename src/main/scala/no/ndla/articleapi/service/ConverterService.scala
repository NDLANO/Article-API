/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import scala.annotation.tailrec
import no.ndla.articleapi.ArticleApiProperties.maxConvertionRounds
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.model.{api, domain}
import no.ndla.mapping.License.getLicense

trait ConverterService {
  this: ConverterModules with ExtractConvertStoreContent with ImageApiClient =>
  val converterService: ConverterService

  class ConverterService extends LazyLogging {
    def toDomainArticle(nodeToConvert: NodeToConvert, importStatus: ImportStatus): (domain.Article, ImportStatus) = {
      val updatedVisitedNodes = importStatus.visitedNodes ++ nodeToConvert.contents.map(_.nid)
      val (convertedContent, converterStatus) = convert(nodeToConvert, maxConvertionRounds, importStatus.copy(visitedNodes = updatedVisitedNodes.distinct))
      val (postProcessed, postProcessStatus) = postProcess(convertedContent, converterStatus)

      (toDomainArticle(postProcessed), postProcessStatus)
    }

    @tailrec private def convert (nodeToConvert: NodeToConvert, maxRoundsLeft: Int, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
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


    private def toDomainArticle(nodeToConvert: NodeToConvert): (domain.Article) = {
      val requiredLibraries = nodeToConvert.contents.flatMap(_.requiredLibraries).distinct

      val ingresses = nodeToConvert.contents.flatMap(content => content.asArticleIntroduction)
      val metaDescriptions = nodeToConvert

      domain.Article(None,
        nodeToConvert.titles,
        nodeToConvert.contents.map(_.asContent),
        toDomainCopyright(nodeToConvert.license, nodeToConvert.authors),
        nodeToConvert.tags,
        requiredLibraries,
        nodeToConvert.visualElements,
        ingresses,
        nodeToConvert.contents.map(_.asArticleMetaDescription),
        nodeToConvert.created,
        nodeToConvert.updated,
        nodeToConvert.contentType)
    }

    private def toDomainCopyright(license: String, authors: Seq[Author]): Copyright = {
      val origin = authors.find(author => author.`type`.toLowerCase == "opphavsmann").map(_.name).getOrElse("")
      val authorsExcludingOrigin = authors.filterNot(x => x.name != origin && x.`type` == "opphavsmann")
      Copyright(license, origin, authorsExcludingOrigin)
    }

    def toApiArticle(article: domain.Article): api.Article = {
      api.Article(
        article.id.get.toString,
        article.title.map(toApiArticleTitle),
        article.content.map(toApiArticleContent),
        toApiCopyright(article.copyright),
        article.tags.map(toApiArticleTag),
        article.requiredLibraries.map(toApiRequiredLibrary),
        article.visualElement.map(toApiVisualElement),
        article.introduction.map(toApiArticleIntroduction),
        article.metaDescription.map(toApiArticleMetaDescription),
        article.created,
        article.updated,
        article.contentType
      )
    }

    def toApiArticleTitle(title: domain.ArticleTitle): api.ArticleTitle = {
      api.ArticleTitle(title.title, title.language)
    }

    def toApiArticleContent(content: domain.ArticleContent): api.ArticleContent = {
      api.ArticleContent(
        content.content,
        content.footNotes.map(_ map {case (key, value) => key -> toApiFootNoteItem(value)}),
        content.language)
    }

    def toApiFootNoteItem(footNote: domain.FootNoteItem): api.FootNoteItem = {
      api.FootNoteItem(footNote.title, footNote.`type`, footNote.year, footNote.edition, footNote.publisher, footNote.authors)
    }

    def toApiCopyright(copyright: domain.Copyright): api.Copyright = {
      api.Copyright(
        toApiLicense(copyright.license),
        copyright.origin,
        copyright.authors.map(toApiAuthor)
      )
    }

    def toApiLicense(shortLicense: String): api.License = {
      getLicense(shortLicense) match {
        case Some(l) => api.License(l.license, l.description, l.url)
        case None => api.License("unknown", "", None)
      }
    }

    def toApiAuthor(author: domain.Author): api.Author = {
      api.Author(author.`type`, author.name)
    }

    def toApiArticleTag(tag: domain.ArticleTag): api.ArticleTag = {
      api.ArticleTag(tag.tags, tag.language)
    }

    def toApiRequiredLibrary(required: domain.RequiredLibrary): api.RequiredLibrary = {
      api.RequiredLibrary(required.mediaType, required.name, required.url)
    }

    def toApiVisualElement(visual: domain.VisualElement): api.VisualElement = {
      api.VisualElement(visual.resource, visual.`type`, visual.language)
    }

    def toApiArticleIntroduction(intro: domain.ArticleIntroduction): api.ArticleIntroduction = {
      api.ArticleIntroduction(intro.introduction, intro.language)
    }

    def toApiArticleMetaDescription(metaDescription: domain.ArticleMetaDescription): api.ArticleMetaDescription= {
      api.ArticleMetaDescription(metaDescription.content, metaDescription.language)
    }

  }
}
