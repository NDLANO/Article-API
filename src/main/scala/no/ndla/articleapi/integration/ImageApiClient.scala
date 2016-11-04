/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.domain.Copyright
import no.ndla.network.NdlaClient

import scalaj.http.{Http, HttpRequest}

trait ImageApiClient {
  this: NdlaClient =>
  val imageApiClient: ImageApiClient

  class ImageApiClient {
    private val imageApiInternEndpointURL = s"http://${ArticleApiProperties.internalImageApiUrl}/intern"
    private val imageApiImportImageURL = s"$imageApiInternEndpointURL/import/:external_id"
    private val imageApiGetByExternalIdURL = s"$imageApiInternEndpointURL/extern/:external_id"

    def getMetaByExternId(externId: String): Option[ImageMetaInformation] = {
      val request: HttpRequest = Http(s"$imageApiGetByExternalIdURL".replace(":external_id", externId))
      ndlaClient.fetch[ImageMetaInformation](request).toOption
    }

    def importImage(externId: String): Option[ImageMetaInformation] = {
      val second = 1000
      val request: HttpRequest = Http(s"$imageApiImportImageURL".replace(":external_id", externId)).timeout(15 * second, 15 * second).postForm
      ndlaClient.fetch[ImageMetaInformation](request).toOption
    }

    def importOrGetMetaByExternId(externId: String): Option[ImageMetaInformation] = {
      getMetaByExternId(externId) match {
        case None => importImage(externId)
        case Some(image) => Some(image)
      }
    }

  }
}

case class ImageMetaInformation(id:String, titles:List[ImageTitle], alttexts:List[ImageAltText], url:String, size:Int, contentType:String, copyright:Copyright, tags:List[ImageTag])
case class ImageTitle(title:String, language:Option[String])
case class ImageAltText(alttext:String, language:Option[String])
case class ImageTag(tags: Seq[String], language:Option[String])
