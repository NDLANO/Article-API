/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import io.lemonlabs.uri.{Path, Url}
import no.ndla.articleapi.ArticleApiProperties.externalApiUrls
import no.ndla.articleapi.caching.MemoizeAutoRenew
import no.ndla.articleapi.model.api
import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain.Language._
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.validation.EmbedTagRules.ResourceHtmlEmbedTag
import no.ndla.validation.HtmlTagRules.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.validation.{ResourceType, TagAttributes}
import org.jsoup.nodes.Element

import scala.jdk.CollectionConverters._
import scala.math.max
import scala.util.{Failure, Success, Try}

trait ReadService {
  this: ArticleRepository with ConverterService =>
  val readService: ReadService

  class ReadService {

    def getInternalIdByExternalId(externalId: Long): Option[api.ArticleIdV2] =
      articleRepository.getIdFromExternalId(externalId.toString).map(api.ArticleIdV2)

    def withIdV2(id: Long,
                 language: String,
                 fallback: Boolean = false,
                 revision: Option[Int] = None): Try[api.ArticleV2] = {
      val article = revision match {
        case Some(rev) => articleRepository.withIdAndRevision(id, rev)
        case None      => articleRepository.withId(id)
      }

      article.map(addUrlsOnEmbedResources) match {
        case None          => Failure(NotFoundException(s"The article with id $id was not found"))
        case Some(article) => converterService.toApiArticleV2(article, language, fallback)
      }
    }

    private[service] def addUrlsOnEmbedResources(article: Article): Article = {
      val articleWithUrls = article.content.map(content => content.copy(content = addUrlOnResource(content.content)))
      val visualElementWithUrls =
        article.visualElement.map(visual => visual.copy(resource = addUrlOnResource(visual.resource)))

      article.copy(content = articleWithUrls, visualElement = visualElementWithUrls)
    }

    def getNMostUsedTags(n: Int, language: String): Option[api.ArticleTag] = {
      val tagUsageMap = getTagUsageMap()
      val searchLanguage = getSearchLanguage(language, supportedLanguages)

      tagUsageMap
        .get(searchLanguage)
        .map(tags => api.ArticleTag(tags.getNMostFrequent(n), searchLanguage))
    }

    def getAllTags(input: String, pageSize: Int, offset: Int, language: String): api.TagsSearchResult = {
      val (tags, tagsCount) = articleRepository.getTags(input, pageSize, (offset - 1) * pageSize, language)
      converterService.toApiArticleTags(tags, tagsCount, pageSize, offset, language)
    }

    def getArticlesByPage(pageNo: Int, pageSize: Int, lang: String, fallback: Boolean = false): api.ArticleDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      val results = articleRepository
        .getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize)
        .flatMap(article => converterService.toApiArticleV2(article, lang, fallback).toOption)

      api.ArticleDump(articleRepository.articleCount, pageNo, pageSize, lang, results)
    }

    def getArticleDomainDump(pageNo: Int, pageSize: Int): api.ArticleDomainDump = {
      val (safePageNo, safePageSize) = (max(pageNo, 1), max(pageSize, 0))
      val results =
        articleRepository.getArticlesByPage(safePageSize, (safePageNo - 1) * safePageSize).map(addUrlsOnEmbedResources)

      api.ArticleDomainDump(articleRepository.articleCount, pageNo, pageSize, results)
    }

    val getTagUsageMap = MemoizeAutoRenew(() => {
      articleRepository.allTags
        .map(languageTags => languageTags.language -> new MostFrequentOccurencesList(languageTags.tags))
        .toMap
    })

    private[service] def addUrlOnResource(content: String): String = {
      val doc = stringToJsoupDocument(content)

      val embedTags = doc.select(s"$ResourceHtmlEmbedTag").asScala.toList
      embedTags.foreach(addUrlOnEmbedTag)
      jsoupDocumentToString(doc)
    }

    private def addUrlOnEmbedTag(embedTag: Element): Unit = {
      val typeAndPathOption = embedTag.attr(TagAttributes.DataResource.toString) match {
        case resourceType
            if resourceType == ResourceType.File.toString
              || resourceType == ResourceType.H5P.toString
                && embedTag.hasAttr(TagAttributes.DataPath.toString) =>
          val path = embedTag.attr(TagAttributes.DataPath.toString)
          Some((resourceType, path))

        case resourceType if embedTag.hasAttr(TagAttributes.DataResource_Id.toString) =>
          val id = embedTag.attr(TagAttributes.DataResource_Id.toString)
          Some((resourceType, id))
        case _ =>
          None
      }

      typeAndPathOption match {
        case Some((resourceType, path)) =>
          val baseUrl = Url.parse(externalApiUrls(resourceType))
          val pathParts = Path.parse(path).parts

          embedTag.attr(
            s"${TagAttributes.DataUrl}",
            baseUrl.addPathParts(pathParts).toString
          )
        case _ =>
      }
    }

    def getRevisions(articleId: Long): Try[Seq[Int]] = {
      articleRepository.getRevisions(articleId) match {
        case Nil       => Failure(NotFoundException(s"Could not find any revisions for article with id $articleId"))
        case revisions => Success(revisions)
      }
    }

    class MostFrequentOccurencesList(list: Seq[String]) {
      // Create a map where the key is a list entry, and the value is the number of occurences of this entry in the list
      private[this] val listToNumOccurencesMap: Map[String, Int] = list.groupBy(identity).view.mapValues(_.size).toMap
      // Create an inverse of the map 'listToNumOccurencesMap': the key is number of occurences, and the value is a list of all entries that occured that many times
      private[this] val numOccurencesToListMap: Map[Int, Set[String]] =
        listToNumOccurencesMap.groupBy(x => x._2).view.mapValues(_.keySet).toMap
      // Build a list sorted by the most frequent words to the least frequent words
      private[this] val mostFrequentOccorencesDec = numOccurencesToListMap.keys.toSeq.sorted
        .foldRight(Seq[String]())((current, result) => result ++ numOccurencesToListMap(current))

      def getNMostFrequent(n: Int): Seq[String] = mostFrequentOccorencesDec.slice(0, n)
    }

    def getContentByExternalId(externalId: String): Option[Content] =
      articleRepository.withExternalId(externalId)

    def getArticleIdByExternalId(externalId: String): Option[Long] =
      articleRepository.getIdFromExternalId(externalId)

    def getArticleIdsByExternalId(externalId: String): Option[api.ArticleIds] =
      articleRepository.getArticleIdsFromExternalId(externalId).map(converterService.toApiArticleIds)
  }

}
