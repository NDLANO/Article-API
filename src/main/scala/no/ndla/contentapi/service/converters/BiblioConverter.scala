package no.ndla.contentapi.service.converters

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.{ConverterModule, LanguageContent}
import no.ndla.contentapi.model.{FootNoteItem, ImportStatus}
import no.ndla.contentapi.service.ExtractServiceComponent
import org.jsoup.nodes.Element
import scala.collection.JavaConversions._

import scala.annotation.tailrec

trait BiblioConverter {
  this: ExtractServiceComponent =>
  val biblioConverter: BiblioConverter

  class BiblioConverter extends ConverterModule with LazyLogging {
    def convert(content: LanguageContent, importStatus: ImportStatus): (LanguageContent, ImportStatus) = {
      val element = stringToJsoupDocument(content.content)

      val references = buildReferences(element)
      val (map, messages) = references.isEmpty match {
        case true => return (content, importStatus)
        case false => buildReferenceMap(references)
      }

      val finalImportStatus = ImportStatus(importStatus.messages ++ messages, importStatus.visitedNodes)
      (content.copy(content=jsoupDocumentToString(element), footNotes=content.footNotes ++ map), finalImportStatus)
    }

    def buildReferences(element: Element): Seq[String] = {
      @tailrec def buildReferences(references: List[Element], referenceNodes: Seq[String], index: Int): Seq[String] = {
        if (references.isEmpty)
          return referenceNodes

        val id = references.head.id()
        val nodeId = id.substring(id.indexOf("-") + 1)

        references.head.removeAttr("id")
        references.head.attr("data-resource", "footnote")
        references.head.attr("data-key", s"ref_$index")
        references.head.html(s"<sup>$index</sup>")

        buildReferences(references.tail, referenceNodes :+ nodeId, index + 1)
      }

      buildReferences(element.select("a[id~=biblio-(.*)]").toList, List(), 1)
    }

    def buildReferenceMap(references: Seq[String]) = {
      @tailrec def buildReferenceMap(references: Seq[String], biblioMap: Map[String, FootNoteItem], errorList: Seq[String], index: Int): (Map[String, FootNoteItem], Seq[String]) = {
        if (references.isEmpty)
          return (biblioMap, errorList)

        buildReferenceItem(references.head) match {
          case (Some(fotNote)) => buildReferenceMap(references.tail, biblioMap + (s"ref_$index" -> fotNote), errorList, index + 1)
          case None => {
            val errorMessage = s"Could not find biblio with id ${references.head}"
            logger.warn(errorMessage)
            buildReferenceMap(references.tail, biblioMap, errorList :+ errorMessage, index + 1)
          }
        }
      }
      buildReferenceMap(references, Map(), List(), 1)
    }

    def buildReferenceItem(nodeId: String): Option[FootNoteItem] =
      extractService.getBiblio(nodeId).map(biblio => FootNoteItem(biblio, extractService.getBiblioAuthors(nodeId)))
  }
}


