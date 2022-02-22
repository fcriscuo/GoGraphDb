package org.batterypakdev.gographdb.go.app

import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.neo4j.defineGoDatabaseConstraints
import org.batterypakdev.gographdb.go.neo4j.loader.GoTermLoader

/*
Application to load Gene Ontology (GO) data from an OBO-formatted text file
into a local Neo4j database. GO terms that have been marked as obsolete are not
included.
 */
class GoDataImporter() {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    fun loadGeneOntologyData(filename:String) =
        GoTermLoader.loadGoTerms(filename)

}

fun main(args: Array<String>) {
    val filename = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    defineGoDatabaseConstraints()
    GoDataImporter().loadGeneOntologyData(filename)
}
