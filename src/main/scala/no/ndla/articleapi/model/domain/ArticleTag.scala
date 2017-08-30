/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.domain

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

case class ArticleTag(tags: Seq[String],  language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
}
