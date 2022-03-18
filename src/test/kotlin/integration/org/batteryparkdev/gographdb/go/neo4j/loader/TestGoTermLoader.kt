package org.batteryparkdev.gographdb.go.neo4j.loader

import org.batterypakdev.gographdb.go.neo4j.loader.GoTermLoader

class TestGoTermLoader {

}
fun main(args: Array<String>) {
    val filename = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    GoTermLoader.loadGoTerms(filename)
}