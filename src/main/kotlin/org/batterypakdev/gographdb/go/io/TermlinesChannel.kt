package org.batterypakdev.gographdb.go.io

import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.batterypakdev.gographdb.go.model.GoTerm

class TermlinesChannel( filename:String) {
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
            for(termline in termlines) {
                val lines = termline.getTermlinesContent()
                send(GoTerm.generateGoTerm(lines))
                delay(20)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun CoroutineScope.formatGoTerm(goterms: ReceiveChannel<GoTerm>) =
        produce<String> {
            for (goterm in goterms){
               send("GOTerm  id: ${goterm.goId}  name: ${goterm.name}   PMIDs: ${goterm.pubmedIdentifiers}")
                delay(10)
            }
        }

    fun displayGoTerms() = runBlocking {
        var count = 0
        val lines =  formatGoTerm(
            mapTermlineToGoTerm(
                produceTermlines()
            )
        )
        for (line in lines){
            count += 1
            println("$count:  $line")
        }
    }

    fun collectLines():Termlines {
        val termlines = Termlines()
        var line = supplier.get()
        while(line.equals("EOF").not().and(line.isNotBlank())) {
            termlines.addTermlines(line)
            line = supplier.get()
        }
        return termlines
    }

     private fun advanceToNextTerm() {
         var line:String =" "
         while(line.startsWith("[Term]").not().and(line.isNotBlank())) {
             line = supplier.get()
         }
     }

}

fun main(args: Array<String>) {
    val filename = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    TermlinesChannel(filename).displayGoTerms()
}