package org.batterypakdev.gographdb.go.neo4j.dao

import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.model.GoTerm
import org.batterypakdev.gographdb.go.model.Relationship
import org.batterypakdev.gographdb.go.neo4j.Neo4jConnectionService
import org.batterypakdev.gographdb.go.neo4j.Neo4jUtils

/*
Responsible for creating and managing neo4j labeled relationships between
GoTerm nodes
 */
object GoRelationshipDao {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    private const val relationshipCypher =
        "MATCH (got1:GoTerm), (got2:GoTerm) WHERE " +
                "got1.go_id = SOURCE  AND got2.go_id = TARGET " +
                " MERGE (got1) - [r:RELATIONSHIP] -> (got2) " +
                " ON CREATE SET " +
                " r+= {description: DESCRIPTION}" +
                " RETURN r"

    private fun loadGoTermRelationship(goId: String, goRel: Relationship) {
        val cypher = relationshipCypher.replace("SOURCE", Neo4jUtils.formatQuotedString(goId))
            .replace("TARGET", Neo4jUtils.formatQuotedString(goRel.targetId))
            .replace("RELATIONSHIP", goRel.type.uppercase())
            .replace("DESCRIPTION", Neo4jUtils.formatQuotedString(goRel.description))
        Neo4jConnectionService.executeCypherCommand(cypher)
    }

    /*
    Public function to create a Relationship node for each of a GO Term's relationships
    A placeholder GoTerm node is created for the relationship target node if that term
    has not been loaded yet
     */
    fun loadGoTermRelationships(goTerm: GoTerm) {
        val goId = goTerm.goId
        goTerm.relationshipList.forEach { rel ->
            run {
                if (GoTermDao.goTermNodeExistsPredicate(rel.targetId).not()) {
                    GoTermDao.createPlaceholderGoTerm(rel.targetId)
                }
                loadGoTermRelationship(goId, rel)
            }
        }
    }

}