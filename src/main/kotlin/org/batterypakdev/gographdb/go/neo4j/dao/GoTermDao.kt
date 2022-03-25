package org.batterypakdev.gographdb.go.neo4j.dao

import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.model.GoTerm
import org.batteryparkdev.neo4j.service.Neo4jConnectionService
import org.batteryparkdev.neo4j.service.Neo4jUtils
import org.batteryparkdev.nodeidentifier.model.NodeIdentifier


/*
Responsible for data access operations for GoTerm nodes in the neo4j database

 */
object GoTermDao {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()
    private const val cypherLoadTemplate = "MERGE (got:GoTerm{ go_id: GOID}) " +
            " SET got += {go_name:GONAME, go_definition:GODEFINITION} " +
            " RETURN got.go_id"

    fun loadGoTermNode( goTerm: GoTerm): String {
        try {
            val merge = cypherLoadTemplate.replace("GOID",Neo4jUtils.formatPropertyValue( goTerm.goId))
                    .replace("GONAME",Neo4jUtils.formatPropertyValue(goTerm.name))
                    .replace("GODEFINITION",Neo4jUtils.formatPropertyValue(goTerm.definition))
            return Neo4jConnectionService.executeCypherCommand(merge)
        } catch (e: Exception) {
            logger.atSevere().log(e.message)
            println("Failed to merge GoTerm: ${goTerm.goId} ${goTerm.name}")
        }
        return " "
    }

    /*
    Function to create a placeholder GoTerm which allows inter-GoTerm
    relationships to be defined before the target GoTerm is fully loaded
     */
    fun createPlaceholderGoTerm(goId: String):String =
       Neo4jConnectionService.executeCypherCommand(
           "MERGE (got:GoTerm{go_id: " +
                  " ${Neo4jUtils.formatPropertyValue(goId)}})  RETURN got.go_id"
       )

    /*
    Function to add a label to a GoTerm node based on the term's GO namespace value
     */
    fun addGoNamespaceLabel(goTerm: GoTerm) {
        val id =goTerm.goId
        val label = goTerm.namespace
        val nodeId = NodeIdentifier("GoTerm", "go_id",
           goTerm.goId,
            label)
        Neo4jUtils.addLabelToNode(nodeId)
    }

    /*
    Function to determine if a GoTerm has been loaded into the database
     */
    fun goTermNodeExistsPredicate( goId: String) :Boolean =
        Neo4jUtils.nodeExistsPredicate( NodeIdentifier("GoTerm", "go_id", goId))
}