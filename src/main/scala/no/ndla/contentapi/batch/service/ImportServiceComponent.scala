package no.ndla.contentapi.batch.service

import no.ndla.contentapi.batch.Node
import no.ndla.contentapi.batch.service.integration.CMDataComponent

trait ImportServiceComponent {
  this: CMDataComponent =>

  val importService: ImportService

  class ImportService {

    def importNode(nodeId: String): Node = {
      return cmData.getNode(nodeId);
    }
  }
}
