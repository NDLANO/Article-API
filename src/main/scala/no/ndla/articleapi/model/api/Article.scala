/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.api

import java.util.Date

import org.scalatra.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "Information about the article")
case class Article(@(ApiModelProperty@field)(description = "The unique id of the article") id: String,
                   @(ApiModelProperty@field)(description = "Link to article on old platform") oldNdlaUrl: Option[String],
                   @(ApiModelProperty@field)(description = "The revision number for the article") revision: Int,
                   @(ApiModelProperty@field)(description = "Available titles for the article") title: Seq[ArticleTitle],
                   @(ApiModelProperty@field)(description = "The content of the article in available languages") content: Seq[ArticleContent],
                   @(ApiModelProperty@field)(description = "Describes the copyright information for the article") copyright: Copyright,
                   @(ApiModelProperty@field)(description = "Searchable tags for the article") tags: Seq[ArticleTag],
                   @(ApiModelProperty@field)(description = "Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
                   @(ApiModelProperty@field)(description = "A visual element article") visualElement: Seq[VisualElement],
                   @(ApiModelProperty@field)(description = "An introduction for the article") introduction: Seq[ArticleIntroduction],
                   @(ApiModelProperty@field)(description = "Meta description for the article") metaDescription: Seq[ArticleMetaDescription],
                   @(ApiModelProperty@field)(description = "When the article was created") created: Date,
                   @(ApiModelProperty@field)(description = "When the article was last updated") updated: Date,
                   @(ApiModelProperty@field)(description = "By whom the article was last updated") updatedBy: String,
                   @(ApiModelProperty@field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String
                  )

case class ArticleV2(@(ApiModelProperty@field)(description = "The unique id of the article") id: String,
                     @(ApiModelProperty@field)(description = "Link to article on old platform") oldNdlaUrl: Option[String],
                     @(ApiModelProperty@field)(description = "The revision number for the article") revision: Int,
                     @(ApiModelProperty@field)(description = "The chosen language") language: String,
                     @(ApiModelProperty@field)(description = "Available titles for the article") title: String,
                     @(ApiModelProperty@field)(description = "The content of the article in available languages") content: String,
                     @(ApiModelProperty@field)(description = "Foot notes referred to within the html article") footNotes: Option[Map[String, FootNoteItem]],
                     @(ApiModelProperty@field)(description = "Describes the copyright information for the article") copyright: Copyright,
                     @(ApiModelProperty@field)(description = "Searchable tags for the article") tags: Seq[String],
                     @(ApiModelProperty@field)(description = "Required libraries in order to render the article") requiredLibraries: Seq[RequiredLibrary],
                     @(ApiModelProperty@field)(description = "A visual element article") visualElement: String,
                     @(ApiModelProperty@field)(description = "An introduction for the article") introduction: String,
                     @(ApiModelProperty@field)(description = "Meta description for the article") metaDescription: String,
                     @(ApiModelProperty@field)(description = "When the article was created") created: Date,
                     @(ApiModelProperty@field)(description = "When the article was last updated") updated: Date,
                     @(ApiModelProperty@field)(description = "By whom the article was last updated") updatedBy: String,
                     @(ApiModelProperty@field)(description = "The type of article this is. Possible values are topic-article,standard") articleType: String,
                     @(ApiModelProperty@field)(description = "") supportedLanguages: Seq[String]
                    )
