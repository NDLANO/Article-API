/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import java.util.Date

import no.ndla.articleapi.TestEnvironment
import no.ndla.articleapi.integration.{LanguageContent, LanguageIngress, MigrationRelatedContent, MigrationRelatedContents}
import no.ndla.articleapi.model._
import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.service.converters.HTMLCleaner
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.Try

class ConverterServiceTest extends UnitSuite with TestEnvironment {

  val service = new ConverterService
  val contentTitle = ArticleTitle("", Some(""))
  val license = License("licence", "description", Some("http://"))
  val author = Author("forfatter", "Henrik")
  val copyright = Copyright(license, "", List(author))
  val tag = ArticleTag(List("asdf"), Some("nb"))
  val visualElement = VisualElement("http://image-api/1", "image", Some("nb"))
  val requiredLibrary = RequiredLibrary("", "", "")
  val nodeId = "1234"
  val sampleAlt = "Fotografi"
  val sampleContentString = s"[contentbrowser ==nid=${nodeId}==imagecache=Fullbredde==width===alt=$sampleAlt==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion===link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"


  test("That the document is wrapped in an article tag") {
    val nodeId = "1"
    val initialContent = "<h1>Heading</h1>"
    val contentNode = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNode), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))
    val expedtedResult = initialContent

    when(extractConvertStoreContent.processNode("4321")).thenReturn(Try(1: Long, ImportStatus(Seq(), Seq())))

    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))
    val strippedResult = result.content.head.content.replace("\n", "").replace(" ", "")

    strippedResult should equal (expedtedResult)
  }

  test("That content embedded in a node is converted") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val altText = "Jente som spiser melom. Grønn bakgrunn, rød melon. Fotografi."
    val contentString = s"[contentbrowser ==nid=$nodeId==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val contentString2 = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=$altText==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=inline==link_title_text= ==link_text= ==text_align===css_class=contentbrowser contentbrowser]"
    val sampleOppgave1 = NodeGeneralContent(nodeId, nodeId, "Tittel", s"Innhold! $contentString2", "nb")
    val sampleOppgave2 = NodeGeneralContent(nodeId, nodeId2, "Tittel", "Enda mer innhold!", "nb")
    val initialContent = s"$contentString"
    val contentNode = LanguageContent(nodeId, nodeId, initialContent, Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNode), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))

    when(extractService.getNodeType(nodeId)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId)).thenReturn(Seq(sampleOppgave1))

    when(extractService.getNodeType(nodeId2)).thenReturn(Some("oppgave"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(sampleOppgave2))

    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))
    result.content.head.content should equal ("Innhold! Enda mer innhold!")
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That the ingress is not added to the content") {
    val (nodeId, nodeId2) = ("1234", "4321")
    val ingressNodeBokmal = NodeIngressFromSeparateDBTable("1", "1", "Hvem er sterkest?", None, 1, Some("nn"))
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, "Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste", Some("nb"))

    val ingressNodeNynorsk = NodeIngressFromSeparateDBTable("2", "2", "Kven er sterkast?", None, 1, Some("nn"))
    val contentNodeNynorsk = LanguageContent(nodeId2, nodeId, "Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste", Some("nn"))

    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal, contentNodeNynorsk), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))
    val bokmalExpectedResult = "Nordavinden og sola kranglet en gang om hvem av dem som var den sterkeste"
    val nynorskExpectedResult = "Nordavinden og sola krangla ein gong om kven av dei som var den sterkaste"

    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))
    val bokmalStrippedResult = " +".r.replaceAllIn(result.content.head.content, " ")
    val nynorskStrippedResult = " +".r.replaceAllIn(result.content.last.content, " ")

    bokmalStrippedResult should equal (bokmalExpectedResult)
    nynorskStrippedResult should equal (nynorskExpectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("ingress is extracted when wrapped in <p> tags") {
    val content =
      """<section>
        |<figure data-size="fullbredde" data-url="http://image-api/images/5359" data-align="" data-id="9" data-resource="image" data-alt="To personer" data-caption="capt."></figure>
        |<p><strong>Når man driver med medieproduksjon, er det mye arbeid som må gjøres<br /></strong></p>
        |</section>
        |<section> <p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</section>""".stripMargin
    val expectedContentResult = ArticleContent("""<section> <p>Det som kan gi helse- og sikkerhetsproblemer på en dataarbeidsplass, er:</p></section>""", None, Some("nb"))
    val expectedIngressResult = ArticleIntroduction("Når man driver med medieproduksjon, er det mye arbeid som må gjøres", Some("http://image-api/images/5359"), Some("nb"))

    val ingressNodeBokmal = NodeIngressFromSeparateDBTable("1", "1", "Hvem er sterkest?", None, 0, Some("nb"))
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, content, Some("nb"))

    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal), copyright, List(tag), Seq(visualElement), Seq(ingressNodeBokmal), "fagstoff", new Date(0), new Date(1))
    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))

    result.content.length should be (1)
    result.introduction.length should be (1)
    result.content.head should equal(expectedContentResult)
    result.introduction.head should equal(expectedIngressResult)
  }

  test("That html attributes are removed from the article") {
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, """<table class="testclass" data-resource="test"></table>""", Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))
    val bokmalExpectedResult = """<table data-resource="test"></table>"""

    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))
    val bokmalStrippedResult = " +".r.replaceAllIn(result.content.head.content, " ")

    bokmalStrippedResult should equal (bokmalExpectedResult)
    status.messages.nonEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That html comments are removed") {
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, """<p><!-- this is a comment -->not a comment</p> <!-- another comment -->""", Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))
    val expectedResult = """<p>not a comment</p>"""

    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))
    val strippedResult = " +".r.replaceAllIn(result.content.head.content, " ")

    strippedResult should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That images are converted") {
    val (nodeId, imageUrl, alt) = ("1234", "full.jpeg", "Fotografi")
    val newId = "1"
    val contentNode = LanguageContent(nodeId, nodeId, s"<article>$sampleContentString</article>", Some("en"))
    val node = NodeToConvert(List(contentTitle), List(contentNode), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))
    val imageMeta = ImageMetaInformation(newId, List(), List(), ImageVariants(Some(Image("small.jpeg", 128, "")), Some(Image(imageUrl, 256, ""))), Copyright(License("", "", Some("")), "", List()), List())
    val expectedResult =
      s"""|<article>
          |<figure data-size="fullbredde" data-url="http://localhost/images/$newId" data-align="" data-id="1" data-resource="image" data-alt="$sampleAlt" data-caption=""></figure>
          |</article>""".stripMargin.replace("\n", "")

    when(extractService.getNodeType(nodeId)).thenReturn(Some("image"))
    when(imageApiService.importOrGetMetaByExternId(nodeId)).thenReturn(Some(imageMeta))
    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))

    result.content.head.content should equal (expectedResult)
    result.requiredLibraries.length should equal (0)
  }

  test("&nbsp is removed") {
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, """<article> <p>hello&nbsp; you</article>""", Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))
    val expectedResult = """<article> <p>hello you</p></article>"""

    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))
    val strippedResult = " +".r.replaceAllIn(result.content.head.content.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

  test("That empty html tags are removed") {
    val contentNodeBokmal = LanguageContent(nodeId, nodeId, """<article> <div></div><p><div></div></p><figure data-id="1"></figure></article>""", Some("nb"))
    val node = NodeToConvert(List(contentTitle), List(contentNodeBokmal), copyright, List(tag), Seq(visualElement), Seq(), "fagstoff", new Date(0), new Date(1))
    val expectedResult = """<article> <figure data-id="1"></figure></article>"""

    val (result, status) = service.toArticle(node, ImportStatus(Seq(), Seq()))
    val strippedResult = " +".r.replaceAllIn(result.content.head.content.replace("\n", ""), " ")

    strippedResult should equal (expectedResult)
    status.messages.isEmpty should equal (true)
    result.requiredLibraries.isEmpty should equal (true)
  }

}
