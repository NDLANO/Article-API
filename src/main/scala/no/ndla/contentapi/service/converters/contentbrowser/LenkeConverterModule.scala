package no.ndla.contentapi.service.converters.contentbrowser

import no.ndla.contentapi.model.RequiredLibrary
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.service.ExtractServiceComponent

trait LenkeConverterModule {
  this: ExtractServiceComponent =>

  object LenkeConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "lenke"

    override def convert(content: ContentBrowser): (String, List[RequiredLibrary], List[String]) = {
      val (replacement, errors) = convertLink(content)
      (replacement, List[RequiredLibrary](), errors)
    }

    def convertLink(cont: ContentBrowser): (String, List[String]) = {
      var errors = List[String]()
      val (url, embedCode) = extractService.getNodeEmbedData(cont.get("nid")).get
      val NDLAPattern = """.*(ndla.no).*""".r

      url.host match {
        case NDLAPattern(_) => {
          errors = errors :+ s"(Warning) Link to NDLA resource '${url}'"
          logger.warn("Link to NDLA resource: '{}'", url)
        }
        case _ =>
      }

      val converted = cont.get("insertion") match {
        case "inline" => {
          // TODO: embed code from NDLAs DB is only used here for demo purposes. Should be switched out with a proper alternative
          embedCode
        }
        case "link" | "lightbox_large" => s"""<a href="${url}" title="${cont.get("link_title_text")}">${cont.get("link_text")}</a>"""
      }
      (converted, errors)
    }
  }
}
