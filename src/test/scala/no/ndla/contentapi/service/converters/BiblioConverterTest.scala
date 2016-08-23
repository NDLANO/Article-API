package no.ndla.contentapi.service.converters

import no.ndla.contentapi.integration._
import no.ndla.contentapi.model.{FootNoteItem, ImportStatus}
import no.ndla.contentapi.{TestEnvironment, UnitSuite}
import org.mockito.Mockito._

class BiblioConverterTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val sampleBiblio = Biblio("The Catcher in the Rye", "Book", "1951", "1", "Little, Brown and Company")
  val sampleBiblioAuthors = Seq(BiblioAuthor("J. D. Salinger", "Salinger", "Jerome David"))

  test("That BiblioConverter initializes and builds a map of footnote items") {
    val initialContent = LanguageContent(nodeId, nodeId, s"""<article><a id="biblio-$nodeId"></a><h1>CONTENT</h1>more content</article>""", Some("en"))
    val expectedFootNotes = Map("ref_1" -> FootNoteItem(sampleBiblio, sampleBiblioAuthors))
    val expectedResult = initialContent.copy(content=s"""<article> <a data-resource="footnote" data-key="ref_1"><sup>1</sup></a> <h1>CONTENT</h1>more content</article>""", footNotes=Some(expectedFootNotes))

    when(extractService.getBiblio(nodeId)).thenReturn(Some(sampleBiblio))
    when(extractService.getBiblioAuthors(nodeId)).thenReturn(sampleBiblioAuthors)
    val (result, status) = biblioConverter.convert(initialContent, ImportStatus(Seq(), Seq()))
    val strippedContent = " +".r.replaceAllIn(result.content.replace("\n", ""), " ")

    result.copy(content=strippedContent) should equal (expectedResult)
    status.messages.isEmpty should be (true)
  }
}
