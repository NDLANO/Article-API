/*
 * Part of NDLA article_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.{ArticleSwagger, TestEnvironment, UnitSuite}
import org.scalatra.test.scalatest.ScalatraFunSuite

class ArticleControllerTest extends UnitSuite with TestEnvironment with ScalatraFunSuite {

  val jwtHeader = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"

  val jwtClaims = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsiYXJ0aWNsZXM6d3JpdGUiXSwibmRsYV9pZCI6ImFiYzEyMyJ9LCJuYW1lIjoiRG9uYWxkIER1Y2siLCJpc3MiOiJodHRwczovL3NvbWUtZG9tYWluLyIsInN1YiI6Imdvb2dsZS1vYXV0aDJ8MTIzIiwiYXVkIjoiYWJjIiwiZXhwIjoxNDg2MDcwMDYzLCJpYXQiOjE0ODYwMzQwNjN9"
  val jwtClaimsNoRoles = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOltdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"
  val jwtClaimsWrongRole = "eyJhcHBfbWV0YWRhdGEiOnsicm9sZXMiOlsic29tZTpvdGhlciJdLCJuZGxhX2lkIjoiYWJjMTIzIn0sIm5hbWUiOiJEb25hbGQgRHVjayIsImlzcyI6Imh0dHBzOi8vc29tZS1kb21haW4vIiwic3ViIjoiZ29vZ2xlLW9hdXRoMnwxMjMiLCJhdWQiOiJhYmMiLCJleHAiOjE0ODYwNzAwNjMsImlhdCI6MTQ4NjAzNDA2M30"

  val authHeaderWithWriteRole = s"Bearer $jwtHeader.$jwtClaims.VxqM2bu2UF8IAalibIgdRdmsTDDWKEYpKzHPbCJcFzA"
  val authHeaderWithoutAnyRoles = s"Bearer $jwtHeader.$jwtClaimsNoRoles.kXjaQ9QudcRHTqhfrzKr0Zr4pYISBfJoXWHVBreDyO8"
  val authHeaderWithWrongRole = s"Bearer $jwtHeader.$jwtClaimsWrongRole.JsxMW8y0hCmpuu9tpQr6ZdfcqkOS8hRatFi3cTO_PvY"

  implicit val formats = org.json4s.DefaultFormats
  implicit val swagger = new ArticleSwagger

  lazy val controller = new ArticleController
  addServlet(controller, "/")

  test("That / returns a validation message if article is invalid") {
    post("/", headers = Map("Authorization" -> authHeaderWithWriteRole)) {
      status should equal (400)
    }
  }

  test("That POST / returns 403 if no auth-header") {
    post("/") {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have expected role") {
    post("/", headers = Map("Authorization" -> authHeaderWithWrongRole)) {
      status should equal (403)
    }
  }

  test("That POST / returns 403 if auth header does not have any roles") {
    post("/", headers = Map("Authorization" -> authHeaderWithoutAnyRoles)) {
      status should equal (403)
    }
  }

}
