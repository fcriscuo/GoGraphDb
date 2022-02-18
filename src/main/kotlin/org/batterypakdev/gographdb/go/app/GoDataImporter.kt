package org.batterypakdev.gographdb.go.app

import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.flow.*
import org.batterypakdev.gographdb.go.io.OboFileLineSupplier
import org.batterypakdev.gographdb.go.io.Termlines
import org.batterypakdev.gographdb.go.model.GoTerm
import org.batterypakdev.gographdb.go.neo4j.loader.GoTermLoader

class GoDataImporter(filename: String) {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()
    private val supplier = OboFileLineSupplier(filename)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.produceTermlines() =
        produce<Termlines> {
            advanceToNextTerm()
            var termlines = collectLines()
            while (termlines.hasContent()) {
                send(termlines)
                advanceToNextTerm()
                termlines = collectLines()
                delay(20)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.mapTermlineToGoTerm(termlines: ReceiveChannel<Termlines>) =
        produce<GoTerm> {
            for (termline in termlines) {
                val lines = termline.getTermlinesContent()
                send(GoTerm.generateGoTerm(lines))
                delay(20)
            }
        }

    private fun produceGoTermFlow(goterms: ReceiveChannel<GoTerm>): Flow<GoTerm> = flow {
        for (goterm in goterms) {
            if (goterm.definition.uppercase().contains("OBSOLETE").not()) {
                emit(goterm)
            } else {
               logger.atFine().log("***** GO Term: ${goterm.goId} is marked as obsolete and will not be loaded")
            }
        }
    }.flowOn(Dispatchers.Default)

    fun loadGoTerms() = runBlocking {
        val goterms = produceGoTermFlow(
            mapTermlineToGoTerm(
                produceTermlines()
            )
        )
        goterms.collect {
            GoTermLoader.loadGoTerm(it)
        }
    }

    fun collectLines(): Termlines {
        val termlines = Termlines()
        var line = supplier.get()
        while (line.equals("EOF").not().and(line.isNotBlank())) {
            termlines.addTermlines(line)
            line = supplier.get()
        }
        return termlines
    }

    private fun advanceToNextTerm() {
        var line: String = " "
        while (line.startsWith("[Term]").not().and(line.isNotBlank())) {
            line = supplier.get()
        }
    }
}

fun main(args: Array<String>) {
    val filename = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    GoDataImporter(filename).loadGoTerms()
}
