package no.ndla.articleapi.repository

import no.ndla.articleapi.model.domain.ArticleTitle
import no.ndla.articleapi.{DBMigrator, IntegrationSuite, TestData, TestEnvironment}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

class ArticleRepositoryTest extends IntegrationSuite with TestEnvironment {
  var repository: ArticleRepository = _

  val sampleArticle = TestData.sampleArticleWithByNcSa

  override def beforeEach() = {
    repository = new ArticleRepository()
  }

  override def beforeAll() = {
    val datasource = getDataSource
    DBMigrator.migrate(datasource)
    ConnectionPool.singleton(new DataSourceConnectionPool(datasource))
  }

  test("updating several times updates revision number") {
    val first = repository.insert(sampleArticle)
    first.id.isDefined should be (true)

    val second = repository.update(first.copy(title = Seq(ArticleTitle("first change", Some("en")))))
    second.isSuccess should be (true)

    val third = repository.update(second.get.copy(title = Seq(ArticleTitle("second change", Some("en")))))
    third.isSuccess should be (true)

    first.revision should equal (Some(1))
    second.get.revision should equal(Some(2))
    third.get.revision should equal(Some(3))

    repository.delete(first.id.get)
  }

  test("Updating with an outdated revision number returns a Failure") {
    val first = repository.insert(sampleArticle)
    first.id.isDefined should be (true)

    val oldRevision = repository.update(first.copy(revision=Some(0), title = Seq(ArticleTitle("first change", Some("en")))))
    oldRevision.isFailure should be (true)

    val tooNewRevision = repository.update(first.copy(revision=Some(99), title = Seq(ArticleTitle("first change", Some("en")))))
    tooNewRevision.isFailure should be (true)

    repository.delete(first.id.get)
  }

  test("updateWithExternalId does not update revision number") {
    val externalId = "123"
    val articleId = repository.insertWithExternalIds(sampleArticle, externalId, Seq("52"))

    val firstUpdate = repository.updateWithExternalId(sampleArticle, externalId)
    val secondUpdate = repository.updateWithExternalId(sampleArticle.copy(title = Seq(ArticleTitle("new title", Some("en")))), externalId)

    firstUpdate.isSuccess should be (true)
    secondUpdate.isSuccess should be (true)

    val updatedArticle = repository.withId(articleId)
    updatedArticle.isDefined should be (true)
    updatedArticle.get.revision should be (Some(1))

    repository.delete(articleId)
  }

  test("updateWithExternalId returns a Failure if article has been updated on new platform") {
    val externalId = "123"
    val articleId = repository.insertWithExternalIds(sampleArticle, externalId, Seq("52"))

    repository.update(sampleArticle.copy(id=Some(articleId)))
    val result = repository.updateWithExternalId(sampleArticle.copy(id=Some(articleId)), externalId)
    result.isFailure should be (true)

    repository.delete(articleId)
  }

}
