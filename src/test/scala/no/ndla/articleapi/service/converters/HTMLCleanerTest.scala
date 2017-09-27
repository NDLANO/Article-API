package no.ndla.articleapi.service.converters

import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.articleapi.integration.LanguageIngress
import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.model.domain.ImportStatus
import no.ndla.articleapi.integration.ConverterModule.{jsoupDocumentToString, stringToJsoupDocument}

import scala.util.Success

class HTMLCleanerTest extends UnitSuite with TestEnvironment {
  val nodeId = "1234"
  val defaultImportStatus = ImportStatus(Seq(), Seq())

  val defaultLanguageIngress = LanguageIngress("Jeg er en ingress", None)
  val defaultLanguageIngressWithHtml = LanguageIngress("<p>Jeg er en ingress</p>", None)

  test("embed tag should be an allowed tag and contain data attributes") {
    HTMLCleaner.isTagValid("embed")

    val dataAttrs = Attributes.values.map(_.toString).filter(x => x.startsWith("data-") && x != Attributes.DataType.toString).toSet
    val legalEmbedAttrs = HTMLCleaner.legalAttributesForTag("embed")

    dataAttrs.foreach(x => legalEmbedAttrs should contain(x))
  }

  test("That HTMLCleaner unwraps illegal attributes") {
    val initialContent = TestData.sampleContent.copy(content="""<body><h1 class="useless">heading<div style="width='0px'">hey</div></h1></body>""")
    val expectedResult = "<section><h1>heading<div>hey</div></h1></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That HTMLCleaner unwraps illegal tags") {
    val initialContent = TestData.sampleContent.copy(content="""<section><h1>heading</h1><henriktag><p>hehe</p></henriktag></section>""")
    val expectedResult = "<section><h1>heading</h1><p>hehe</p></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That HTMLCleaner removes comments") {
    val initialContent = TestData.sampleContent.copy(content="""<section><!-- this is a comment --><h1>heading<!-- comment --></h1></section>""")
    val expectedResult = "<section><h1>heading</h1></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("That HTMLCleaner removes empty p,div,section,aside tags") {
    val initialContent = TestData.sampleContent.copy(content="""<h1>not empty</h1><section><p></p><div></div><aside></aside></section>""")
    val expectedResult = "<section><h1>not empty</h1></section>"
    val Success((result, _)) = htmlCleaner.convert(initialContent, defaultImportStatus)

    result.content should equal (expectedResult)
    result.requiredLibraries.size should equal (0)
  }

  test("ingress is extracted when wrapped in <p> tags") {
    val content = s"""<section>
                   |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                   |<p><strong>Medievanene er i endring.</br></strong></p>
                   |</section>
                   |<section>
                   |<h2>Mediehverdagen</h2>
                   |</section>""".stripMargin.replace("\n", "")

    val expectedContentResult =
      s"""<section>
         |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |</section>
         |<section>
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = LanguageIngress("Medievanene er i endring.", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("ingress text is not extracted when not present") {
    val content = s"""<section>
                    |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                    |<h2>Mediehverdagen</h2>
                    |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
         |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
         |<h2>Mediehverdagen</h2>
         |</section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = None
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(expectedIngressResult)
    result.requiredLibraries.size should equal (0)
  }

