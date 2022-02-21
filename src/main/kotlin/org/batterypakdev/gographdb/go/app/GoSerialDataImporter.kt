package org.batterypakdev.gographdb.go.app

import arrow.core.Either
import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.io.GoTermSupplier
import org.batterypakdev.gographdb.go.neo4j.loader.GoTermLoader

/*
Kotlin application responsible for loading Gene Ontology terms
into a local Neo4j database
 */
class GoSerialDataImporter {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()

    fun loadData(filename: String) {
        val supplier = GoTermSupplier(filename)
        while (supplier.hasMoreLines()) {
            println("Accessing supplier....")
            when (val result = supplier.get()) {
                is Either.Right -> {
                    val goterm = result.value
                    if (goterm.definition.uppercase().contains("OBSOLETE").not()) {
                        GoTermLoader.loadGoTerm(goterm)
                    } else {
                        println("+++++GO term: ${goterm.goId} has been marked obsolete and was not loaded")
                    }
                }
                is Either.Left -> {
                    println("Exception: ${result.value.message}")
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    val filename = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    GoSerialDataImporter().loadData(filename)
    println("FINIS....")
}