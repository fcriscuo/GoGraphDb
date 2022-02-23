package org.batterypakdev.gographdb.go.pubmed.model

/*
Represents a placeholder for a PubMed publication that will
be completed by an asynchronous PubMed mining application
 */
data class GoPubMedIdentifier (val pubmedId: Int, val  goId: String, val label: String ="Publication") {

}