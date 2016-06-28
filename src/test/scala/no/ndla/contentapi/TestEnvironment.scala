package no.ndla.contentapi

import javax.sql.DataSource

import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.ElasticClient
import no.ndla.contentapi.controller.{ContentController, InternController}
import no.ndla.contentapi.integration.{AmazonClientComponent, CMDataComponent, DataSourceComponent, ElasticClientComponent}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.converters.{DivTableConverter, IngressConverter, SimpleTagConverter}
import no.ndla.contentapi.service._
import no.ndla.contentapi.service.converters.contentbrowser._
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
  with CMDataComponent
  with ExtractServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with ImageConverterModule
  with LenkeConverterModule
  with H5PConverterModule
  with AktualitetConverterModule
  with OppgaveConverterModule
  with FagstoffConverterModule
  with AudioConverterModule
  with ContentBrowserConverter
  with IngressConverter
  with ImageApiServiceComponent
  with AmazonClientComponent
  with StorageService
{
  val elasticClient = mock[ElasticClient]
  val elasticContentSearch = mock[ElasticContentSearch]
  val elasticContentIndex = mock[ElasticContentIndex]

  val internController = mock[InternController]
  val contentController = mock[ContentController]

  val dataSource = mock[DataSource]
  val contentRepository = new ContentRepository
  val amazonClient = mock[AmazonS3Client]
  val storageName = "testStorageName"

  val cmData = mock[CMData]
  val extractService = mock[ExtractService]
  val converterService = mock[ConverterService]
  val contentBrowserConverter = new ContentBrowserConverter
  val ingressConverter = new IngressConverter
  val converterModules = List(SimpleTagConverter, ingressConverter, DivTableConverter, contentBrowserConverter)
  val imageApiService = mock[ImageApiService]
  val storageService = mock[AmazonStorageService]
}
