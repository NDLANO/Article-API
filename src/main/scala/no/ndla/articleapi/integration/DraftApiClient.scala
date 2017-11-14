/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.integration

import java.util.Date

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.api
import no.ndla.network.NdlaClient

import scala.util.{Success, Try}
import scalaj.http.{Http, HttpRequest}

trait DraftApiClient {
  this: NdlaClient =>
  val draftApiClient: DraftApiClient

  class DraftApiClient {
    private val draftApiInternEndpointURL = s"http://${ArticleApiProperties.DraftHost}/intern"
    private val draftApiGetAgreementEndpoint = s"http://${ArticleApiProperties.DraftHost}/draft-api/v1/agreements/:agreement_id"
    private val draftApiHealthEndpoint = s"http://${ArticleApiProperties.DraftHost}/health"


    def getAgreementCopyright(agreementId: Long): Option[api.Copyright] = {
      implicit val formats = org.json4s.DefaultFormats
      val request: HttpRequest = Http(s"$draftApiGetAgreementEndpoint".replace(":agreement_id", agreementId.toString))
      ndlaClient.fetch[Agreement](request).toOption match {
        case Some(a) => Some(a.copyright)
        case _ => None
      }
    }

    def isHealthy: Boolean = {
      Try(Http(draftApiHealthEndpoint).execute()) match {
        case Success(resp) => resp.isSuccess
        case _ => false
      }
    }
  }
}

case class Agreement(id: Long,
                     title: String,
                     content: String,
                     copyright: api.Copyright,
                     created: Date,
                     updated: Date,
                     updatedBy: String
                    )
