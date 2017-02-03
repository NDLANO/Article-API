/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.integration.ImageApiClient
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.service.converters.HtmlTagGenerator

import scala.util.{Failure, Success, Try}

trait ImageConverterModule {
  this: ImageApiClient with HtmlTagGenerator =>

  object ImageConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "image"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")
      logger.info(s"Converting image with nid $nodeId")
      getImage(content).map(imageHtml => (imageHtml, Seq(), ImportStatus(Seq(), visitedNodes))) match {
        case Success(x) => Success(x)
        case Failure(_) => Failure(ImportException(s"Failed to import image with node id $nodeId"))
      }
    }

    def getImage(cont: ContentBrowser): Try[String] = {
      val alignment = getImageAlignment(cont)
      imageApiClient.importOrGetMetaByExternId(cont.get("nid")) match {
        case Some(image) => Success(HtmlTagGenerator.buildImageEmbedContent(
          caption=cont.get("link_text"),
          imageId=image.id,
          align=alignment.getOrElse(""),
          size=cont.get("imagecache").toLowerCase,
          altText=cont.get("alt")))
        case None =>
          Failure(ImportException(s"Failed to import image with ID ${cont.get("nid")}"))
      }
    }

    private def getImageAlignment(cont: ContentBrowser): Option[String] = {
      val marginCssClass = cont.get("css_class").split(" ").find(_.contains("margin"))
      val margin = marginCssClass.flatMap(margin => """contentbrowser_margin_(left|right)$""".r.findFirstMatchIn(margin).map(_.group(1)))

      // right margin = left alignment, left margin = right alignment
      margin match {
        case Some("right") => Some("left")
        case Some("left") => Some("right")
        case _ => None
      }
    }

  }
}
