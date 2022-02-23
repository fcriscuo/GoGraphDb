package org.batterypakdev.gographdb.go.neo4j

import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.pubmed.model.GoPubMedIdentifier
import org.neo4j.driver.Record
import java.util.*

object Neo4jUtils {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    /*
    Function to simplify quoting a String value for Cypher input
     */
    fun formatQuotedString(input:String):String =
        "\"" + input +"\""
    /*
    Function to determine if a node has already been loaded into Neo4j
     */
    fun nodeLoadedPredicate(cypherCommand: String): Boolean {
        if (cypherCommand.contains("PREDICATE", ignoreCase = true)) {
            try {
                val predicate = Neo4jConnectionService.executeCypherCommand(cypherCommand)
                when (predicate.lowercase(Locale.getDefault())) {
                    "true" -> return true
                    "false" -> return false
                }
            } catch (e: Exception) {
                logger.atSevere().log(e.message.toString())
                return false
            }
        }
        return false
    }

    /*
    Function to delete a Neo4j relationship
     */
    fun deleteNodeRelationshipByName(parentNode: String, childNode: String, relName: String) {
        val deleteTemplate = "MATCH (:PARENT) -[r:RELATIONSHIP_NAME] ->(:CHILD) DELETE r;"
        val countTemplate = "MATCH (:PubMedArticle) -[:RELATIONSHIP_NAME] -> (:PubMedArticle) RETURN COUNT(*)"
        val delCommand = deleteTemplate
            .replace("PARENT", parentNode)
            .replace("CHILD", childNode)
            .replace("RELATIONSHIP_NAME", relName)
        val countCommand = countTemplate
            .replace("PARENT", parentNode)
            .replace("CHILD", childNode)
            .replace("RELATIONSHIP_NAME", relName)
        val beforeCount = Neo4jConnectionService.executeCypherCommand(countCommand)
        logger.atInfo().log("Deleting $parentNode $relName $childNode relationships, before count = $beforeCount")
        Neo4jConnectionService.executeCypherCommand(delCommand)
        val afterCount = Neo4jConnectionService.executeCypherCommand(countCommand)
        logger.atInfo().log("After deletion command count = $afterCount")
    }

    /*
    Function to delete a specified label from a specified node type
     */
    fun removeNodeLabel(nodeName: String, label: String) {
        val removeLabelTemplate = "MATCH (n:NODENAME) REMOVE n:LABEL RETURN COUNT(n)"
        val countLabelTemplate = "MATCH(l:LABEL) RETURN COUNT(l)"
        val removeLabelCommand = removeLabelTemplate
            .replace("NODENAME", nodeName)
            .replace("LABEL", label)
        val countLabelCommand = countLabelTemplate.replace("LABEL", label)
        val beforeCount = Neo4jConnectionService.executeCypherCommand(countLabelCommand)
        logger.atInfo().log("Node type: $nodeName, removing label: $label before count = $beforeCount")
        Neo4jConnectionService.executeCypherCommand(removeLabelCommand)
        val afterCount = Neo4jConnectionService.executeCypherCommand(countLabelCommand)
        logger.atInfo().log("Node type: $nodeName, after label removal command count = $afterCount")
    }

    // detach and delete specified nodes in database
    fun detachAndDeleteNodesByName(nodeName: String) {
        val beforeCount = Neo4jConnectionService.executeCypherCommand(
            "MATCH (n: $nodeName) RETURN COUNT (n)"
        )
        Neo4jConnectionService.executeCypherCommand(
            "MATCH (n: $nodeName) DETACH DELETE (n);"
        )
        val afterCount = Neo4jConnectionService.executeCypherCommand(
            "MATCH (n: $nodeName) RETURN COUNT (n)"
        )
        logger.atInfo().log(
            "Deleted $nodeName nodes, before count=${beforeCount.toString()}" +
                    "  after count=$afterCount"
        )
    }

    /*
    Function to find empty (i.e. placholder) PubMedArticle nodes
    Returns a Sequence of PubMed Ids as Ints
     */
    private val emptyNodeQuery = "MATCH (c:PubMedArticle) WHERE c.article_title =\"\" " +
            " return c.pubmed_id, c.parent_id"
    /*
    Private function to map selected items from an
    empty PubMedArticle node into a PubMedIdentifier
     */
    private fun resolvePubMedIdentifier(record: Record): GoPubMedIdentifier {
        val recMap = record.asMap()
        val id = recMap.get("c.pubmed_id").toString().toInt()
        val parentId = when (record.get("c.parent_id")) {
            null -> "0"
            else -> record.get("c.parent_id").toString()
        }
        return GoPubMedIdentifier(id, parentId,"Publication")
    }

    fun resolvePlaceholderPubMedArticleNodes(): Sequence<GoPubMedIdentifier> =
        Neo4jConnectionService.executeCypherQuery(emptyNodeQuery)
            .map{rec -> resolvePubMedIdentifier(rec) }
            .toList().asSequence()

}