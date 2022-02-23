package org.batterypakdev.gographdb.go.neo4j.dao

import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.neo4j.Neo4jConnectionService
import org.batterypakdev.gographdb.go.pubmed.model.GoPubMedIdentifier

/*
Responsible for creating Publication (i.e. PubMed) placeholder nodes in the
Neo4j database. Initially, these nodes only contain the PubMed id and have a relationship
to one or more GoTerm nodes. The node's remaining attributes (tile, DOI, etc.)
are meant to be completed by an asynchronous PubMed data mining application.
This division is to accommodate NCBI's maximum request rate (3 or 10 requests/second)
without impacting the importing of Gene Ontology data
 */
object GoPubMedDao {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    private const val mergeTemplate = "MERGE (pub:Publication {pubmed_id: PUBMEDID}) " +
            " SET pub += { title: \" \"} RETURN pub.pubmed_id"

    private const val relationshipTemplate = "MATCH (got:GoTerm)," +
            "(pub:Publication) WHERE got.go_id=\"GOID\" " +
            "AND pub.pubmed_id= PUBMEDID " +
            " MERGE (got) -[r:HAS_PUBLICATION] -> (pub) RETURN r"

    fun loadGoPublication(pubId: GoPubMedIdentifier) {
        val pubmedId = pubId.pubmedId
        if (publicationNodeExistsPredicate(pubmedId).not()) {
            val cypher = mergeTemplate.replace("PUBMEDID", pubmedId.toString())
            try {
                Neo4jConnectionService.executeCypherCommand(cypher)
            } catch (e: Exception) {
                logger.atSevere().log(e.message.toString())
            }
        }
        loadPublicationRelationship(pubId.pubmedId, pubId.goId)
    }

    /*
    Function to create a relationship between a GoTerm node and a
    Publication node
     */
    private fun loadPublicationRelationship(pubmedId: Int, goId: String) {
        if(publicationNodeExistsPredicate(pubmedId).and(GoTermDao.goTermNodeExistsPredicate(goId))) {
            val cypher = relationshipTemplate.replace("GOID",goId)
                .replace("PUBMEDID",pubmedId.toString())
            Neo4jConnectionService.executeCypherCommand(cypher)
        } else {
            logger.atSevere().log("WARNING: unable to establish a relationship between GoTerm: $goId " +
                    " and PubMed Id $pubmedId")
        }
    }

    /*
    Function to determine if a Publication node with the same
    PubMed id ahs already been created
     */
    fun publicationNodeExistsPredicate(pubmedId: Int): Boolean {
        val cypher = "OPTIONAL MATCH (p:Publication{pubmed_id: $pubmedId }) " +
                " RETURN p IS NOT NULL AS Predicate"
        return try {
            Neo4jConnectionService.executeCypherCommand(cypher).toBoolean()
        } catch (e: Exception) {
            logger.atSevere().log(e.message.toString())
            false
        }
    }

}