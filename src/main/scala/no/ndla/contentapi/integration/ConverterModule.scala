package no.ndla.contentapi.integration

import no.ndla.contentapi.model.{Content, ContentInformation, ImportStatus, RequiredLibrary}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode


trait ConverterModule {
  def stringToJsoupDocument(htmlString: String): Element = {
    val document = Jsoup.parseBodyFragment(htmlString)
    document.outputSettings().escapeMode(EscapeMode.xhtml)
    val article = document.select("article")
    val content = article.isEmpty match {
      case false => article
      case true => document.select("body")
    }
    content.first()
  }

  def jsoupDocumentToString(element: Element): String = {
    val article = element.select("article")
    val content = article.isEmpty match {
      case false => article
      case true => element.select("body")
    }

    content.outerHtml()
  }

  def convert(content: LanguageContent): (LanguageContent, ImportStatus)

  def convert(contentInformation: NodeToConvert, importStatus: ImportStatus): (NodeToConvert, ImportStatus) = {
    val (convertedContent, importStatuses) = contentInformation.contents.map(x => convert(x)).unzip
    val finalImportStatuses = ImportStatus(importStatuses.flatMap(is => is.messages) ++ importStatus.messages)  // Slå sammen importStatus
    (contentInformation.copy(contents=convertedContent), finalImportStatuses)
  }
}

case class LanguageContent(nid: String, tnid: String, content: String, language: Option[String], requiredLibraries: Seq[RequiredLibrary] = List[RequiredLibrary](), containsIngress: Boolean = false) {
  def isMainNode = (nid == tnid || tnid == "0")
  def isTranslation = !isMainNode

  def asContent: Content = Content(content, language)
}
