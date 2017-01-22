/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service

import no.ndla.articleapi.model.{api, domain}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.ArticleApiProperties.{externalApiUrls, resourceHtmlEmbedTag}
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}

import scala.collection.JavaConversions._

trait ReadService {
  this: ArticleRepository with ConverterService =>
  val readService: ReadService

  class ReadService {
    def withId(id: Long): Option[api.Article] =
      articleRepository.withId(id)
        .map(addUrlsOnEmbedResources)
        .map(converterService.toApiArticle)

    private[service] def addUrlsOnEmbedResources(article: domain.Article): domain.Article = {
      val articleWithUrls = article.content.map(addUrlOnResource)
      article.copy(content = articleWithUrls)
    }

    private[service] def addUrlOnResource(content: domain.ArticleContent): domain.ArticleContent = {
      val doc = stringToJsoupDocument(content.content)
      val resourceIdAttrName = Attributes.DataResource_Id.toString

      for (el <- doc.select(s"$resourceHtmlEmbedTag[$resourceIdAttrName]")) {
        val (resourceType, id) = (el.attr(s"${Attributes.DataResource}"), el.attr(resourceIdAttrName))
        el.removeAttr(resourceIdAttrName)
        el.attr(s"${Attributes.DataUrl}", s"${externalApiUrls(resourceType)}/$id")
      }

      content.copy(content = jsoupDocumentToString(doc))
    }

  }

}
