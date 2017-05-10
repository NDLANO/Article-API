/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model.domain

import java.net.URL
import java.util.Date

import com.netaporter.uri.dsl._
import no.ndla.articleapi.integration.LanguageContent

case class NodeGeneralContent(nid: String, tnid: String, title: String, content: String, language: String) {
  def isTranslation = !isMainNode

  def isMainNode = nid == tnid || tnid == "0"

  def asContentTitle = ArticleTitle(title, Some(language))
}

case class NodeToConvert(titles: Seq[ArticleTitle],
                         contents: Seq[LanguageContent],
                         license: String,
                         authors: Seq[Author],
                         tags: Seq[ArticleTag],
                         visualElements: Seq[VisualElement],
                         contentType: String,
                         nodeType: String,
                         created: Date,
                         updated: Date
                        )

case class ContentFilMeta(nid: String, tnid: String, title: String, fileName: String, url: URL, mimeType: String, fileSize: String)

object ContentFilMeta {
  implicit def stringToUrl(s: String): URL = new URL(s.uri)
}

case class BiblioMeta(biblio: Biblio, authors: Seq[BiblioAuthor])

case class Biblio(title: String, bibType: String, year: String, edition: String, publisher: String)

case class BiblioAuthor(name: String, lastname: String, firstname: String)
