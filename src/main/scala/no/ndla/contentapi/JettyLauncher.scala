/*
 * Part of NDLA Content-API. API for searching and downloading content from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.contentapi

import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler}
import org.scalatra.servlet.ScalatraListener


object JettyLauncher extends LazyLogging {
  def main(args: Array[String]) {
    logger.info(io.Source.fromInputStream(getClass.getResourceAsStream("/log-license.txt")).mkString)

    PropertiesLoader.load()
    DBMigrator.migrate(ComponentRegistry.dataSource)

    val startMillis = System.currentTimeMillis()

    val context = new ServletContextHandler()
    context setContextPath "/"
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

    val server = new Server(ContentApiProperties.ApplicationPort)
    server.setHandler(context)
    server.start

    val startTime = System.currentTimeMillis() - startMillis
    logger.info(s"Started at port ${ContentApiProperties.ApplicationPort} in $startTime ms.")

    server.join
  }
}
