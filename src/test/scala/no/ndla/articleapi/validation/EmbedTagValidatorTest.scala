/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.validation

import no.ndla.articleapi.ArticleApiProperties.resourceHtmlEmbedTag
import no.ndla.articleapi.UnitSuite
import no.ndla.articleapi.model.api.ValidationMessage
import no.ndla.articleapi.service.converters.{Attributes, ResourceType}

class EmbedTagValidatorTest extends UnitSuite {
  val embedTagValidator = new EmbedTagValidator()

  private def generateAttributes(attrs: Map[String, String]): String = {
    attrs.toList.sortBy(_._1.toString).map { case (key, value) => s"""$key="$value"""" }.mkString(" ")
  }

  private def generateTagWithAttrs(attrs: Map[Attributes.Value, String]): String = {
    val strAttrs = attrs map{ case (k, v) => k.toString -> v }
    s"""<$resourceHtmlEmbedTag ${generateAttributes(strAttrs)} />"""
  }

  private def findErrorByMessage(validationMessages: Seq[ValidationMessage], partialMessage: String) =
    validationMessages.find(_.message.contains(partialMessage))

  test("validate should return an empty sequence if input is not an embed tag") {
    embedTagValidator.validate("content", "<h1>hello</h1>") should equal (Seq())
  }

  test("validate should return validation error if embed tag uses illegal attributes") {
    val attrs = generateAttributes(Map(
      Attributes.DataResource.toString -> ResourceType.ExternalContent.toString,
      Attributes.DataUrl.toString -> "google.com", "illegalattr" -> "test"))

    val res = embedTagValidator.validate("content", s"""<$resourceHtmlEmbedTag $attrs />""")
    findErrorByMessage(res, "illegal attribute(s) 'illegalattr'").size should be (1)
  }

  test("validate should return validation error if an attribute contains HTML") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ExternalContent.toString,
      Attributes.DataUrl -> "<i>google.com</i>"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, "contains attributes with HTML: data-url").size should be (1)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=image") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Image} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if image embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Image.toString,
      Attributes.DataId -> "1",
      Attributes.DataResource_Id -> "1234",
      Attributes.DataSize -> "fullbredde",
      Attributes.DataAlt -> "alternative text",
      Attributes.DataCaption -> "here is a rabbit",
      Attributes.DataAlign -> "left"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=audio") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Audio.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Audio} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if audio embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Audio.toString,
      Attributes.DataId -> "1",
      Attributes.DataResource_Id -> "1234"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=h5p") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.H5P.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.H5P} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if h5p embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.H5P.toString,
      Attributes.DataId -> "1",
      Attributes.DataUrl -> "http://ndla.no/h5p/embed/1234"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=brightcove") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Brightcove.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Brightcove} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if brightcove embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Brightcove.toString,
      Attributes.DataId -> "1",
      Attributes.DataCaption -> "here is a video",
      Attributes.DataVideoId -> "1234",
      Attributes.DataAccount -> "2183716",
      Attributes.DataPlayer -> "B28fas"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=content-link") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ContentLink.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.ContentLink} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if content-link embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ContentLink.toString,
      Attributes.DataId -> "1",
      Attributes.DataContentId -> "54",
      Attributes.DataLinkText -> "interesting article"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=error") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Error.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.Error} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if error embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.Error.toString,
      Attributes.DataId -> "1",
      Attributes.DataMessage -> "interesting article"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=external") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ExternalContent.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.ExternalContent} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if external embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.ExternalContent.toString,
      Attributes.DataId -> "1",
      Attributes.DataUrl -> "https://www.youtube.com/watch?v=pCZeVTMEsik"))
    embedTagValidator.validate("content", tag).size should be (0)
  }

  test("validate should return validation error if embed tag does not contain required attributes for data-resource=nrk") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.NRKContent.toString,
      Attributes.DataId -> "1"))
    val res = embedTagValidator.validate("content", tag)
    findErrorByMessage(res, s"data-resource=${ResourceType.NRKContent} must contain the following attributes:").size should be (1)
  }

  test("validate should return no validation errors if nrk embed-tag is used correctly") {
    val tag = generateTagWithAttrs(Map(
      Attributes.DataResource -> ResourceType.NRKContent.toString,
      Attributes.DataId -> "1",
      Attributes.DataNRKVideoId -> "123",
      Attributes.DataUrl -> "http://nrk.no/video/123"
    ))
    embedTagValidator.validate("content", tag).size should be (0)
  }

}
