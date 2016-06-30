package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration.{ConverterModule, LanguageContent, NodeIngress}
import no.ndla.contentapi.model.ImportStatus
import no.ndla.contentapi.service.{ExtractServiceComponent, ImageApiServiceComponent}

trait IngressConverter {
  this: ExtractServiceComponent with ImageApiServiceComponent =>
  val ingressConverter: IngressConverter

  class IngressConverter extends ConverterModule {

    override def convert(content: LanguageContent): (LanguageContent, ImportStatus) = {
      if (content.containsIngress) {
        return (content, ImportStatus())
      }

      extractService.getNodeIngress(content.nid) match {
        case Some(ingress) => {
          ingress.ingressVisPaaSiden match {
            case 0 => (content.copy(containsIngress = true), ImportStatus())
            case _ => {
              val element = stringToJsoupDocument(content.content)
              val (ingressContent, status) = createIngress(ingress)
              element.prepend(ingressContent)
              (content.copy(content = jsoupDocumentToString(element), containsIngress = true), status)
            }
          }
        }
        case None => (content.copy(containsIngress = true), ImportStatus())
      }
    }

    private def createIngress(ingress: NodeIngress): (String, ImportStatus) = {
      val (imageTag, message) = ingress.imageNid match {
        case Some(imageId) => {
          imageApiService.importOrGetMetaByExternId(imageId) match {
            case Some(image) => (s"""<img src="/images/${image.images.full.get.url}" />""", ImportStatus())
            case None => {
              ("", ImportStatus(s"Image with id $imageId was not found"))
            }
          }
        }
        case None => ("", ImportStatus())
      }
      (s"<section>$imageTag ${ingress.content}</section>", message)
    }
  }
}
