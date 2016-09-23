/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.search

import com.google.gson.JsonObject
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Count, Search, SearchResult => JestSearchResult}
import io.searchbox.params.Parameters
import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.integration.ElasticClientComponent
import no.ndla.articleapi.model._
import no.ndla.network.ApplicationUrl
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.index.IndexNotFoundException
import org.elasticsearch.index.query.{BoolQueryBuilder, MatchQueryBuilder, QueryBuilders}
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.{FieldSortBuilder, SortBuilders, SortOrder}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SearchService {
  this: ElasticClientComponent with SearchIndexServiceComponent with SearchConverterService =>
  val searchService: SearchService

  class SearchService extends LazyLogging {

    val noCopyright = QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery("license", "copyrighted"))

    def getHits(response: JestSearchResult): Seq[ArticleSummary] = {
      var resultList = Seq[ArticleSummary]()
      response.getTotal match {
        case count: Integer if count > 0 => {
          val resultArray = response.getJsonObject.get("hits").asInstanceOf[JsonObject].get("hits").getAsJsonArray
          val iterator = resultArray.iterator()
          while (iterator.hasNext) {
            resultList = resultList :+ hitAsArticleSummary(iterator.next().asInstanceOf[JsonObject].get("_source").asInstanceOf[JsonObject])
          }
          resultList
        }
        case _ => Seq()
      }
    }

    def hitAsArticleSummary(hit: JsonObject): ArticleSummary = {
      import scala.collection.JavaConversions._

      ArticleSummary(
        hit.get("id").getAsString,
        hit.get("titles").getAsJsonObject.entrySet().to[Seq].map(entr => ArticleTitle(entr.getValue.getAsString, Some(entr.getKey))),
        ApplicationUrl.get + hit.get("id").getAsString,
        hit.get("license").getAsString)
    }

    def all(language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      executeSearch(
        language.getOrElse(Language.DefaultLanguage),
        license,
        sort,
        page,
        pageSize,
        QueryBuilders.boolQuery())
    }

    def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int], sort: Sort.Value): SearchResult = {
      val searchLanguage = language.getOrElse(Language.DefaultLanguage)

      val titleSearch = QueryBuilders.matchQuery(s"titles.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val articleSearch = QueryBuilders.matchQuery(s"article.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
      val tagSearch = QueryBuilders.matchQuery(s"tags.$searchLanguage", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)

      val fullSearch = QueryBuilders.boolQuery()
        .must(QueryBuilders.boolQuery()
          .should(QueryBuilders.nestedQuery("titles", titleSearch))
          .should(QueryBuilders.nestedQuery("article", articleSearch))
          .should(QueryBuilders.nestedQuery("tags", tagSearch)))

      executeSearch(searchLanguage, license, sort, page, pageSize, fullSearch)
    }

    def executeSearch(language: String, license: Option[String], sort: Sort.Value, page: Option[Int], pageSize: Option[Int], queryBuilder: BoolQueryBuilder): SearchResult = {
      val filteredSearch = license match {
        case None => queryBuilder.filter(noCopyright)
        case Some(lic) => queryBuilder.filter(QueryBuilders.termQuery("license", lic))
      }

      val searchQuery = new SearchSourceBuilder().query(filteredSearch).sort(getSortDefinition(sort, language))

      val (startAt, numResults) = getStartAtAndNumResults(page, pageSize)
      val request = new Search.Builder(searchQuery.toString)
        .addIndex(ArticleApiProperties.SearchIndex)
        .setParameter(Parameters.SIZE, numResults)
        .setParameter("from", startAt)

      val response = jestClient.execute(request.build())
      response.isSucceeded match {
        case true => SearchResult(response.getTotal.toLong, page.getOrElse(1), numResults, getHits(response))
        case false => errorHandler(response)
      }
    }

    def getSortDefinition(sort: Sort.Value, language: String): FieldSortBuilder = {
      sort match {
        case (Sort.ByTitleAsc) => SortBuilders.fieldSort(s"titles.$language.raw").setNestedPath("titles").order(SortOrder.ASC).missing("_last")
        case (Sort.ByTitleDesc) => SortBuilders.fieldSort(s"titles.$language.raw").setNestedPath("titles").order(SortOrder.DESC).missing("_last")
        case (Sort.ByRelevanceAsc) => SortBuilders.fieldSort("_score").order(SortOrder.ASC)
        case (Sort.ByRelevanceDesc) => SortBuilders.fieldSort("_score").order(SortOrder.DESC)
        case (Sort.ByLastUpdatedAsc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.ASC).missing("_last")
        case (Sort.ByLastUpdatedDesc) => SortBuilders.fieldSort("lastUpdated").order(SortOrder.DESC).missing("_last")
      }
    }

    def countDocuments(): Int = {
      jestClient.execute(
        new Count.Builder().addIndex(ArticleApiProperties.SearchIndex).build()
      ).getCount.toInt
    }

    def getStartAtAndNumResults(page: Option[Int], pageSize: Option[Int]): (Int, Int) = {
      val numResults = pageSize match {
        case Some(num) =>
          if (num > 0) num.min(ArticleApiProperties.MaxPageSize) else ArticleApiProperties.DefaultPageSize
        case None => ArticleApiProperties.DefaultPageSize
      }

      val startAt = page match {
        case Some(sa) => (sa - 1).max(0) * numResults
        case None => 0
      }

      (startAt, numResults)
    }

    private def errorHandler(response: JestSearchResult) = {
      response.getResponseCode match {
        case notFound: Int if notFound == 404 => {
          logger.error(s"Index ${ArticleApiProperties.SearchIndex} not found. Scheduling a reindex.")
          scheduleIndexDocuments()
          throw new IndexNotFoundException(s"Index ${ArticleApiProperties.SearchIndex} not found. Scheduling a reindex")
        }
        case _ => {
          logger.error(response.getErrorMessage)
          throw new ElasticsearchException(s"Unable to execute search in ${ArticleApiProperties.SearchIndex}", response.getErrorMessage)
        }
      }
    }

    def scheduleIndexDocuments() = {
      val f = Future {
        searchIndexService.indexDocuments()
      }
      f onFailure { case t => logger.error("Unable to create index: " + t.getMessage) }
    }
  }

}
