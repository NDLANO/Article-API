/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.indexes.IndexDefinition
import com.sksamuel.elastic4s.mappings.{MappingBuilderFn, MappingDefinition, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.model.domain.Article
import no.ndla.articleapi.model.domain.Language.languageAnalyzers
import no.ndla.articleapi.model.search.{SearchableArticle, SearchableLanguageFormats}
import no.ndla.articleapi.repository.{ArticleRepository, Repository}
import org.json4s.native.Serialization.write

trait ArticleIndexService {
  this: SearchConverterService with IndexService with ArticleRepository =>
  val articleIndexService: ArticleIndexService

  class ArticleIndexService extends LazyLogging with IndexService[Article, SearchableArticle] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = ArticleApiProperties.ArticleSearchDocument
    override val searchIndex: String = ArticleApiProperties.ArticleSearchIndex
    override val repository: Repository[Article] = articleRepository

    override def createIndexRequest(domainModel: Article, indexName: String): IndexDefinition = {
      val source = write(searchConverterService.asSearchableArticle(domainModel))
      indexInto(indexName / documentType).doc(source).id(domainModel.id.get.toString)
    }

    def getMapping: MappingDefinition = {
      mapping(documentType).fields(
        intField("id"),
        languageSupportedField("title", keepRaw = true),
        languageSupportedField("content"),
        languageSupportedField("visualElement"),
        languageSupportedField("introduction"),
        languageSupportedField("metaDescription"),
        languageSupportedField("tags"),
        keywordField("defaultTitle"),
        dateField("lastUpdated"),
        keywordField("license"),
        textField("authors").fielddata(true),
        textField("articleType").analyzer("keyword")
      )
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = NestedFieldDefinition(fieldName).fields(
      keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw")))
        case false => languageAnalyzers.map(langAnalyzer => textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer))
      })

      languageSupportedField
    }

  }
}
