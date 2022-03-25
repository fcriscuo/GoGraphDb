package org.batterypakdev.gographdb.go.app

import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.*
import org.batterypakdev.gographdb.go.neo4j.defineGoDatabaseConstraints
import org.batterypakdev.gographdb.go.neo4j.loader.GoTermLoader
import org.batteryparkdev.neo4j.service.Neo4jUtils
import org.batteryparkdev.publication.pubmed.loader.AsyncPubMedPublicationLoader
import kotlin.coroutines.CoroutineContext

/*
Application to load Gene Ontology (GO) data from an OBO-formatted text file
into a local Neo4j database. GO terms that have been marked as obsolete are not
included.
 */
class GoDataImporter(val goFilename:String): CoroutineScope {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch() {
        delay(2000)
    }

    // creating local CoroutineContext
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    // extension function
    // source: https://stackoverflow.com/questions/53921470/how-to-run-two-jobs-in-parallel-but-wait-for-another-job-to-finish-using-kotlin
    fun <T> CoroutineScope.asyncIO(ioFun: () -> T) = async(Dispatchers.IO) { ioFun() }
    fun <T> CoroutineScope.asyncDefault(defaultFun: () -> T) = async(Dispatchers.Default) { defaultFun() }

    private val nodeNameList = listOf<String>("GoTerm","GoSynonymCollection", "GoSynonym")

    private fun loadGeneOntologyData():String {
        GoTermLoader.loadGoTerms(goFilename)
        return ("Gene Ontology data import task completed")
    }


    fun deleteGoNodes():String {
        nodeNameList.forEach { nodeName -> Neo4jUtils.detachAndDeleteNodesByName(nodeName) }
        return "GoTerm-related nodes & relationships deleted"
    }

    /*
  Function to define the asynchronous workflow
   */
    @OptIn(DelicateCoroutinesApi::class)
    fun loadData() {
        GlobalScope.launch {
            val task01 = asyncDefault { loadPubmedJob() }
            val task02 = asyncIO {  loadGeneOntologyData() }
            onDone(task01.await(), task02.await())
        }
    }
    fun onDone(job1Result:String, job2Result:String) {
        logger.atInfo().log("Executing onDone function")
        logger.atInfo().log("task01 = $job1Result ")
        logger.atInfo().log("task02 = $job2Result ")
        job.cancel()
    }

    private fun loadPubmedJob(): String {  // job 1
        logger.atInfo().log("1 - Starting PubMed loader")
        val taskDuration = 172_800_000L
        val timerInterval = 60_000L
        val scanTimer = AsyncPubMedPublicationLoader.scheduledPlaceHolderNodeScan(timerInterval)
        try {
            Thread.sleep(taskDuration)
        } finally {
            scanTimer.cancel();
        }
        return "PubMed data loaded"
    }
}

fun main(args: Array<String>): Unit = runBlocking {
    val filename = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    println("WARNING: Invoking this application will delete all Gene Ontology data from the database")
    println("There will be a 20 second delay period to cancel this execution (CTRL-C) if this is not your intent")
    // Thread.sleep(20_000L)
    println("Gene Ontology data will now be loaded from: $filename")
    defineGoDatabaseConstraints()
   val loader = GoDataImporter(filename)
    loader.deleteGoNodes()
    loader.loadData()
    println("Gene Ontology data has been loaded into Neo4j")
    awaitCancellation()
}
