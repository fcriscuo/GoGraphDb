package org.batteryparkdev.pubmedref.neo4j

import com.google.common.flogger.FluentLogger
import org.batteryparkdev.cosmicgraphdb.neo4j.Neo4jConnectionService

/*
A collection of Neo4j database constraint definitions in Cypher
These constraints should be defined prior to loading the database with
any initial data.
 */
val constraints by lazy {
    listOf<String>(
        "CREATE CONSTRAINT unique_goterm_id IF NOT EXISTS ON (got: GoTerm) ASSERT got.go_id IS UNIQUE",
        "CREATE CONSTRAINT unique_gosyn_coll_id IF NOT EXISTS ON (gsc: GoSynonymCollection) ASSERT gsc.go_id IS UNIQUE",
        "CREATE CONSTRAINT unique_gosyn_id IF NOT EXISTS ON (gos: GoSynonym) ASSERT gos.synonym_id IS UNIQUE"
    )
}

val logger: FluentLogger = FluentLogger.forEnclosingClass();

fun defineConstraints() {
    constraints.forEach {
        Neo4jConnectionService.defineDatabaseConstraint(it)
        logger.atInfo().log("Constraint: $it  has been defined")
    }
}

// stand-alone invocation
fun main(){
    defineConstraints()
}