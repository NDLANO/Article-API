package no.ndla.contentapi.integration

import javax.sql.DataSource

import com.typesafe.scalalogging.LazyLogging
import no.ndla.contentapi.business.ContentData
import no.ndla.contentapi.model.ContentInformation
import org.postgresql.util.PGobject
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, _}

class PostgresData(dataSource: DataSource) extends ContentData with LazyLogging {

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override def insert(contentInformation: ContentInformation, externalId: String) = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val json = write(contentInformation)
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)

    DB localTx {implicit session =>
      sql"insert into contentdata(external_id, document) values(${externalId}, ${dataObject})".update.apply
    }

    logger.info(s"Inserted ${externalId}")
  }

  override def withId(contentId: String): Option[ContentInformation] = {
    DB readOnly {implicit session =>
      sql"select document from contentdata where id = ${contentId.toInt}".map(rs => rs.string("document")).single.apply match {
        case Some(json) => Option(asContentInformation(contentId, json))
        case None => None
      }
    }
  }

  def asContentInformation(contentId: String, json: String): ContentInformation = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    val meta = read[ContentInformation](json)
    ContentInformation(
      contentId,
      meta.titles,
      meta.content,
      meta.copyright,
      meta.tags,
      meta.requiredLibraries)
  }
}
