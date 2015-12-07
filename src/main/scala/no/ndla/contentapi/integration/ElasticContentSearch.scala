package no.ndla.contentapi.integration

import no.ndla.contentapi.ContentApiProperties
import no.ndla.contentapi.business.ContentSearch
import no.ndla.contentapi.model.ContentSummary

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import no.ndla.contentapi.network.ApplicationUrl
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.index.query.MatchQueryBuilder

import scala.collection.mutable.ListBuffer

class ElasticContentSearch(clusterName:String, clusterHost:String, clusterPort:String) extends ContentSearch {

  val PageSize = 100
  val settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).build()
  val client = ElasticClient.remote(settings, ElasticsearchClientUri(s"elasticsearch://$clusterHost:$clusterPort"))

  val noCopyrightFilter = not(nestedFilter("copyright.license").filter(termFilter("license", "copyrighted")))

  implicit object ContentHitAs extends HitAs[ContentSummary] {
    override def as(hit: RichSearchHit): ContentSummary = {
      val sourceMap = hit.sourceAsMap
      ContentSummary(
        sourceMap("id").toString,
        sourceMap("titles").asInstanceOf[java.util.ArrayList[AnyRef]].get(0).asInstanceOf[java.util.HashMap[String,String]].get("title"),
        ApplicationUrl.get + sourceMap("id").toString,
        sourceMap("copyright").asInstanceOf[java.util.HashMap[String, AnyRef]].get("license").asInstanceOf[java.util.HashMap[String, String]].get("license"))

    }
  }

  override def all(license: Option[String]): Iterable[ContentSummary] = {
    val theSearch = search in ContentApiProperties.SearchIndex -> ContentApiProperties.SearchDocument

    val filterList = new ListBuffer[FilterDefinition]()
    license.foreach(license => filterList += nestedFilter("copyright.license").filter(termFilter("license", license)))
    filterList += noCopyrightFilter

    if(filterList.nonEmpty){
      theSearch.postFilter(must(filterList.toList))
    }
    theSearch.sort(field sort "id")
    client.execute{theSearch limit PageSize}.await.as[ContentSummary]
  }

  override def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String]): Iterable[ContentSummary] = {
    val titleSearch = new ListBuffer[QueryDefinition]
    titleSearch += matchQuery("title", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => titleSearch += termQuery("language", lang))

    val contentSearch = new ListBuffer[QueryDefinition]
    contentSearch += matchQuery("content", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => contentSearch += termQuery("language", lang))

    val tagSearch = new ListBuffer[QueryDefinition]
    tagSearch += matchQuery("tag", query.mkString(" ")).operator(MatchQueryBuilder.Operator.AND)
    language.foreach(lang => tagSearch += termQuery("language", lang))


    val theSearch = search in ContentApiProperties.SearchIndex -> ContentApiProperties.SearchDocument query {
      bool {
        should (
          nestedQuery("titles").query {bool {must (titleSearch.toList)}},
          nestedQuery("content").query {bool {must (contentSearch.toList)}},
          nestedQuery("tags").query {bool {must (tagSearch.toList)}}
        )
      }
    }

    val filterList = new ListBuffer[FilterDefinition]()
    license.foreach(license => filterList += nestedFilter("copyright.license").filter(termFilter("license", license)))
    filterList += noCopyrightFilter

    if(filterList.nonEmpty){
      theSearch.postFilter(must(filterList.toList))
    }

    client.execute{theSearch limit PageSize}.await.as[ContentSummary]
  }
}