  test("ingress with word count less than 3 should not be interpreted as an ingress") {
    val content = s"""<section>
                     |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                     |<p><strong>Medievanener<br></strong></p>
                     |</section>
                     |<section>
                     |<h2>Mediehverdagen</h2>
                     |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.content should equal(content)
    result.ingress should equal(None)
  }

  test("ingress image is not extracted when not present") {
    val content =
      """<section>
          |<p><strong>Du har sikkert opplevd rykter og usannheter</strong></p>
          |<ul>
          |<li><a href="#" title="Snopes">Snopes</a></li>
          |</ul>
        |</section>
      |""".stripMargin.replace("\n", "")
    val expectedContentResult = """<section><ul><li><a href="#" title="Snopes">Snopes</a></li></ul></section>"""
    val expectedIngressResult = LanguageIngress("Du har sikkert opplevd rykter og usannheter", None)
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)
    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.requiredLibraries.size should equal (0)
  }

  test("ingress text is extracted when wrapped in <strong> tags") {
    val content = s"""<section>
                    |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                    |<strong>Medievanene er i endring.</strong>
                    |<h2>Mediehverdagen</h2>
                    |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult =
      s"""<section>
        |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
        |<h2>Mediehverdagen</h2></section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = LanguageIngress("Medievanene er i endring.", None)
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("ingress should not be extracted if not located in the beginning of content") {
    val content = s"""<section>
                     |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                     |<h2>Mediehverdagen</h2>
                     |<p>A paragraph!</p>
                     |<p><strong>Medievanene er i endring.</strong><p>
                     |</section>""".stripMargin.replace("\n", "")
    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.ingress should be (None)
  }

  test("only the first paragraph should be extracted if strong") {
    val content = s"""<section><embed data-align="" data-alt="Hånd som tegner" data-caption="" data-resource="image" data-resource_id="200" data-size="fullbredde"/>
         |<p><strong>Når du skal jobbe med fotoutstilling, er det lurt å sette seg godt inn i tema for utstillingen og bestemme seg for hvilket uttrykk man
         |er ute etter å skape allerede i planleggingsfasen.
         |</strong></p>
         |<h2>Tips til aktuelle verktøy og bruk av verktøy</h2>
         |</section>""".stripMargin
    val expectedContentResult = s"""<section><embed data-align="" data-alt="Hånd som tegner" data-caption="" data-resource="image" data-resource_id="200" data-size="fullbredde">
      |<h2>Tips til aktuelle verktøy og bruk av verktøy</h2></section>""".stripMargin.replace("\n", "")
    val expectedIngressResult = LanguageIngress("Når du skal jobbe med fotoutstilling, er det lurt å sette seg godt inn i tema for utstillingen og bestemme seg for hvilket uttrykk man er ute etter å skape allerede i planleggingsfasen.", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("ingress inside a div should be extracted") {
    val nbsp = "\u00a0"
    val content =
      s"""<section><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>$nbsp</strong><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div></section>""".stripMargin
    val expectedContent =
      """<section><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |
        |</div></section>""".stripMargin
    val expectedIngress = Some(LanguageIngress("Du er et unikt individ, med en rekke personlige egenskaper.", None))

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.ingress should be (expectedIngress)
    result.content should be (expectedContent)
  }

  test("ingress inside a nested div should be extracted") {
    val content =
      """<section><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedContent =
      """<section><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedIngress = Some(LanguageIngress("Du er et unikt individ, med en rekke personlige egenskaper.", None))

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.ingress should be (expectedIngress)
    result.content should be (expectedContent)
  }

  test("ingres inside first paragraph should be extracted when also div is present") {
    val content =
      """<section><p><strong>correct ingress more than three words</strong></p><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedContent =
      """<section><div><div>
        |<embed data-align="" data-alt="To gutter" data-caption="" data-resource="image" data-resource_id="1234" data-size="fullbredde" data-url="http://ndla">
        |<p><strong>Du er et unikt individ, med en rekke personlige egenskaper.</strong></p>
        |</div><p>do not touch</p></div></section>""".stripMargin
    val expectedIngress = Some(LanguageIngress("correct ingress more than three words", None))

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.ingress should be (expectedIngress)
    result.content should be (expectedContent)
  }

  test("standalone text in a section is wrapped in <p> tags") {
    val content = s"""<section>
                      |Medievanene er i endring.
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = s"""<section>
                      |<p>Medievanene er i endring.</p>
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.content should equal(expectedContentResult)
  }

