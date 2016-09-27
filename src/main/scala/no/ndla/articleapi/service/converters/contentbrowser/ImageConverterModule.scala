/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ImageApiServiceComponent
import no.ndla.articleapi.ArticleApiProperties.imageApiUrl

trait ImageConverterModule {
  this: ImageApiServiceComponent =>

  object ImageConverter extends ContentBrowserConverterModule with LazyLogging{
    override val typeName: String = "image"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val (replacement, errors) = getImage(content)
      logger.info(s"Converting image with nid ${content.get("nid")}")
      (replacement, List[RequiredLibrary](), ImportStatus(errors, visitedNodes))
    }

    def getImage(cont: ContentBrowser): (String, Seq[String]) = {
      val figureDataAttributes = Map(
        "size" -> cont.get("imagecache").toLowerCase,
        "alt" -> cont.get("alt"),
        "caption" -> cont.get("link_text"),
        "id" -> s"${cont.id}"
      )

      imageApiService.importOrGetMetaByExternId(cont.get("nid")) match {
        case Some(image) =>
          val dataAttributes = buildDataAttributesString(figureDataAttributes + ("url" -> s"$imageApiUrl/${image.id}"))
          (s"""<figure data-resource="image" $dataAttributes></figure>""", Seq())
        case None =>
          (s"<img src='stock.jpeg' alt='The image with id ${cont.get("nid")} was not not found' />",
            Seq(s"Image with id ${cont.get("nid")} was not found"))
      }
    }

    def buildDataAttributesString(figureDataAttributeMap: Map[String, String]): String = {
      figureDataAttributeMap.toList.map { case (key, value) => s"""data-$key="${value.trim}""""}.mkString(" ")
    }

  }
}
