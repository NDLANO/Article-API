/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.ArticleApiProperties.{
  DefaultPageSize,
  ElasticSearchIndexMaxResultWindow,
  ElasticSearchScrollKeepAlive,
  MaxPageSize
}
import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.model.api._
import no.ndla.articleapi.model.domain.{ArticleIds, ArticleType, Language, SearchSettings, Sort}
import no.ndla.articleapi.service.search.{ArticleSearchService, SearchConverterService}
import no.ndla.articleapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.articleapi.validation.ContentValidator
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{NotFound, Ok}
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}
import org.scalatra.util.NotNothing

import scala.util.{Failure, Success}

trait ArticleControllerV2 {
  this: ReadService
    with WriteService
    with ArticleSearchService
    with SearchConverterService
    with ConverterService
    with Role
    with User
    with ContentValidator =>
  val articleControllerV2: ArticleControllerV2

  class ArticleControllerV2(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected implicit override val jsonFormats: Formats = DefaultFormats
    protected val applicationDescription = "Services for accessing articles from NDLA."

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()

    val converterService = new ConverterService
    val response400 = ResponseMessage(400, "Validation Error", Some("ValidationError"))
    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only articles with content matching the specified query.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license = Param[Option[String]](
      "license",
      "Return only results with provided license. Specifying 'all' gives all articles regardless of licence.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.values.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and id (asc) when query is empty.""".stripMargin
    )
    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]]("page-size", "The number of search hits to display for each page.")
    private val articleId = Param[Long]("article_id", "Id of the article that is to be fecthed.")
    private val size =
      Param[Option[Int]](
        "size",
        s"Limit the number of results to this many elements. Default is $DefaultPageSize and max is $MaxPageSize.")
    private val articleTypes = Param[Option[String]](
      "articleTypes",
      "Return only articles of specific type(s). To provide multiple types, separate by comma (,).")
    private val articleIds = Param[Option[Seq[Long]]](
      "ids",
      "Return only articles that have one of the provided ids. To provide multiple ids, separate by comma (,).")
    private val deprecatedNodeId = Param[String]("deprecated_node_id", "Id of deprecated NDLA node")
    private val fallback = Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A search context retrieved from the response header of a previous search.
         |If search-context is specified, all other query parameters, except '${this.language.paramName}' and '${this.fallback.paramName}' are ignored
         |For the rest of the parameters the original search of the search-context is used.
         |The search context may change between scrolls. Always use the most recent one (The context if unused dies after $ElasticSearchScrollKeepAlive).
         |Used to enable scrolling past $ElasticSearchIndexMaxResultWindow results.
      """.stripMargin
    )
    private val grepCodes = Param[Option[Seq[String]]](
      "grep-codes",
      "A comma separated list of codes from GREP API the resources should be filtered by.")

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)

    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)

    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)

    /**
      * Does a scroll with [[ArticleSearchService]]
      * If no scrollId is specified execute the function @orFunction in the second parameter list.
      *
      * @param orFunction Function to execute if no scrollId in parameters (Usually searching)
      * @return A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String, fallback: Boolean)(
        orFunction: => Any): Any = {
      scrollId match {
        case Some(scroll) =>
          articleSearchService.scroll(scroll, language, fallback) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiSearchResultV2(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
          }
        case None => orFunction
      }
    }

    get(
      "/tags/",
      operation(
        apiOperation[ArticleTag]("getTags")
          .summary("Fetch tags used in articles.")
          .description("Retrieves a list of all previously used tags in articles.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(size),
            asQueryParam(language)
          )
          .responseMessages(response500))
    ) {
      val defaultSize = 20
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val size = intOrDefault(this.size.paramName, defaultSize) match {
        case tooSmall if tooSmall < 1 => defaultSize
        case x                        => x
      }
      val tags = readService.getNMostUsedTags(size, language)
      if (tags.isEmpty) {
        NotFound(body = Error(Error.NOT_FOUND, s"No tags with language $language was found"))
      } else {
        tags
      }
    }

    get(
      "/tag-search/",
      operation(
        apiOperation[ArticleTag]("getTags-paginated")
          .summary("Fetch tags used in articles.")
          .description("Retrieves a list of all previously used tags in articles.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo),
            asQueryParam(language)
          )
          .responseMessages(response500))
    ) {
      val query = paramOrDefault(this.query.paramName, "")
      val pageSize = intOrDefault(this.pageSize.paramName, ArticleApiProperties.DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => ArticleApiProperties.DefaultPageSize
        case x                        => x
      }
      val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      readService.getAllTags(query, pageSize, pageNo, language)
    }

    private def search(
        query: Option[String],
        sort: Option[Sort.Value],
        language: String,
        license: Option[String],
        page: Int,
        pageSize: Int,
        idList: List[Long],
        articleTypesFilter: Seq[String],
        fallback: Boolean,
        grepCodes: Seq[String]
    ) = {

      val settings = query match {
        case Some(q) =>
          SearchSettings(
            query = Some(q),
            withIdIn = idList,
            language = language,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
            fallback = fallback,
            grepCodes = grepCodes
          )

        case None =>
          SearchSettings(
            query = None,
            withIdIn = idList,
            language = language,
            license = license,
            page = page,
            pageSize = if (idList.isEmpty) pageSize else idList.size,
            sort = sort.getOrElse(Sort.ByIdAsc),
            if (articleTypesFilter.isEmpty) ArticleType.all else articleTypesFilter,
            fallback = fallback,
            grepCodes = grepCodes
          )
      }

      articleSearchService.matchingQuery(settings) match {
        case Success(searchResult) =>
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiSearchResultV2(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/",
      operation(
        apiOperation[List[SearchResultV2]]("getAllArticles")
          .summary("Find published articles.")
          .description("Returns all articles. You can search it too.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(articleTypes),
            asQueryParam(query),
            asQueryParam(articleIds),
            asQueryParam(language),
            asQueryParam(license),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(sort),
            asQueryParam(scrollId)
          )
          .responseMessages(response500))
    ) {
      val scrollId = paramOrNone(this.scrollId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      scrollSearchOr(scrollId, language, fallback) {
        val query = paramOrNone(this.query.paramName)
        val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
        val license = paramOrNone(this.license.paramName)
        val pageSize = intOrDefault(this.pageSize.paramName, ArticleApiProperties.DefaultPageSize)
        val page = intOrDefault(this.pageNo.paramName, 1)
        val idList = paramAsListOfLong(this.articleIds.paramName)
        val articleTypesFilter = paramAsListOfString(this.articleTypes.paramName)
        val grepCodes = paramAsListOfString(this.grepCodes.paramName)

        search(query, sort, language, license, page, pageSize, idList, articleTypesFilter, fallback, grepCodes)
      }
    }

    post(
      "/search/",
      operation(
        apiOperation[List[SearchResultV2]]("getAllArticlesPost")
          .summary("Find published articles.")
          .description("Search all articles.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(scrollId),
            bodyParam[ArticleSearchParams]
          )
          .responseMessages(response400, response500))
    ) {
      val searchParams = extract[ArticleSearchParams](request.body)
      val language = searchParams.language.getOrElse(Language.AllLanguages)
      val fallback = searchParams.fallback.getOrElse(false)

      scrollSearchOr(searchParams.scrollId, language, fallback) {
        val query = searchParams.query
        val sort = Sort.valueOf(searchParams.sort.getOrElse(""))
        val license = searchParams.license
        val pageSize = searchParams.pageSize.getOrElse(ArticleApiProperties.DefaultPageSize)
        val page = searchParams.page.getOrElse(1)
        val idList = searchParams.idList
        val articleTypesFilter = searchParams.articleTypes
        val grepCodes = searchParams.grepCodes

        search(query, sort, language, license, page, pageSize, idList, articleTypesFilter, fallback, grepCodes)
      }
    }

    get(
      "/:article_id",
      operation(
        apiOperation[List[ArticleV2]]("getArticleById")
          .summary("Fetch specified article.")
          .description("Returns the article for the specified id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId),
            asQueryParam(language),
            asQueryParam(fallback)
          )
          .responseMessages(response404, response500))
    ) {
      val articleId = long(this.articleId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      readService.withIdV2(articleId, language, fallback) match {
        case Success(article) => article
        case Failure(ex)      => errorHandler(ex)
      }
    }

    get(
      "/external_id/:deprecated_node_id",
      operation(
        apiOperation[ArticleIdV2]("getInternalIdByExternalId")
          .summary("Get id of article corresponding to specified deprecated node id.")
          .description("Get internal id of article for a specified ndla_node_id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(deprecatedNodeId)
          )
          .responseMessages(response404, response500))
    ) {
      val externalId = long(this.deprecatedNodeId.paramName)
      readService.getInternalIdByExternalId(externalId) match {
        case Some(id) => id
        case None     => NotFound(body = Error(Error.NOT_FOUND, s"No article with id $externalId"))
      }
    }

    get(
      "/external_ids/:deprecated_node_id",
      operation(
        apiOperation[ArticleIds]("getExternalIdsByExternalId")
          .summary("Get all ids related to article corresponding to specified deprecated node id.")
          .description(
            "Get internal id as well as all deprecated ndla_node_ids of article for a specified ndla_node_id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(deprecatedNodeId)
          )
          .responseMessages(response404, response500))
    ) {
      val externalId = params(this.deprecatedNodeId.paramName)
      readService.getArticleIdsByExternalId(externalId) match {
        case Some(idObject) => idObject
        case None           => NotFound()
      }
    }

    patch(
      "/partial-publish/:article_id",
      operation(
        apiOperation[ArticleV2]("partialPublish")
          .summary("Publish metadata of article directly without affecting rest of article.")
          .description("Publish metadata of article directly without affecting rest of article.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(articleId),
            asQueryParam(language),
            asQueryParam(fallback),
            bodyParam[PartialPublishArticle]
          )
          .responseMessages(response404, response500))
    ) {

      authRole.assertHasWritePermission()

      val articleId = long(this.articleId.paramName)
      val partialUpdateBody = extract[PartialPublishArticle](request.body)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)

      writeService.partialUpdate(articleId, partialUpdateBody, language, fallback) match {
        case Failure(ex)         => errorHandler(ex)
        case Success(apiArticle) => Ok(apiArticle)
      }

    }
  }
}
