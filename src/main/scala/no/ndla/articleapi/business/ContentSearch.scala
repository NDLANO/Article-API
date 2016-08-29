package no.ndla.articleapi.business

import no.ndla.articleapi.model.{ContentSummary}

trait ContentSearch {
  def all(license: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[ContentSummary]
  def matchingQuery(query: Iterable[String], language: Option[String], license: Option[String], page: Option[Int], pageSize: Option[Int]): Iterable[ContentSummary]
}
