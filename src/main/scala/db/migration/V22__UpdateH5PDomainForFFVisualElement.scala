/*
 * Part of NDLA article-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */
package db.migration

import no.ndla.articleapi.ArticleApiProperties
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JArray, JObject, JString}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

class V22__UpdateH5PDomainForFFVisualElement extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats

  override def migrate(context: Context): Unit = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    if (ArticleApiProperties.Environment == "ff") {
      db.withinTx { implicit session =>
        migrateArticles
      }
    }
  }

  def migrateArticles(implicit session: DBSession): Unit = {
    val count = countAllArticles.get
    var numPagesLeft = (count / 1000) + 1
    var offset = 0L
    val pageSize = 1000
    println(s"Running migration '${this.getClass.getSimpleName}' on $count rows in $numPagesLeft batches of $pageSize")

    while (numPagesLeft > 0) {
      allArticles(offset * pageSize).map {
        case (id, document) => updateArticle(convertArticleUpdate(document), id)
      }
      numPagesLeft -= 1
      offset += 1
      println(s"'${this.getClass.getSimpleName}' processsed ${math.min(offset * pageSize, count)} rows, ${math
        .max(count - (offset * pageSize), 0)} left...")
    }
  }

  def countAllArticles(implicit session: DBSession): Option[Long] = {
    sql"select count(*) from contentdata where document is not NULL"
      .map(rs => rs.long("count"))
      .single()
      .apply()
  }

  def allArticles(offset: Long)(implicit session: DBSession): Seq[(Long, String)] = {
    sql"select id, document from contentdata where document is not null order by id limit 1000 offset $offset"
      .map(rs => {
        (rs.long("id"), rs.string("document"))
      })
      .list
      .apply()
  }

  def updateArticle(document: String, id: Long)(implicit session: DBSession): Int = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(document)

    sql"update contentdata set document = $dataObject where id = $id"
      .update()
      .apply
  }

  def updateH5PDomains(html: String): String = {
    val oldDomain = "h5p.ndla.no"
    val newDomain = "h5p-ff.ndla.no"

    html.replaceAll(oldDomain, newDomain)
  }

  private[migration] def convertArticleUpdate(document: String): String = {
    val oldArticle = parse(document)

    val newArticle = oldArticle.mapField {
      case ("visualElement", contents: JArray) =>
        val updatedContent = contents.map {
          case content: JObject =>
            content.mapField {
              case ("resource", JString(html)) => ("resource", JString(updateH5PDomains(html)))
              case z                           => z
            }
          case y => y
        }
        ("visualElement", updatedContent)
      case x => x
    }

    compact(render(newArticle))
  }
}