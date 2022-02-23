package org.batterypakdev.gographdb.go.neo4j

import com.google.common.flogger.FluentLogger

/*
A collection of Neo4j database constraint definitions in Cypher
These constraints should be defined prior to loading the database with
any initial data.
 */
val constraints by lazy {
    listOf<String>(
        "CREATE CONSTRAINT unique_goterm_id IF NOT EXISTS ON (got: GoTerm) ASSERT got.go_id IS UNIQUE",
        "CREATE CONSTRAINT unique_gosyn_coll_id IF NOT EXISTS ON (gsc: GoSynonymCollection) ASSERT gsc.go_id IS UNIQUE",
        "CREATE CONSTRAINT unique_pubmed_id IF NOT EXISTS ON (pub: Publication) ASSERT pub.pubmed_id IS UNIQUE",
        "CREATE CONSTRAINT unique_gosyn_id IF NOT EXISTS ON (gos: GoSynonym) ASSERT gos.synonym_id IS UNIQUE"
    )
}

val logger: FluentLogger = FluentLogger.forEnclosingClass();

fun defineGoDatabaseConstraints() {
    constraints.forEach {
        Neo4jConnectionService.defineDatabaseConstraint(it)
        logger.atInfo().log("Constraint: $it  has been defined")
    }
}

// stand-alone invocation
fun main(){
    defineGoDatabaseConstraints()
}