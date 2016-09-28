package no.ndla.articleapi.service.converters

import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.integration.LanguageContent
import no.ndla.articleapi.model.ImportStatus

class HTMLCleanerTest extends UnitSuite {
  val nodeId = "1234"

  test("That HTMLCleaner unwraps illegal attributes") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><h1 class="useless">heading<p style="width='0px'">test</p></h1></article>""", Some("en"))
    val expectedResult = "<article> <h1>heading<p>test</p></h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner unwraps illegal tags") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><h1>heading</h1><henriktag>hehe</henriktag></article>""", Some("en"))
    val expectedResult = "<article> <h1>heading</h1>hehe</article>"
    val (result, status) = HTMLCleaner.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("That HTMLCleaner removes comments") {
    val initialContent = LanguageContent(nodeId, nodeId, """<article><!-- this is a comment --><h1>heading<!-- comment --></h1></article>""", Some("en"))
    val expectedResult = "<article> <h1>heading</h1></article>"
    val (result, status) = HTMLCleaner.convert(initialContent, ImportStatus(Seq(), Seq()))

    result.content.replace("\n", "") should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }
}
