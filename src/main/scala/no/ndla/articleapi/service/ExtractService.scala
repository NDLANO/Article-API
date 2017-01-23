/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.integration._
import no.ndla.articleapi.model.domain.{BiblioMeta, ContentFilMeta, NodeGeneralContent, NodeToConvert}

import scala.util.{Failure, Success, Try}

trait ExtractService {
  this: MigrationApiClient with TagsService =>

  val extractService: ExtractService

  class ExtractService extends LazyLogging {
    def getNodeData(nodeId: String): NodeToConvert = {
      val tagsForNode = tagsService.forContent(nodeId) match {
        case Failure(e) =>
          logger.warn(s"Could not import tags for node $nodeId", e)
          List()
        case Success(tags) => tags
      }

      migrationApiClient.getContentNodeData(nodeId) match {
        case Success(data) => data.asNodeToConvert(nodeId, tagsForNode)
        case Failure(ex) => throw ex
      }
    }

    def getNodeType(nodeId: String): Option[String] =
      migrationApiClient.getContentType(nodeId).map(x => x.nodeType).toOption

    def getNodeEmbedMeta(nodeId: String): Option[MigrationEmbedMeta] =
      migrationApiClient.getNodeEmbedData(nodeId).toOption

    def getNodeFilMeta(nodeId: String): Option[ContentFilMeta] =
      migrationApiClient.getFilMeta(nodeId).map(x => x.asContentFilMeta).toOption

    def getNodeGeneralContent(nodeId: String): Seq[NodeGeneralContent] = {
      val content = migrationApiClient.getNodeGeneralContent(nodeId).getOrElse(Seq()).map(x => x.asNodeGeneralContent)

      // make sure to return the content along with all its translations
      content.exists {x => x.isMainNode} match {
        case true => content
        case false => if (content.nonEmpty)
          migrationApiClient.getNodeGeneralContent(content.head.tnid).getOrElse(Seq()).map(x => x.asNodeGeneralContent)
        else
          content
      }
    }

    def getBiblioMeta(nodeId: String): Option[BiblioMeta] =
      migrationApiClient.getBiblioMeta(nodeId).map(x => x.asBiblioMeta).toOption
  }
}
