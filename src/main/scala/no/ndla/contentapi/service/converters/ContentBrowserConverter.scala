package no.ndla.contentapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.ConverterModule
import no.ndla.contentapi.model.RequiredLibrary
import no.ndla.contentapi.service.ExtractServiceComponent
import org.jsoup.nodes.Element

import scala.collection.mutable.ListBuffer


trait ContentBrowserConverter {
  this: ExtractServiceComponent =>

  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule with LazyLogging {
    case class ContentBrowser(contentBrowserString: String) {
      // Extract the contentbrowser variables
      private val Pattern = """(?s).*\[contentbrowser (.*) contentbrowser\].*""".r
      private val ContentField = contentBrowserString match {
        case Pattern(group) => group
        case _ => ""
      }

      // Extract every key-value pair and build a map
      private val KeyVal = ContentField.split("==").map(x => x.stripPrefix("=").split("="))
      private val FieldMap = KeyVal.map(el => el(0) -> (if (el.length > 1) el(1) else "")).toMap

      def isContentBrowserField(): Boolean = {
        contentBrowserString.matches(Pattern.toString)
      }

      def getStartEndIndex(): (Int, Int) = {
        val (startIdf, endIdf) = ("[contentbrowser ", " contentbrowser]")
        val a = contentBrowserString.indexOf(ContentField)
        (a - startIdf.length(), a + ContentField.length() + endIdf.length())
      }

      def get(key: String): String = {
        FieldMap.get(key).get
      }
    }

    def convert(el: Element)(implicit requiredLibraries: ListBuffer[RequiredLibrary]): Element = {
      var isContentBrowserField = false

      do {
        val text = el.html()
        val cont = ContentBrowser(text)

        isContentBrowserField = cont.isContentBrowserField()
        if (isContentBrowserField) {
          val (start, end) = cont.getStartEndIndex()
          val nodeId = cont.get("nid")
          val newContent = extractService.getNodeType(cont.get("nid")) match {
            case Some("h5p_content") => {
              requiredLibraries find {el => el.name == "H5P-Resizer"} match {
                case None => requiredLibraries += RequiredLibrary("text/javascript", "H5P-Resizer", "http://ndla.no/sites/all/modules/h5p/library/js/h5p-resizer.js")
                case _ =>
              }
              s"""<embed data-oembed="http://ndla.no/h5p/embed/${nodeId}" />"""
            }
            case _ => {
              logger.warn("ContentBrowserConverter: Unsupported content ({})", nodeId)
              s"{Unsupported content: ${nodeId}}"
            }
          }

          el.html(text.substring(0, start) + newContent+ text.substring(end))
        }
      } while (isContentBrowserField)
      el
    }
  }
}
