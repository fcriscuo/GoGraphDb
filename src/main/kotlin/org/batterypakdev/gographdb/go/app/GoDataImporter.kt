package org.batterypakdev.gographdb.go.app

import arrow.core.Either
import com.google.common.base.Stopwatch
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.batterypakdev.gographdb.go.io.GoTermSupplier
import org.batterypakdev.gographdb.go.model.GoTerm
import org.batterypakdev.gographdb.go.neo4j.loader.GoTermLoader

/*
Application to load Gene Ontology (GO) data from an OBO-formatted text file
into a local Neo4j database. GO terms that have been marked as obsolete are not
included.
 */
class GoDataImporter() {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.parseGoOboFile(filename: String) =
        produce<GoTerm> {
            val supplier = GoTermSupplier(filename)
            while (supplier.hasMoreLines()) {
                when (val result = supplier.get()) {
                    is Either.Right -> {
                        println("parse obo ${result.value.goId}")
                        send(result.value)
                        delay(10)
                    }
                    is Either.Left -> {
                        println("Exception: ${result.value.message}")
                    }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.filterGoTerms(goterms: ReceiveChannel<GoTerm>) =
        produce<GoTerm> {
            for (goterm in goterms){
                if (goterm.definition.uppercase().contains("OBSOLETE").not()) {
                    send(goterm)
                    delay(10)
                } else {
                    logger.atFine().log("***** GO Term: ${goterm.goId} is marked as obsolete and will not be loaded")
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.loadGoTerms(goterms: ReceiveChannel<GoTerm>) =
        produce<String> {
            for (goterm in goterms) {
                GoTermLoader.loadGoTerm(goterm)
                send(goterm.goId)
            }
        }
    fun importGoTerms(filename: String) = runBlocking {
        var nodeCount = 0
        val stopwatch = Stopwatch.createStarted()
        val goIds = loadGoTerms(filterGoTerms(parseGoOboFile(filename)))
        for (goId in goIds){
            println("GoTerm: $goId  has been loaded")
            nodeCount +=1
        }
        logger.atInfo().log("GoDataImporter loaded $nodeCount " +
                " GoTerm nodes in " +
                "${stopwatch.elapsed(java.util.concurrent.TimeUnit.SECONDS)} seconds")
    }
}

fun main(args: Array<String>) {
    val filename = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    GoDataImporter().importGoTerms(filename)
}
