/*
 * Part of NDLA article_api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import java.sql.Connection

import org.flywaydb.core.api.migration.jdbc.JdbcMigration
import org.json4s.Extraction.decompose
import org.json4s.JsonAST.JArray
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, JValue}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V15__AddImageMetaAltText extends JdbcMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(connection: Connection): Unit = {
    val db = DB(connection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      migrateArticles
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L

    while (numPagesLeft > 0) {
      allArticles(offset * 1000).map {
        case (id, document) => updateArticle(convertArticleUpdate(document), id)
      }
      numPagesLeft -= 1
      offset += 1
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
      .apply()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from contentdata where document is not null order by id limit 1000 offset ${offset}"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list
      .apply()
  }

  def convertMetaImage(images: List[V6_MetaImage]): JValue = {
    val newMetaImages = images.map(image => V7_MetaImage(image.imageId, image.altText.getOrElse(""), image.language))
    decompose(newMetaImages)
  }

  def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("metaImage", metaImage: JArray) =>
        "metaImage" -> convertMetaImage(metaImage.extract[List[V6_MetaImage]])
      case x => x
    }
    compact(render(newArticle))
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = ${dataObject} where id = ${id}"
      .update()
      .apply
  }

  case class V6_MetaImage(imageId: String, altText: Option[String], language: String)
  case class V7_MetaImage(imageId: String, altText: String, language: String)

}