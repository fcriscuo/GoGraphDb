package org.batterypakdev.gographdb.go.neo4j.dao

import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.model.GoSynonym
import org.batterypakdev.gographdb.go.model.GoTerm
import org.batterypakdev.gographdb.go.neo4j.Neo4jConnectionService
import org.batterypakdev.gographdb.go.neo4j.Neo4jUtils


/*
Responsible for creating GoSynonymCollection and GoSynonym nodes
  GoTerm -----1-> GoSynonymCollection ------n-> GoSynonym
 */
object GoSynonymDao {
    /*
     Cypher database templates for Synonym related transactions
     */
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()
    private const val synCollectLoadTemplate = "MERGE (gsc:GoSynonymCollection{go_id: GOID}) " +
            " RETURN gsc.go_id"
    private const val cypherRelationshipTemplate = "MATCH (got:GoTerm), (gsc:GoSynonymCollection) " +
            " WHERE got.go_id = GOID  AND gsc.go_id = GOID " +
            " MERGE (got) - [r:HAS_SYNONYM_COLLECTION] -> (gsc) " +
            " RETURN r"
    private const val synonymLoadTemplate = "MERGE (gos:GoSynonym{synonym_id: SYNID}) " +
            " SET gos += {text: TEXT, type: TYPE} RETURN gos.synonym_id "
    private const val synonymRelationshipTemplate = "MATCH (gsc:GoSynonymCollection), " +
            " (gos:GoSynonym) WHERE gsc.go_id = GOID AND gos.synonym_id = SYNID " +
            " MERGE (gsc) - [r:HAS_SYNONYM] -> (gos) RETURN r"

    /*
    Create the SynonymCollection node
     */
    private fun addSynonymCollectionNode(goId: String): String {
        val loadCypher = synCollectLoadTemplate.replace("GOID", Neo4jUtils.formatQuotedString(goId))
        return Neo4jConnectionService.executeCypherCommand(loadCypher)
    }

    /*
    Create a relationship between the GoTerm and the new SynonymCollection Node
     */
    private fun addSynonymCollRelationship(goId: String) {
        val relCypher = cypherRelationshipTemplate.replace("GOID", Neo4jUtils.formatQuotedString(goId))
        Neo4jConnectionService.executeCypherCommand(relCypher)
    }

    /*
    Create the Synonym node(s) and their relationship to the SynonymCollection Node
     */
   private fun addSynonymNodes(goId: String, synonyms: List<GoSynonym>) {
        var index = 1
        synonyms.forEach { syn ->
            run {
                val synId = Neo4jUtils.formatQuotedString(goId + index.toString())
                val loadCypher = synonymLoadTemplate.replace("SYNID", synId)
                    .replace("TEXT", Neo4jUtils.formatQuotedString(syn.synonymText))
                    .replace("TYPE", Neo4jUtils.formatQuotedString(syn.synonymType))
                Neo4jConnectionService.executeCypherCommand(loadCypher)
                val relCypher = synonymRelationshipTemplate.replace("GOID", Neo4jUtils.formatQuotedString(goId))
                    .replace("SYNID", synId)
                Neo4jConnectionService.executeCypherCommand(relCypher)
                index += 1
            }
        }
    }
    /*
    Public method to persist GO Synonym nodes and relationships
     */
    fun persistGoSynonymData(goTerm: GoTerm){
        if (GoTermDao.goTermNodeExistsPredicate(goTerm.goId)
                .and(goTerm.synonyms.isNotEmpty())) {
            addSynonymCollectionNode(goTerm.goId)
            addSynonymCollRelationship(goTerm.goId)
            addSynonymNodes(goTerm.goId, goTerm.synonyms)
        } else {
            when (GoTermDao.goTermNodeExistsPredicate(goTerm.goId)) {
                true -> logger.atFine().log("GO Term ${goTerm.goId} does not have synonyms")
                false -> logger.atSevere().log("ERROR GO Term ${goTerm.goId} is not in the database")
            }
        }
    }

}