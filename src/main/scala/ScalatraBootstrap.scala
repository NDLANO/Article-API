/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
import javax.servlet.ServletContext

import no.ndla.contentapi.ComponentRegistry.{internController, contentController, resourcesApp}
import no.ndla.contentapi.ContentSwagger
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  implicit val swagger = new ContentSwagger

  override def init(context: ServletContext) {
    context.mount(contentController, "/content", "content")
    context.mount(resourcesApp, "/api-docs")
    context.mount(internController, "/intern")
  }

}
