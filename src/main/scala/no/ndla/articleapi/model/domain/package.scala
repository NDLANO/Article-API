/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */
package no.ndla.articleapi.model

package object domain {
  def emptySomeToNone(lang: Option[String]): Option[String] = {
    lang.filter(_.nonEmpty)
  }

  case class ArticleIds(articleId: Long, externalId: Option[String])

  def getByLanguage[T](entries: Seq[LanguageField[T]], language: String): Option[T] =
    entries.find(_.language.getOrElse(Language.UnknownLanguage) == language).map(_.value)
}
