/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties.{NDLABrightcoveAccountId, NDLABrightcovePlayerId, NDLABrightcoveVideoScriptUrl}
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.ExtractConvertStoreContent
import no.ndla.articleapi.service.converters.HtmlTagGenerator

import scala.util.{Failure, Success, Try}

trait VideoConverterModule {
  this: HtmlTagGenerator with ExtractConvertStoreContent =>

  object VideoConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "video"

    override def convert(content: ContentBrowser, importStatus: ImportStatus): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val (linkText, nodeId) = (content.get("link_text"), content.get("nid"))

      val embedVideo = content.get("insertion") match {
        case "link" =>
          toVideoLink(linkText, nodeId) match {
            case Success(link) => link
            case Failure(e) => return Failure(e)
          }
        case _ => toInlineVideo(linkText, nodeId)
      }

      logger.info(s"Added video with nid ${content.get("nid")}")
      Success(embedVideo, Seq.empty, importStatus)
    }

    private def toVideoLink(linkText: String, nodeId: String): Try[String] = {
      extractConvertStoreContent.processNode(nodeId, ImportStatus.empty) match {
        case Success((content, _)) => Success(HtmlTagGenerator.buildContentLinkEmbedContent(content.id.get, linkText))
        case Failure(ex) => Failure(ex)
      }
    }

    def toInlineVideo(linkText: String, nodeId: String): String = {
      val embedVideoMeta = HtmlTagGenerator.buildBrightCoveEmbedContent(
        caption=linkText,
        videoId=s"ref:$nodeId",
        account=s"$NDLABrightcoveAccountId",
        player=s"$NDLABrightcovePlayerId")

      embedVideoMeta
    }

  }
}
