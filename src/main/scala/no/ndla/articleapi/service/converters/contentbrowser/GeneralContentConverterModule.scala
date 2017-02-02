/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.api.ImportException
import no.ndla.articleapi.model.domain.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractService}

import scala.util.{Failure, Success, Try}

trait GeneralContentConverterModule {
  this: ExtractService with ArticleRepository with ExtractConvertStoreContent with HtmlTagGenerator =>

  abstract class GeneralContentConverter extends ContentBrowserConverterModule with LazyLogging {
    override def convert(contentBrowser: ContentBrowser, visitedNodes: Seq[String]): Try[(String, Seq[RequiredLibrary], ImportStatus)] = {
      val externalId = contentBrowser.get("nid")
      val contents = extractService.getNodeGeneralContent(externalId)

      contents.find(x => x.language == contentBrowser.language.getOrElse("")) match {
        case Some(content) =>
          insertContent(content.content, contentBrowser, visitedNodes) map { case (finalContent, status) =>
            (finalContent, Seq(), status)
          }
        case None =>
          Failure(ImportException(s"Failed to retrieve '$typeName' with language '${contentBrowser.language.getOrElse("")}' ($externalId)"))
      }
    }

    def insertContent(content: String, contentBrowser: ContentBrowser, visitedNodes: Seq[String]): Try[(String, ImportStatus)] = {
      val insertionMethod = contentBrowser.get("insertion")
      insertionMethod match {
        case "inline" => Success(content, ImportStatus(Seq(), visitedNodes))
        case "collapsed_body" =>
          Success(HtmlTagGenerator.buildDetailsSummaryContent(contentBrowser.get("link_text"), content), ImportStatus(Seq(), visitedNodes))
        case "link" => insertLink(contentBrowser, visitedNodes)
        case _ => {
          val warnMessage = s"""Unhandled insertion method '$insertionMethod' on '${contentBrowser.get("link_text")}'. Defaulting to link."""
          logger.warn(warnMessage)
          insertLink(contentBrowser, visitedNodes) map { case (insertString, importStatus) =>
            (insertString, ImportStatus(importStatus.messages :+ warnMessage, importStatus.visitedNodes))
          }
        }
      }
    }

    def insertLink(contentBrowser: ContentBrowser, visitedNodes: Seq[String]): Try[(String, ImportStatus)] = {
      getContentId(contentBrowser.get("nid"), visitedNodes) match {
        case Success((id, importStatus)) => {
          val embedContent = HtmlTagGenerator.buildLinkEmbedContent(id.toString, contentBrowser.get("link_text"))
          Success(s" $embedContent", importStatus)
        }
        case Failure(e) => Failure(e)
      }
    }

    def getContentId(externalId: String, visitedNodes: Seq[String]): Try[(Long, ImportStatus)] = {
      articleRepository.getIdFromExternalId(externalId) match {
        case Some(id) => Success(id, ImportStatus(Seq(), (visitedNodes :+ externalId).distinct))
        case None => extractConvertStoreContent.processNode(externalId, ImportStatus(Seq(), visitedNodes))
      }
    }

  }
}
