/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.service.converters

import no.ndla.articleapi.integration.{ConverterModule, LanguageContent}
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}
import no.ndla.articleapi.model.domain.ImportStatus
import org.jsoup.nodes.Element

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

object TableConverter extends ConverterModule {
  override def convert(content: LanguageContent, importStatus: ImportStatus): Try[(LanguageContent, ImportStatus)] = {
    val element = stringToJsoupDocument(content.content)

    stripParagraphTag(element)
    convertFirstTrToTh(element)

    Success(content.copy(content=jsoupDocumentToString(element)), importStatus)
  }

  def stripParagraphTag(el: Element) = {
    for (cell <- el.select("td").asScala) {
      val paragraphs = cell.select("p")
      if (paragraphs.size() == 1) {
        paragraphs.first.unwrap
      }
    }
  }

  def convertFirstTrToTh(el: Element) = {
    for (table <- el.select("table").asScala) {
      Option(table.select("tr").first).foreach(firstRow => {
        firstRow.select("td").tagName("th")
        firstRow.select("strong").unwrap()
      })
    }
  }

}
