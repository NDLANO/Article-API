package no.ndla.contentapi

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.contentapi.controller.{ContentController, InternController}
import no.ndla.contentapi.integration.{MigrationApiClient, _}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service._
import no.ndla.contentapi.service.converters.{BiblioConverter, DivTableConverter, IngressConverter, SimpleTagConverter}
import org.elasticsearch.common.settings.Settings
import no.ndla.contentapi.service.converters.contentbrowser._
import no.ndla.network.NdlaClient
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends DataSourceComponent
  with InternController
  with ContentController
  with ContentRepositoryComponent
  with ElasticClientComponent
  with ElasticContentSearchComponent
  with ElasticContentIndexComponent
  with ExtractServiceComponent
  with MigrationApiClient
  with NdlaClient
  with ConverterModules
  with ConverterServiceComponent
  with ImageApiServiceComponent
  with ContentBrowserConverterModules
  with ContentBrowserConverter
  with BiblioConverterModule
  with BiblioConverter
  with AmazonClientComponent
  with StorageService
  with IngressConverter
  with HtmlTagsUsage
  with ExtractConvertStoreContent
{
  implicit val swagger = new ContentSwagger

  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ContentApiProperties.get("META_USER_NAME"))
  dataSource.setPassword(ContentApiProperties.get("META_PASSWORD"))
  dataSource.setDatabaseName(ContentApiProperties.get("META_RESOURCE"))
  dataSource.setServerName(ContentApiProperties.get("META_SERVER"))
  dataSource.setPortNumber(ContentApiProperties.getInt("META_PORT"))
  dataSource.setInitialConnections(ContentApiProperties.getInt("META_INITIAL_CONNECTIONS"))
  dataSource.setMaxConnections(ContentApiProperties.getInt("META_MAX_CONNECTIONS"))
  dataSource.setCurrentSchema(ContentApiProperties.get("META_SCHEMA"))

  lazy val extractConvertStoreContent = new ExtractConvertStoreContent
  lazy val internController = new InternController
  lazy val contentController = new ContentController
  lazy val resourcesApp = new ResourcesApp

  lazy val elasticClient = ElasticClient.transport(
    Settings.settingsBuilder().put("cluster.name", ContentApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://${ContentApiProperties.SearchHost}:${ContentApiProperties.SearchPort}"))

  lazy val contentRepository = new ContentRepository
  lazy val elasticContentSearch = new ElasticContentSearch
  lazy val elasticContentIndex = new ElasticContentIndex

  val amazonClient = new AmazonS3Client(new BasicAWSCredentials(ContentApiProperties.StorageAccessKey, ContentApiProperties.StorageSecretKey))
  amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
  lazy val storageName = ContentApiProperties.StorageName
  lazy val storageService = new AmazonStorageService

  lazy val CMHost = ContentApiProperties.CMHost
  lazy val CMPort = ContentApiProperties.CMPort
  lazy val CMDatabase = ContentApiProperties.CMDatabase
  lazy val CMUser = ContentApiProperties.CMUser
  lazy val CMPassword = ContentApiProperties.CMPassword
  lazy val imageApiBaseUrl = ContentApiProperties.imageApiBaseUrl

  lazy val migrationApiClient = new MigrationApiClient
  lazy val ndlaClient = new NdlaClient
  lazy val extractService = new ExtractService
  lazy val converterService = new ConverterService
  lazy val imageApiService = new ImageApiService

  lazy val contentBrowserConverter = new ContentBrowserConverter
  lazy val ingressConverter = new IngressConverter
  lazy val biblioConverter = new BiblioConverter
  lazy val converterModules = List(ingressConverter, contentBrowserConverter, biblioConverter, DivTableConverter, SimpleTagConverter)
}
