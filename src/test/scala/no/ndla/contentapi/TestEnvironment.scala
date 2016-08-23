package no.ndla.contentapi

import javax.sql.DataSource

import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.ElasticClient
import no.ndla.contentapi.controller.{ContentController, InternController}
import no.ndla.contentapi.integration._
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.converters.{BiblioConverter, DivTableConverter, IngressConverter, SimpleTagConverter}
import no.ndla.contentapi.service._
import no.ndla.contentapi.service.converters.contentbrowser._
import no.ndla.network.NdlaClient
import org.scalatest.mock.MockitoSugar


trait TestEnvironment
  extends ElasticClientComponent
  with ElasticContentSearchComponent
  with ElasticContentIndexComponent
  with ContentController
  with InternController
  with DataSourceComponent
  with ContentRepositoryComponent
  with MockitoSugar
  with MigrationApiClient
  with NdlaClient
  with ExtractServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with ContentBrowserConverterModules
  with ContentBrowserConverter
  with IngressConverter
  with BiblioConverterModule
  with BiblioConverter
  with ImageApiServiceComponent
  with AmazonClientComponent
  with StorageService
  with HtmlTagsUsage
  with ExtractConvertStoreContent
{
  val elasticClient = mock[ElasticClient]
  val elasticContentSearch = mock[ElasticContentSearch]
  val elasticContentIndex = mock[ElasticContentIndex]

  val internController = mock[InternController]
  val contentController = mock[ContentController]

  val dataSource = mock[DataSource]
  val contentRepository = mock[ContentRepository]
  val amazonClient = mock[AmazonS3Client]
  val storageName = "testStorageName"

  val extractConvertStoreContent = mock[ExtractConvertStoreContent]

  val migrationApiClient = mock[MigrationApiClient]
  val ndlaClient = mock[NdlaClient]
  val extractService = mock[ExtractService]
  val converterService = new ConverterService
  val contentBrowserConverter = new ContentBrowserConverter
  val ingressConverter = new IngressConverter
  val biblioConverter = new BiblioConverter
  val converterModules = List(ingressConverter, SimpleTagConverter, biblioConverter, DivTableConverter, contentBrowserConverter)
  val imageApiService = mock[ImageApiService]
  val storageService = mock[AmazonStorageService]
}
