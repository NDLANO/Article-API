/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import java.util.Date

import io.searchbox.client.JestResult
import no.ndla.articleapi.integration.{LanguageIngress, MigrationSubjectMeta}
import no.ndla.articleapi.model.api.{NotFoundException, OptimisticLockException}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._
import scalikejdbc.DBSession

import scala.util.{Failure, Success, Try}

class ExtractConvertStoreContentTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val newNodeid: Long = 4444
  val sampleTitle = ArticleTitle("title", Some("en"))
  val sampleIngress =  LanguageIngress("ingress here", None)
  val contentString = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=alttext==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
  val sampleContent = TestData.sampleContent.copy(content=contentString)
  val author = Author("forfatter", "Henrik")

  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), "by-sa", Seq(author), List(ArticleTag(List("tag"), Some("en"))), Seq(TestData.visualElement), "fagstoff", new Date(0), new Date(1))

  val eCSService = new ExtractConvertStoreContent

  override def beforeEach = {
    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode)
    when(extractService.getNodeType(nodeId2)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(NodeGeneralContent(nodeId2, nodeId2, "title", "content", "en")))
    when(articleRepository.getIdFromExternalId(nodeId2)).thenReturn(None)
    when(migrationApiClient.getSubjectForNode(nodeId)).thenReturn(Try(Seq(MigrationSubjectMeta("52", "helsearbeider vg2"))))

    when(articleRepository.exists(sampleNode.contents.head.nid)).thenReturn(false)
    when(articleRepository.insertWithExternalIds(any[Article], any[String], any[Seq[String]])(any[DBSession])).thenReturn(newNodeid)
    when(extractConvertStoreContent.processNode("9876")).thenReturn(Try(1: Long, ImportStatus(Seq(), Seq())))
    when(searchIndexService.indexDocument(any[Article])).thenReturn(Success())
  }

  test("That ETL extracts, translates and loads a node correctly") {
    when(extractConvertStoreContent.processNode(nodeId2, ImportStatus(Seq(), Seq(nodeId)))).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq(nodeId, nodeId2)))))

    val result = eCSService.processNode(nodeId)
    result should equal(Success(newNodeid, ImportStatus(List(s"Successfully imported node $nodeId: $newNodeid"), List(nodeId, nodeId2))))
  }

  test("That ETL returns a list of visited nodes") {
    when(extractConvertStoreContent.processNode(nodeId2, ImportStatus(Seq(), Seq("9876", nodeId)))).thenReturn(Try((newNodeid, ImportStatus(Seq(), Seq("9876", nodeId, nodeId2)))))

    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result should equal(Success(newNodeid, ImportStatus(List(s"Successfully imported node $nodeId: $newNodeid"), List("9876", nodeId, nodeId2))))
  }

  test("That ETL returns a Failure if the node was not found") {
    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode.copy(contents=Seq()))
    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result.isFailure should be (true)
  }

  test("That ETL returns a Failure if failed to persist the converted article") {
    when(articleRepository.updateWithExternalId(any[Article], any[String])).thenReturn(Failure(new OptimisticLockException()))
    when(articleRepository.exists(sampleNode.contents.head.nid)).thenReturn(true)
    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result.isFailure should be (true)
  }

  test("That ETL returns a Failure if failed to index the converted article") {
    when(searchIndexService.indexDocument(any[Article])).thenReturn(any[Failure[NdlaSearchException]])
    val result = eCSService.processNode(nodeId, ImportStatus(Seq(), Seq("9876")))
    result.isFailure should be (true)
  }

}
