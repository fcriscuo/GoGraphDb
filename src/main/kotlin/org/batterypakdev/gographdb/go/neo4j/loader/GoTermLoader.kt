package org.batterypakdev.gographdb.go.neo4j.loader

import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.batterypakdev.gographdb.go.model.GoTerm
import org.batterypakdev.gographdb.go.neo4j.dao.GoRelationshipDao
import org.batterypakdev.gographdb.go.neo4j.dao.GoSynonymDao
import org.batterypakdev.gographdb.go.neo4j.dao.GoTermDao

/*
Responsible for loading Gene Ontology nodes and relationships into
the Neo4j database
 */
object GoTermLoader {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    /*
    Persist the GoTerm node
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.persistGoTermNode(goTerm: GoTerm) =
        produce<GoTerm>{
            GoTermDao.loadGoTermNode(goTerm)
            send(goTerm)
            delay(10)
        }
    /*
    Persist the GO Term's synonyms
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.persistGoTermSynonyms(goTerms:ReceiveChannel<GoTerm>) =
        produce<GoTerm> {
           for(goTerm in goTerms){
               GoSynonymDao.persistGoSynonymData(goTerm)
               send(goTerm)
               delay(20)
           }
        }

    /*
    Persist the GO Term's relationships to other GO Terms
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun CoroutineScope.persistGoTermRelationships(goTerms:ReceiveChannel<GoTerm>) =
        produce<String> {
            for (goTerm in goTerms){
                GoRelationshipDao.loadGoTermRelationships(goTerm)
                send(goTerm.goId)
            }
        }

    /*
    Public function to persist GO Terms into the Neo4j database
     */
    fun loadGoTerm(goTerm: GoTerm) = runBlocking {
        val goId = persistGoTermRelationships(persistGoTermSynonyms(persistGoTermNode(goTerm)))
        logger.atInfo().log("GO term: ${goId} has been loaded")
    }

}