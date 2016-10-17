/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}
import scala.annotation.meta.field

@ApiModel(description = "Description of the article introduction")
case class ArticleIntroduction(@(ApiModelProperty@field)(description = "The introduction content") introduction: String,
                               @(ApiModelProperty@field)(description = "An image complementing the introduction text") image: Option[String],
                               @(ApiModelProperty@field)(description = "A boolean describing whether to display the introduction in the article or not") displayIngress: Boolean,
                               @(ApiModelProperty@field)(description = "The ISO 639-1 language code describing which article translation this introduction belongs to") language: Option[String])
