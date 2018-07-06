package no.ndla.articleapi.repository

import no.ndla.articleapi.model.api.NotFoundException
import no.ndla.articleapi.model.domain
import no.ndla.articleapi.model.domain.ArticleIds
import no.ndla.articleapi.{DBMigrator, IntegrationSuite, TestData, TestEnvironment}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

import scala.util.{Failure, Success, Try}

class ArticleRepositoryTest extends IntegrationSuite with TestEnvironment {
  var repository: ArticleRepository = _

  val sampleArticle = TestData.sampleArticleWithByNcSa

  def databaseIsAvailable: Boolean = Try(repository.articleCount).isSuccess

  override def beforeEach() = {
    repository = new ArticleRepository()
  }

  override def beforeAll() = {
    ConnectionPool.singleton(new DataSourceConnectionPool(getDataSource))
    if (databaseIsAvailable)
      DBMigrator.migrate(ConnectionPool.dataSource())
  }

  override def afterEach(): Unit = {
    repository.getAllIds.foreach(articleId => repository.delete(articleId.articleId))
  }

  test("getAllIds returns a list with all ids in the database") {
    assume(databaseIsAvailable, "Database is unavailable")
    val externalIds = (100 to 150).map(_.toString)
    val ids = externalIds.map(exId => repository.allocateArticleIdWithExternalIds(List(exId), Set("52")))
    val expected = ids.zip(externalIds).map { case (id, exId) => ArticleIds(id, List(exId)) }.toList

    repository.getAllIds should equal(expected)
  }

  test("getIdFromExternalId works with all ids") {
    assume(databaseIsAvailable, "Database is unavailable")
    val inserted1 = repository.allocateArticleIdWithExternalIds(List("6000","10"), Set("52"))
    val inserted2 = repository.allocateArticleIdWithExternalIds(List("6001","11"), Set("52"))

    repository.getIdFromExternalId("6000").get should be(inserted1)
    repository.getIdFromExternalId("6001").get should be(inserted2)
    repository.getIdFromExternalId("10").get should be(inserted1)
    repository.getIdFromExternalId("11").get should be(inserted2)
  }

  test("getArticleIdsFromExternalId should return ArticleIds object with externalIds") {
    assume(databaseIsAvailable, "Database is unavailable")
    val externalIds = List("1", "6010", "6011", "5084", "763", "8881", "1919")
    val inserted = repository.allocateArticleIdWithExternalIds(externalIds, Set.empty)

    repository.getArticleIdsFromExternalId("6011").get.externalId should be(externalIds)
    repository.delete(inserted)
  }

  test("updateArticleFromDraftApi should update all columns with data from draft-api") {
    assume(databaseIsAvailable, "Database is unavailable")

    val externalIds = List("123", "456")
    val sampleArticle: domain.Article = TestData.sampleDomainArticle.copy(id = Some(repository.allocateArticleId()), revision = Some(42))

    sampleArticle.created.setTime(0)
    sampleArticle.created.setTime(0)
    val Success((res: domain.Article)) = repository.updateArticleFromDraftApi(sampleArticle, externalIds)

    res.id.isDefined should be (true)
    repository.withId(res.id.get).get should be (sampleArticle)
  }

  test("updateArticleFromDraftApi fail if trying to update an article which does not exist") {
    assume(databaseIsAvailable, "Database is unavailable")

    val externalIds = List("123", "456")
    val sampleArticle: domain.Article = TestData.sampleDomainArticle.copy(id = Some(123), revision = Some(42))
    val Failure ((res: NotFoundException)) = repository.updateArticleFromDraftApi(sampleArticle, externalIds)
    res.message should equal (s"No article with id Some(123) exists!")
  }

}
