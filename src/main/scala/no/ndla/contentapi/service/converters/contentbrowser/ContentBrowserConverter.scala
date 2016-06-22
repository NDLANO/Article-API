package no.ndla.contentapi.service.converters.contentbrowser

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.integration.{ConverterModule, LanguageContent}
import no.ndla.contentapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.contentapi.service.ExtractServiceComponent

trait ContentBrowserConverter {
  this: ExtractServiceComponent with ImageConverterModule with LenkeConverterModule with H5PConverterModule with OppgaveConverterModule with FagstoffConverterModule with AudioConverterModule =>
  val contentBrowserConverter: ContentBrowserConverter

  class ContentBrowserConverter extends ConverterModule with LazyLogging {
    private val contentBrowserModules = Map[String, ContentBrowserConverterModule](
      ImageConverter.typeName -> ImageConverter,
      H5PConverter.typeName -> H5PConverter,
      LenkeConverter.typeName -> LenkeConverter,
      OppgaveConverter.typeName -> OppgaveConverter,
      FagstoffConverter.typeName -> FagstoffConverter,
      AudioConverter.typeName -> AudioConverter)

    def convert(content: LanguageContent): (LanguageContent, ImportStatus) = {
      val element = stringToJsoupDocument(content.content)
      var isContentBrowserField = false
      var requiredLibraries = List[RequiredLibrary]()
      var importStatus = ImportStatus()

      do {
        val text = element.html()
        val cont = ContentBrowser(text, content.language)

        isContentBrowserField = cont.isContentBrowserField()
        if (isContentBrowserField) {
          val nodeType = extractService.getNodeType(cont.get("nid")).getOrElse("UNKNOWN")

          val (newContent, reqLibs, messages) = contentBrowserModules.get(nodeType) match {
            case Some(module) => module.convert(cont)
            case None => {
              val errorString = s"{Unsupported content ${nodeType}: ${cont.get("nid")}}"
              logger.warn(errorString)
              (errorString, List[RequiredLibrary](), List(errorString))
            }
          }
          requiredLibraries = requiredLibraries ++ reqLibs
          importStatus = ImportStatus(importStatus.messages ++ messages)

          val (start, end) = cont.getStartEndIndex()
          element.html(text.substring(0, start) + newContent + text.substring(end))
        }
      } while (isContentBrowserField)

      (content.copy(content=jsoupDocumentToString(element), requiredLibraries=requiredLibraries), importStatus)
    }
  }
}
