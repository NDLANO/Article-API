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

case class ArticleMetaDescription(content: String, language: String) extends LanguageField[String] {
  override def isEmpty: Boolean = content.isEmpty
  override def value: String = content
}
