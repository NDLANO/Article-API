/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.service.{AttachmentStorageService, ExtractService}
import no.ndla.articleapi.integration.AudioApiClient
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.service.converters.HtmlTagGenerator

import scala.util.{Failure, Success, Try}

trait AudioConverterModule  {
  this: ExtractService with AttachmentStorageService with AudioApiClient with HtmlTagGenerator =>

  object AudioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "audio"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val nodeId = content.get("nid")
      logger.info(s"Converting audio with nid $nodeId")

      toAudio(nodeId) match {
        case Success(audioHtml) => Success((audioHtml, Seq.empty, ImportStatus(Seq(), visitedNodes)))
        case Failure(_) => Failure(ImportException(s"Failed to import audio with node id $nodeId"))
      }
    }

    def toAudio(nodeId: String): Try[String] = {
      audioApiClient.getOrImportAudio(nodeId).map(audioId => {
        HtmlTagGenerator.buildAudioEmbedContent(audioId.toString)
      })
    }

  }
}