  test("standalone text and <em> tags in a section should be wrapped in <p> tags") {
    val origContent = """<section>
                        |<h3>De kursiverte ordene og ordforbindelsene i denne teksten er enten skrevet feil eller brukt feil. Hva er riktig?</h3>
                        |Det har <em>skjelden</em> vært så mange <em>tilstede</em> som <em>igår</em>.
                        |<h1>lolol</h1>
                        |should be wrapped
                        |</section>""".stripMargin.replace("\n", "")

    val expectedContent = """<section>
                            |<h3>De kursiverte ordene og ordforbindelsene i denne teksten er enten skrevet feil eller brukt feil. Hva er riktig?</h3>
                            |<p>
                            |Det har <em>skjelden</em> vært så mange <em>tilstede</em> som <em>igår</em>.
                            |</p>
                            |<h1>lolol</h1>
                            |<p>should be wrapped</p>
                            |</section>""".stripMargin.replace("\n", "")

    val Success((res, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=origContent), defaultImportStatus)
    res.content should equal(expectedContent)
  }

  test("That isAttributeKeyValid returns false for illegal attributes") {
    HTMLCleaner.isAttributeKeyValid("data-random-junk", "td") should equal (false)
  }

  test("That isAttributeKeyValid returns true for legal attributes") {
    HTMLCleaner.isAttributeKeyValid("align", "td") should equal (true)
  }

  test("That isTagValid returns false for illegal tags") {
    HTMLCleaner.isTagValid("yodawg") should equal (false)
  }

  test("That isTagValid returns true for legal attributes") {
    HTMLCleaner.isTagValid("section") should equal (true)
  }

  test("That HTMLCleaner do not insert ingress if already added from seperate table") {
    val content = s"""<section>
                      |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                      |<strong>Medievanene er i endring.</strong>
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = s"""<section>
          |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
          |<strong>Medievanene er i endring.</strong>
          |<h2>Mediehverdagen</h2>
          |</section>""".stripMargin.replace("\n", "")

    val notExpectedIngressResult = LanguageIngress("Medievanene er i endring.", None)
    val expectedIngressResult = LanguageIngress("Jeg er en ingress", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content, ingress=Some(defaultLanguageIngress)), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
    result.ingress should not equal Some(notExpectedIngressResult)

  }

  test("That HTMLCleaner removes all tags in ingress from seperate table") {
    val content = s"""<section>
                      |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                      |<strong>Medievanene er i endring.</strong>
                      |<h2>Mediehverdagen</h2>
                      |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = s"""<section>
                                    |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                                    |<strong>Medievanene er i endring.</strong>
                                    |<h2>Mediehverdagen</h2>
                                    |</section>""".stripMargin.replace("\n", "")

    val expectedIngressResult = LanguageIngress("Jeg er en ingress", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content, ingress=Some(defaultLanguageIngressWithHtml)), defaultImportStatus)

