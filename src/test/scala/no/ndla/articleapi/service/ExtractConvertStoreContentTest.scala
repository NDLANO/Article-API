/*
 * Part of NDLA article_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.articleapi.service

import java.util.Date

import no.ndla.articleapi.integration.{LicenseDefinition, MigrationSubjectMeta}
import no.ndla.articleapi.model.domain._
import no.ndla.articleapi.{TestData, TestEnvironment, UnitSuite}
import org.mockito.Mockito._
import org.mockito.Matchers._
import scalikejdbc.DBSession

import scala.util.{Success, Try}

class ExtractConvertStoreContentTest extends UnitSuite with TestEnvironment {
  val (nodeId, nodeId2) = ("1234", "4321")
  val newNodeid: Long = 4444
  val sampleTitle = ArticleTitle("title", Some("en"))
  val sampleIngress =  "ingress here"
  val contentString = s"[contentbrowser ==nid=$nodeId2==imagecache=Fullbredde==width===alt=alttext==link===node_link=1==link_type=link_to_content==lightbox_size===remove_fields[76661]=1==remove_fields[76663]=1==remove_fields[76664]=1==remove_fields[76666]=1==insertion=link==link_title_text===link_text=Tittel==text_align===css_class=contentbrowser contentbrowser]"
  val sampleContent = TestData.sampleContent.copy(content=contentString)
  val author = Author("forfatter", "Henrik")
  val visualElement = VisualElement("http://image-api/1", "image", Some("nb"))

  val sampleNode = NodeToConvert(List(sampleTitle), List(sampleContent), "by-sa", Seq(author), List(ArticleTag(List("tag"), Some("en"))), Seq(visualElement), "fagstoff", new Date(0), new Date(1))

  val eCSService = new ExtractConvertStoreContent

  override def beforeEach = {
    when(mappingApiClient.getLicenseDefinition("by-sa")).thenReturn(Some(LicenseDefinition("by-sa", "Creative Commons Attribution-ShareAlike 2.0 Generic", None)))
    when(extractService.getNodeData(nodeId)).thenReturn(sampleNode)
    when(extractService.getNodeType(nodeId2)).thenReturn(Some("fagstoff"))
    when(extractService.getNodeGeneralContent(nodeId2)).thenReturn(Seq(NodeGeneralContent(nodeId2, nodeId2, "title", "content", "en")))
    when(articleRepository.getIdFromExternalId(nodeId2)).thenReturn(None)
    when(migrationApiClient.getSubjectForNode(nodeId)).thenReturn(Try(Seq(MigrationSubjectMeta("52", "helsearbeider vg2"))))

    when(articleRepository.exists(sampleNode.contents.head.nid)).thenReturn(false)
    when(articleRepository.insert(any[Article], any[String], any[Seq[String]])(any[DBSession])).thenReturn(newNodeid)
    when(extractConvertStoreContent.processNode("9876")).thenReturn(Try(1: Long, ImportStatus(Seq(), Seq())))
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

}
