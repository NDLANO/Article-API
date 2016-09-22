/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.model.search

case class LanguageValue[T](lang: Option[String], value: T)

case class SearchableLanguageValues(languageValues: Seq[LanguageValue[String]])

case class SearchableLanguageList(languageValues: Seq[LanguageValue[Seq[String]]])

case class SearchableArticleInformation(id: String,
                                        titles: SearchableLanguageValues,
                                        article: SearchableLanguageValues,
                                        tags: SearchableLanguageList,
                                        license: String,
                                        authors: Seq[String])