    result.content should equal(expectedContentResult)
    result.ingress should equal(Some(expectedIngressResult))
  }

  test("HTMLCleaner extracts two first string paragraphs as ingress") {
    val content = s"""<section>
                     |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                     |<p><strong>Medievanene er i endring.</strong></p>
                     |<p><strong>Er egentlig medievanene i endring</strong></p>
                     |<h2>Mediehverdagen</h2>
                     |</section>""".stripMargin.replace("\n", "")
    val expectedContentResult = s"""<section>
                                   |<$resourceHtmlEmbedTag data-size="fullbredde" data-url="http://image-api/images/5452" data-align="" data-resource="image" data-alt="Mobiltelefon sender SMS">
                                   |<h2>Mediehverdagen</h2>
                                   |</section>""".stripMargin.replace("\n", "")

    val expectedIngressResult = LanguageIngress("Medievanene er i endring. Er egentlig medievanene i endring", None)

    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=content), defaultImportStatus)

    result.ingress should equal(Some(expectedIngressResult))
    result.content should equal(expectedContentResult)
  }

  test("elements are replaced with data-caption text in meta description") {
    val content = TestData.sampleContent.copy(content="", metaDescription=s"""Look at this image <$resourceHtmlEmbedTag data-resource="image" data-caption="image caption">""")
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.metaDescription should equal ("Look at this image image caption")
  }

  test("an embed-image as the first element inside p tags are moved out of p tag") {
    val image1 = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img.jpg">"""
    val content = TestData.sampleContent.copy(content=s"""<section><p>${image1}sample text</p></section>""")

    val expectedResult = s"""<section>$image1<p>sample text</p></section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal (expectedResult)
  }

  test("an embed-image inside p tags are moved out of p tag") {
    val image1 = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img.jpg">"""
    val content = TestData.sampleContent.copy(content=s"""<section><p><br>${image1}sample text</p></section>""")

    val expectedResult = s"""<section>$image1<p><br>sample text</p></section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal (expectedResult)
  }

  test("an embed-image inside p tags with br are moved out of p tag and p tag is removed") {
    val image1 = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img.jpg">"""
    val content = TestData.sampleContent.copy(content=s"""<section><p><br />$image1</p></section>""")

    val expectedResult = s"""<section>$image1</section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal (expectedResult)
  }

  test("embed-images inside p tags are moved out of p tag and p is removed if empty") {
    val image1 = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img1.jpg">"""
    val image2 = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img2.jpg">"""
    val image3 = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img3.jpg">"""
    val content = TestData.sampleContent.copy(content=s"""<section><p>$image1$image2 $image3</p></section>""")

    val expectedResult = s"""<section>$image1$image2$image3</section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal (expectedResult)
  }

  test("embed-images inside nested p tags are moved out of p tag and p is removed if empty") {
    val image = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img1.jpg">"""
    val content = TestData.sampleContent.copy(content=s"""<section><p><strong>$image</strong></p></section>""")

    val expectedResult = s"""<section>$image</section>"""
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal (expectedResult)
  }

  test("p tags without images are left untouched") {
    val originalContent = """<section><p>sample text</p></section>"""
    val content = TestData.sampleContent.copy(content=originalContent)
    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.content should equal (originalContent)
  }

  test("moveMisplacedAsideTags should move aside tags located at the start of the article further down") {
    val image = s"""<$resourceHtmlEmbedTag data-resource="image" data-url="http://some.url.org/img1.jpg">"""
    val paragraph = "<p>sample text</p>"
    val aside = "<aside>This block should not be on top</aside>"

    val originalContent = s"""<section>$aside$image$paragraph</section>"""
    val expectedContent = s"""<section>$image$aside$paragraph</section>"""
    val Success((result1, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=originalContent), defaultImportStatus)
    result1.content should equal (expectedContent)

    val originalContent2 = s"""<section>$aside$image</section>"""
    val expectedContent2 = s"""<section>$image$aside</section>"""
    val Success((result2, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=originalContent2), defaultImportStatus)
    result2.content should equal (expectedContent2)

    val originalContent3 = s"""<section>$aside</section>"""
    val expectedContent3 = s"""<section>$aside</section>"""
    val Success((result3, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=originalContent3), defaultImportStatus)
    result3.content should equal (expectedContent3)
  }

  test("all content must be wrapped in sections") {
    val original = "<h1>hello</h1><p>content</p>"
    val content = TestData.sampleContent.copy(content=original)
    val expected = s"<section>$original</section>"

    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expected)
  }

  test("elements not inside a section should be moved to previous section") {
    val original =
      "<section>" +
        "<h1>hello</h1>" +
      "</section>" +
      "<p>here, have a content</p>" +
      "<p>you deserve it</p>" +
      "<section>" +
        "<p>outro</p>" +
      "</section>"
    val content = TestData.sampleContent.copy(content=original)
    val expected =
      "<section>" +
        "<h1>hello</h1>" +
        "<p>here, have a content</p>" +
        "<p>you deserve it</p>" +
      "</section>" +
      "<section>" +
        "<p>outro</p>" +
      "</section>"

    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.content should equal(expected)
  }

  test("an empty section should be inserted as the first element if first element is not a section") {
    val original =
      "<h1>hello</h1>" +
      "<section>" +
        "<p>here, have a content</p>" +
      "</section>" +
      "<p>you deserve it</p>" +
      "<section>" +
        "<p>outro</p>" +
      "</section>"
    val content = TestData.sampleContent.copy(content=original)
    val expected =
      "<section>" +
        "<h1>hello</h1>" +
      "</section>" +
      "<section>" +
        "<p>here, have a content</p>" +
        "<p>you deserve it</p>" +
      "</section>" +
      "<section>" +
        "<p>outro</p>" +
      "</section>"


    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)

    result.content should equal(expected)
  }

  test("an empty document should not be wrapped in a section") {
    val original = ""
    val content = TestData.sampleContent.copy(content=original)
    val expected = ""

    val Success((result, _)) = htmlCleaner.convert(content, defaultImportStatus)
    result.content should equal(expected)
  }

  test("first section with only image should be merged with the second section") {
    val originalContent = """<section><embed data-resource="image" /></section>""" +
      "<section><aside><h3>Tallene</h3></aside><p><strong>Dette er en oversikt over tall</strong></p></section>"
    val expectedContent = """<section><embed data-resource="image"><aside><h3>Tallene</h3></aside></section>"""
    val expectedIngress = "Dette er en oversikt over tall"

    val Success((result, _))  = htmlCleaner.convert(TestData.sampleContent.copy(content=originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
    result.ingress should equal(Some(LanguageIngress(expectedIngress, None)))
  }

  test("lists with alphanumeric bullets points should be properly converted") {
    val originalContent =
      """<section>
        |<ol style="list-style-type: lower-alpha;">
        |<li>Definer makt</li>
        |</ol>
        |</section>""".stripMargin.replace("\n", "")
    val expectedContent =
      s"""<section>
         |<ol ${Attributes.DataType}="letters">
         |<li>Definer makt</li>
         |</ol>
         |</section>""".stripMargin.replace("\n", "")

    val Success((result, _))  = htmlCleaner.convert(TestData.sampleContent.copy(content=originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
  }

  test("Ingress should still be fetched if image is placed in ingress section") {
    val originalContent =
      """<section>
        |        <div><p><embed data-align="" data-alt="Identitet (kollasje)" data-caption="" data-resource="image" data-resource_id="1" data-size="fullbredde"><strong></strong></p><p><strong>Du er et unikt individ!<br></strong></p></div></section><section>
        |        <div><p>Er det de genene vi arver fra foreldrene og familien vår, eller er det forholdene vi vokser opp under, for eksempel hjemstedet, familien, venner og skolen? Det er summen av arv og miljø som danner identiteten vår.</p><h2>Normer og regler</h2><p>Normer og regler former oss som individer.</p></div></section>""".stripMargin

    val expectedContent = """<section><embed data-align="" data-alt="Identitet (kollasje)" data-caption="" data-resource="image" data-resource_id="1" data-size="fullbredde">
                            |        <div><p>Er det de genene vi arver fra foreldrene og familien vår, eller er det forholdene vi vokser opp under, for eksempel hjemstedet, familien, venner og skolen? Det er summen av arv og miljø som danner identiteten vår.</p><h2>Normer og regler</h2><p>Normer og regler former oss som individer.</p></div></section>""".stripMargin
    val expectedIngress = """Du er et unikt individ!"""


    val Success((result, _)) = htmlCleaner.convert(TestData.sampleContent.copy(content=originalContent), defaultImportStatus)

    result.content should equal(expectedContent)
    result.ingress should equal(Some(LanguageIngress(expectedIngress, None)))
  }
}
