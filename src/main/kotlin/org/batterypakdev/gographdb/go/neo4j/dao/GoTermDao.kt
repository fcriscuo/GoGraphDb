package org.batterypakdev.gographdb.go.neo4j.dao

import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.model.GoTerm
import org.batterypakdev.gographdb.go.neo4j.Neo4jConnectionService
import org.batterypakdev.gographdb.go.neo4j.Neo4jUtils

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
            val merge = cypherLoadTemplate.replace("GOID",
                Neo4jUtils.formatQuotedString(goTerm.goId))
                    .replace("GONAME", Neo4jUtils.formatQuotedString(goTerm.name))
                    .replace("GODEFINITION",Neo4jUtils.formatQuotedString(goTerm.definition))
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
                   " \"$goId\"})  RETURN got.go_id"
       )

    /*
    Function to add a label to a GoTerm node based on the term's GO namespace value
     */
    fun addGoNamespaceLabel(goTerm: GoTerm): String {
        val id = Neo4jUtils.formatQuotedString(goTerm.goId)
        val label = Neo4jUtils.formatQuotedString(goTerm.namespace)
        val labelCypher = "MATCH (got:GoTerm{ go_id: $id }) " +
                " WHERE apoc.label.exists(got,$label)  = false " +
                "    CALL apoc.create.addLabels(got, [$label] ) yield node return node"
        return Neo4jConnectionService.executeCypherCommand(labelCypher)

    }

    /*
    Function to determine if a GoTerm has been loaded into the database
     */
    fun goTermNodeExistsPredicate( goId: String) :Boolean {
        val cypher = "OPTIONAL MATCH (got:GoTerm{go_id: \"$goId\" }) " +
                " RETURN got IS NOT NULL AS PREDICATE"
        try {
            return Neo4jConnectionService.executeCypherCommand(cypher).toBoolean()
        } catch (e:Exception) {
            logger.atSevere().log(e.message.toString())
        }
        return false
    }
}