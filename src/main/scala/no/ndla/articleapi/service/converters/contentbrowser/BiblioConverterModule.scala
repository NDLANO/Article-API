/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.model.{ImportStatus, RequiredLibrary}
import no.ndla.articleapi.repository.ArticleRepositoryComponent
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractServiceComponent}

trait BiblioConverterModule {
  this: ExtractServiceComponent with ExtractConvertStoreContent with ArticleRepositoryComponent =>

  object BiblioConverter extends ContentBrowserConverterModule with LazyLogging {
    override val typeName: String = "biblio"

    override def convert(content: ContentBrowser, visitedNodes: Seq[String]): (String, Seq[RequiredLibrary], ImportStatus) = {
      val nodeId = content.get("nid")
      (s"""<a id="biblio-$nodeId"></a>""", List[RequiredLibrary](), ImportStatus(Seq(), visitedNodes))
    }
  }
}
