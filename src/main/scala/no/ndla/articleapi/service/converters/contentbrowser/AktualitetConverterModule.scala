/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service.converters.contentbrowser

import no.ndla.articleapi.repository.ArticleRepository
import no.ndla.articleapi.service.converters.HtmlTagGenerator
import no.ndla.articleapi.service.{ExtractConvertStoreContent, ExtractService}

trait AktualitetConverterModule extends GeneralContentConverterModule {
  this: ExtractService with ExtractConvertStoreContent with ArticleRepository with HtmlTagGenerator =>

  object AktualitetConverter extends GeneralContentConverter {
    override val typeName: String = "aktualitet"
  }
}
