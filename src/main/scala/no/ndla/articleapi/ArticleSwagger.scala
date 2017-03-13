/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi

import org.scalatra.ScalatraServlet
import org.scalatra.swagger._

class ResourcesApp(implicit val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  get("/") {
    renderSwagger2(swagger.docs.toList)
  }
}

object ArticleApiInfo {
  val apiInfo = ApiInfo(
    "Article API",
    "Documentation for the Article API of NDLA.no",
    "http://ndla.no",
    ArticleApiProperties.ContactEmail,
    "GPL v3.0",
    "http://www.gnu.org/licenses/gpl-3.0.en.html")
}

class ArticleSwagger extends Swagger("2.0", "0.8", ArticleApiInfo.apiInfo) {
  addAuthorization(OAuth(List("articles:all"), List(ApplicationGrant(TokenEndpoint("/auth/tokens", "access_token")))))
}
