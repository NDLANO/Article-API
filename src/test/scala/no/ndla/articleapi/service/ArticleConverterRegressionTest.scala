package no.ndla.articleapi.service

import java.io.File

import no.ndla.articleapi.integration.JestClientFactory
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.service.converters.{DivTableConverter, MathMLConverter, SimpleTagConverter, TableConverter}
import no.ndla.articleapi.{ArticleApiProperties, IntegrationSuite, TestEnvironment}
import org.json4s.native.Serialization.read
import org.json4s.{DefaultFormats, Formats}
import scalikejdbc.{ConnectionPool, DataSourceConnectionPool}

import scala.util.{Failure, Success}

class ArticleConverterRegressionTest extends IntegrationSuite with TestEnvironment {
  protected implicit val jsonFormats: Formats = DefaultFormats

  override val dataSource = getDataSource
  override val articleRepository = new ArticleRepository()

  override val jestClient = JestClientFactory.getClient(searchServer = "http://localhost:9200")
  override val searchService = new SearchService
  override val indexService = new IndexService
  override val searchIndexService = new SearchIndexService
  override val searchConverterService = new SearchConverterService

  override val attachmentStorageName = ArticleApiProperties.AttachmentStorageName
  override val attachmentStorageService = new AmazonStorageService

  override val extractConvertStoreContent = new ExtractConvertStoreContent

  override val migrationApiClient = new MigrationApiClient
  override val extractService = new ExtractService

  override val converterService = new ConverterService
  override val contentBrowserConverter = new ContentBrowserConverter
  override val biblioConverter = new BiblioConverter
  override val htmlCleaner = new HTMLCleaner
  override val converterModules = List(contentBrowserConverter)
  override val postProcessorModules = List(SimpleTagConverter, biblioConverter, DivTableConverter, TableConverter, MathMLConverter, htmlCleaner, VisualElementConverter)

  override val readService = new ReadService
  override val writeService = new WriteService

  override val ndlaClient = new NdlaClient
  override val tagsService = new TagsService

  override val audioApiClient = new AudioApiClient
  override val imageApiClient = new ImageApiClient

  override val clock = new SystemClock

  override val articleValidator = new ArticleValidator

  override def beforeAll() = {
    ConnectionPool.singleton(new DataSourceConnectionPool(getDataSource))
  }

  def originalFiles: List[File] = {
    val path = getClass.getResource("/perfect_articles")
    new File(path.getPath).listFiles.toList
  }

  case class PerfectArticle(nodeId: String,
                            content: Seq[ArticleContent],
                            introduction: Seq[ArticleIntroduction],
                            metaDescription: Seq[ArticleMetaDescription]
                           )

  def readArticleFromFile(f: File): PerfectArticle = {
    val json = scala.io.Source.fromFile(f.getAbsolutePath).mkString
    read[PerfectArticle](json)
  }

  def getByLanguage[T <: LanguageField](fields: Seq[T], lang: String): Option[T] = {
    fields.find(f => f.language.getOrElse("") == lang)
  }

  def verifyNoLanguageContentChanges[T <: LanguageField](perfect: Seq[T], imported: Seq[T]) = {
    val importedContentLanguages = imported.map(_.language).toSet
    val originalContentLanguages = perfect.map(_.language).toSet
    importedContentLanguages should equal (originalContentLanguages)

    perfect.foreach(origContent => {
      val Some(importedContent) = getByLanguage(imported, origContent.language.getOrElse(""))
      importedContent should equal(origContent)
    })
  }


  def verifyNoChanges(article: PerfectArticle): Unit = {
    val articleId = extractConvertStoreContent.processNode(article.nodeId) match {
      case Success((newId, _)) => newId
      case Failure(exc) => throw exc
    }

    val importedArticle = articleRepository.withId(articleId).get
    verifyNoLanguageContentChanges(article.content, importedArticle.content)
    verifyNoLanguageContentChanges(article.introduction, importedArticle.introduction)
    verifyNoLanguageContentChanges(article.metaDescription, importedArticle.metaDescription)
  }

  test("import routine should not break perfectly looking articles") {
    originalFiles.map(readArticleFromFile).foreach(verifyNoChanges)
  }

}
