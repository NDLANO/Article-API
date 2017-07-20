/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.model.api._
import no.ndla.articleapi.{ArticleSwagger, TestData, TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.Success

class ArticleControllerV2Test extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  val jwtHeader = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"

  val jwtClaims = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXJ0aWNsZXM6d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9"
  val jwtClaimsNoRoles = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"
  val jwtClaimsWrongRole = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsic29tZTpvdGhlciJdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"

  val authHeaderWithWriteRole = s"Bearer $jwtHeader.$jwtClaims.VxqM2bu2UF8IAalibIgdRdmsTDDWKEYpKzHPbCJcFzA"
  val authHeaderWithoutAnyRoles = s"Bearer $jwtHeader.$jwtClaimsNoRoles.kXjaQ9QudcRHTqhfrzKr0Zr4pYISBfJoXWHVBreDyO8"
  val authHeaderWithWrongRole = s"Bearer $jwtHeader.$jwtClaimsWrongRole.JsxMW8y0hCmpuu9tpQr6ZdfcqkOS8hRatFi3cTO_PvY"

  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ArticleSwagger

  lazy val controller = new ArticleControllerV2
  addServlet(controller, "/test")

  val updateTitleJson = """{"revision": 1, "title": "hehe", "language": "nb", "content": "content"}"""
  val invalidArticle = """{"revision": 1, "title": [{"language": "nb", "titlee": "lol"]}"""
  val lang = "nb"
  val articleId = 1

  test("/<article_id> should return 200 if the cover was found withIdV2") {
    when(readService.withIdV2(articleId, lang)).thenReturn(Some(TestData.sampleArticleV2))

    get(s"/test/$articleId?language=$lang") {
      status should equal(200)
    }
  }

  test("/<article_id> should return 404 if the article was not found withIdV2") {
    when(readService.withIdV2(articleId, lang)).thenReturn(None)

    get(s"/test/$articleId?language=$lang") {
      status should equal(404)
    }
  }

  test("/<article_id> should return 400 if the article was not found withIdV2") {
    get(s"/test/one") {
      status should equal(400)
    }
  }


  test("POST / should return 400 on failure to validate request") {
    post("/test/", "{}", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(400)
    }
  }

  test("POST / should return 201 on created") {
    when(writeService.newArticleV2(any[NewArticleV2])).thenReturn(Some(TestData.sampleArticleV2))
    post("/test/", TestData.requestNewArticleV2Body, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal(201)
    }
  }

  test("That / returns a validation message if article is invalid") {
    post("/test", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (400)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    post("/test") {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/test", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/test", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns a validation message if article is invalid") {
    patch("/test/123", invalidArticle, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (400)
    }
  }

  test("That PATCH /:id returns 403 if no auth-header") {
    patch("/test/123") {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns 403 if auth header does not have expected role") {
    patch("/test/123", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns 403 if auth header does not have any roles") {
    patch("/test/123", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

  test("That PATCH /:id returns 200 on success") {
    when(writeService.updateArticle(any[Long], any[UpdatedArticle])).thenReturn(Success(TestData.apiArticleWithHtmlFault))
    patch("/test/123", updateTitleJson, headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (200)
    }
  }

}
