package no.ndla.contentapi

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import no.ndla.contentapi.integration.{AmazonClientComponent, CMDataComponent, DataSourceComponent, ElasticClientComponent}
import no.ndla.contentapi.repository.ContentRepositoryComponent
import no.ndla.contentapi.service.converters.{ContentBrowserConverter, SimpleTagConverter}
import no.ndla.contentapi.service._
import org.elasticsearch.common.settings.ImmutableSettings
import org.postgresql.ds.PGPoolingDataSource

object ComponentRegistry
  extends DataSourceComponent
  with ContentRepositoryComponent
  with ElasticClientComponent
  with ElasticContentSearchComponent
  with ElasticContentIndexComponent
  with ExtractServiceComponent
  with ConverterModules
  with ConverterServiceComponent
  with CMDataComponent
  with ContentBrowserConverter
  with ImageApiServiceComponent
  with AmazonClientComponent
  with StorageService
{
  lazy val dataSource = new PGPoolingDataSource()
  dataSource.setUser(ContentApiProperties.get("META_USER_NAME"))
  dataSource.setPassword(ContentApiProperties.get("META_PASSWORD"))
  dataSource.setDatabaseName(ContentApiProperties.get("META_RESOURCE"))
  dataSource.setServerName(ContentApiProperties.get("META_SERVER"))
  dataSource.setPortNumber(ContentApiProperties.getInt("META_PORT"))
  dataSource.setInitialConnections(ContentApiProperties.getInt("META_INITIAL_CONNECTIONS"))
  dataSource.setMaxConnections(ContentApiProperties.getInt("META_MAX_CONNECTIONS"))
  dataSource.setCurrentSchema(ContentApiProperties.get("META_SCHEMA"))

  lazy val elasticClient = ElasticClient.remote(
    ImmutableSettings.settingsBuilder().put("cluster.name", ContentApiProperties.SearchClusterName).build(),
    ElasticsearchClientUri(s"elasticsearch://${ContentApiProperties.SearchHost}:${ContentApiProperties.SearchPort}")
  )

  lazy val contentRepository = new ContentRepository
  lazy val elasticContentSearch = new ElasticContentSearch
  lazy val elasticContentIndex = new ElasticContentIndex

  val amazonClient = new AmazonS3Client(new BasicAWSCredentials(ContentApiProperties.StorageAccessKey, ContentApiProperties.StorageSecretKey))
  amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))
  lazy val storageName = ContentApiProperties.StorageName

  lazy val storageService = new AmazonStorageService

  lazy val CMHost = scala.util.Properties.envOrNone("CM_HOST")
  lazy val CMPort = scala.util.Properties.envOrNone("CM_PORT")
  lazy val CMDatabase = scala.util.Properties.envOrNone("CM_DATABASE")
  lazy val CMUser = scala.util.Properties.envOrNone("CM_USER")
  lazy val CMPassword = scala.util.Properties.envOrNone("CM_PASSWORD")
  lazy val imageApiBaseUrl = scala.util.Properties.envOrNone("IMAGE_API_BASE_URL").get

  lazy val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)
  lazy val extractService = new ExtractService
  lazy val converterService = new ConverterService
  lazy val imageApiService = new ImageApiService

  lazy val contentBrowserConverter = new ContentBrowserConverter
  lazy val converterModules = List(contentBrowserConverter, SimpleTagConverter)
}
