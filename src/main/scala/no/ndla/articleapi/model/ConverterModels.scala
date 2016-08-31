/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.model

import java.net.URL
import com.netaporter.uri.dsl._
import no.ndla.articleapi.integration.LanguageContent

case class NodeGeneralContent(nid: String, tnid: String, title: String, content: String, language: String) {
  def isMainNode = (nid == tnid || tnid == "0")
  def isTranslation = !isMainNode

  def asContentTitle = ArticleTitle(title, Some(language))
}

case class NodeToConvert(titles: Seq[ArticleTitle], contents: Seq[LanguageContent], copyright: Copyright, tags: Seq[ArticleTag]) {
  def asArticleInformation: ArticleInformation = {
    val requiredLibraries = contents.flatMap(_.requiredLibraries).distinct
    ArticleInformation("0", titles, contents.map(_.asContent), copyright, tags, requiredLibraries)
  }
}

case class ContentFilMeta(nid: String, tnid: String, title: String, fileName: String, url: URL, mimeType: String, fileSize: String)
object ContentFilMeta {
  implicit def stringToUrl(s: String): URL = new URL(s.uri)
}

case class NodeIngress(content: String, imageNid: Option[String], ingressVisPaaSiden: Int)

case class BiblioMeta(biblio: Biblio, authors: Seq[BiblioAuthor])
case class Biblio(title: String, bibType: String, year: String, edition: String, publisher: String)
case class BiblioAuthor(name: String, lastname: String, firstname: String)
