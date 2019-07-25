/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.articleapi.controller

import java.util.concurrent.{Executors, TimeUnit}

import no.ndla.articleapi.ArticleApiProperties
import no.ndla.articleapi.auth.{Role, User}
import no.ndla.articleapi.model.api.{ArticleIdV2, UpdatedConcept}
import no.ndla.articleapi.model.domain.{Concept, Language}
import no.ndla.articleapi.model.api.ArticleIdV2
import no.ndla.articleapi.model.domain.{Article, Language}
import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service._
import no.ndla.articleapi.service.search.{ArticleIndexService, ConceptIndexService, IndexService}
import no.ndla.articleapi.validation.ContentValidator
import no.ndla.validation.ValidationException
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{BadRequest, InternalServerError, NotFound, Ok}

import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success, Try}

trait InternController {
  this: ReadService
    with WriteService
    with ConverterService
    with ArticleRepository
    with IndexService
    with ArticleIndexService
    with ConceptIndexService
    with User
    with Role
    with ContentValidator =>
  val internController: InternController

  class InternController extends NdlaController {

    protected implicit override val jsonFormats: Formats = DefaultFormats

    post("/index") {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      val indexResults = for {
        articleIndex <- Future { articleIndexService.indexDocuments }
        conceptIndex <- Future { conceptIndexService.indexDocuments }
      } yield (articleIndex, conceptIndex)

      Await.result(indexResults, Duration(10, TimeUnit.MINUTES)) match {
        case (Success(articleResult), Success(conceptResult)) =>
          val indexTime = math.max(articleResult.millisUsed, conceptResult.millisUsed)
          val result =
            s"Completed indexing of ${articleResult.totalIndexed} articles and ${conceptResult.totalIndexed} concepts in $indexTime ms."
          logger.info(result)
          Ok(result)
        case (Failure(articleFail), _) =>
          logger.warn(articleFail.getMessage, articleFail)
          InternalServerError(articleFail.getMessage)
        case (_, Failure(conceptFail)) =>
          logger.warn(conceptFail.getMessage, conceptFail)
          InternalServerError(conceptFail.getMessage)
      }
    }

    delete("/index") {
      implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor)
      def pluralIndex(n: Int) = if (n == 1) "1 index" else s"$n indexes"

      val indexes = for {
        articleIndex <- Future { articleIndexService.findAllIndexes(ArticleApiProperties.ArticleSearchIndex) }
        conceptIndex <- Future { conceptIndexService.findAllIndexes(ArticleApiProperties.ConceptSearchIndex) }
      } yield (articleIndex, conceptIndex)

      val deleteResults: Seq[Try[_]] = Await.result(indexes, Duration(10, TimeUnit.MINUTES)) match {
        case (Failure(articleFail), _) => halt(status = 500, body = articleFail.getMessage)
        case (_, Failure(conceptFail)) => halt(status = 500, body = conceptFail.getMessage)
        case (Success(articleIndexes), Success(conceptIndexes)) => {
          val articleDeleteResults = articleIndexes.map(index => {
            logger.info(s"Deleting article index $index")
            articleIndexService.deleteIndexWithName(Option(index))
          })
          val conceptDeleteResults = conceptIndexes.map(index => {
            logger.info(s"Deleting concept index $index")
            conceptIndexService.deleteIndexWithName(Option(index))
          })
          articleDeleteResults ++ conceptDeleteResults
        }
      }

      val (errors, successes) = deleteResults.partition(_.isFailure)
      if (errors.nonEmpty) {
        val message = s"Failed to delete ${pluralIndex(errors.length)}: " +
          s"${errors.map(_.failed.get.getMessage).mkString(", ")}. " +
          s"${pluralIndex(successes.length)} were deleted successfully."
        halt(status = 500, body = message)
      } else {
        Ok(body = s"Deleted ${pluralIndex(successes.length)}")
      }

    }

    get("/ids") {
      articleRepository.getAllIds
    }

    get("/id/:external_id") {
      val externalId = params("external_id")
      articleRepository.getIdFromExternalId(externalId) match {
        case Some(id) => id
        case None     => NotFound()
      }
    }

    post("/id/concept/allocate/?") {
      authRole.assertHasDraftWritePermission()
      val externalIds = paramAsListOfString("external-id")
      ArticleIdV2(writeService.allocateConceptId(externalIds))
    }

    get("/articles") {
      // Dumps Api articles
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)
      val lang = paramOrDefault("language", Language.AllLanguages)
      val fallback = booleanOrDefault("fallback", default = false)

      readService.getArticlesByPage(pageNo, pageSize, lang, fallback)
    }

    get("/dump/article") {
      // Dumps Domain articles
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getArticleDomainDump(pageNo, pageSize)
    }

    post("/validate/article") {
      val importValidate = booleanOrDefault("import_validate", default = false)
      val article = extract[Article](request.body)
      contentValidator.validateArticle(article, allowUnknownLanguage = true, isImported = importValidate) match {
        case Success(_)  => article
        case Failure(ex) => errorHandler(ex)
      }
    }

    post("/article/:id") {
      authRole.assertHasWritePermission()
      val externalIds = paramAsListOfLong("external-id")
      val article = extract[Article](request.body)
      val id = long("id")

      writeService.updateArticle(article.copy(id = Some(id)), externalIds) match {
        case Success(a)  => a
        case Failure(ex) => errorHandler(ex)
      }
    }

    delete("/article/:id/") {
      authRole.assertHasWritePermission()
      writeService.deleteArticle(long("id")) match {
        case Success(a)  => a
        case Failure(ex) => errorHandler(ex)
      }
    }

    post("/article/:id/unpublish/") {
      authRole.assertHasWritePermission()
      writeService.unpublishArticle(long("id")) match {
        case Success(a)  => a
        case Failure(ex) => errorHandler(ex)
      }
    }

    post("/concept/:id") {
      authRole.assertHasWritePermission()
      val id = long("id")
      val concept = extract[Concept](request.body)

      writeService.updateConcept(id, concept) match {
        case Success(c)  => c
        case Failure(ex) => errorHandler(ex)
      }
    }

    delete("/concept/:id/") {
      authRole.assertHasWritePermission()
      writeService.deleteConcept(long("id")) match {
        case Success(c)  => c
        case Failure(ex) => errorHandler(ex)
      }
    }
    get("/dump/concepts") {
      // Dumps Domain articles
      val pageNo = intOrDefault("page", 1)
      val pageSize = intOrDefault("page-size", 250)

      readService.getConceptDomainDump(pageNo, pageSize)
    }

  }
}
