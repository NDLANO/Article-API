/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.JsonAST.{JArray, JString, JValue}
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V4__ChangeArticleToContent extends JdbcMigration {
  implicit val formats = org.json4s.DefaultFormats

  override def migrate(connection: Connection) = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      allContentNodes.map(convertDocumentToNewFormat).foreach(update)
    }
  }

  def allContentNodes(implicit session: DBSession): List[V4_DBContent] = {
    sql"select id, document from contentdata".map(rs => V4_DBContent(rs.long("id"), rs.string("document"))).list().apply()
  }

  def convertDocumentToNewFormat(content: V4_DBContent): V4_DBContent = {
    val json = parse(content.document).mapField {
      case ("article", JArray(x)) => ("content", renameInner(JArray(x)))
      case ("titles", JArray(x)) => ("title", JArray(x))
      case x => x
    }

    V4_DBContent(content.id, write(json))
  }

  def renameInner(array: JArray): JValue = {
    array.mapField {
      case ("article", JString(x)) => ("content", JString(x))
      case x => x
    }
  }

  def update(content: V4_DBContent)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(content.document)

    sql"update contentdata set document = $dataObject where id = ${content.id}".update().apply
  }
}

case class V4_DBContent(id: Long, document: String)
case class V4_OldArticleContent(article: String, footNotes: Option[Map[String, V4_FootNoteItem]], language: Option[String])
case class V4_ArticleContent(content: String, footNotes: Option[Map[String, V4_FootNoteItem]], language: Option[String])
case class V4_FootNoteItem(title: String, `type`: String, year: String, edition: String, publisher: String, authors: Seq[String])