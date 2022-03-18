package org.batterypakdev.gographdb.go.model

import org.batteryparkdev.neo4j.service.Neo4jUtils
import org.batteryparkdev.placeholder.model.NodeIdentifier
import org.batteryparkdev.placeholder.model.PlaceholderNode

data class GoTerm(
    val goId: String, val namespace: String, val name: String,
    val definition: String,
    val pubmedPlaceholders: List<PlaceholderNode>,
    val synonyms: List<GoSynonym>,
    val relationshipList: List<Relationship>,
    val xrefList: List<Xref>
) {
    fun isValid(): Boolean =
        (goId.isNotBlank().and(name.isNotBlank().and(namespace.isNotBlank())))

    companion object : AbstractModel {
        /*
        Function to process a list of text lines encompassing a GO Term
         */
        fun generateGoTerm(termlines: List<String>): GoTerm {
            var goId: String = " "
            var goName: String = " "
            var goNamespace: String = " "
            var goDefinition: String = ""
            termlines.forEach { line ->
                run {
                    when (resolveFirstWord(line)) {
                        "id" -> goId = line.substring(line.indexOf("GO:"))
                        "name" -> goName = line.substring(6)
                        "namespace" -> goNamespace = line.substring(11)
                        "def" -> goDefinition = resolveQuotedString(line)
                        else -> {}  // ignore other lines
                    }
                }
            }

            return GoTerm(
                goId, goNamespace, goName, goDefinition,
                resolvePubMedIdentifiers(goId, termlines), GoSynonym.resolveSynonyms(termlines),
                Relationship.resolveRelationships(termlines), Xref.resolveXrefs(termlines)
            )
        }

        /*
    Function to resolve a List of PubMed Ids from a GO Term
    Input parameter is a List of lines comprising a complete
    GO Term
    Format is PMID:7722643
     */
        fun resolvePubMedIdentifiers(goId: String, lines: List<String>): List<PlaceholderNode> {
            var pmidSet = mutableSetOf<Int>()
            val pmidLabel = "PMID"
            val pmidLength = 12
            val parentNode = NodeIdentifier(
                "GoTerm", "go_id",
                goId
            )
            lines.stream().filter { line -> line.contains(pmidLabel) }
                .forEach { line ->
                    run {
                        var index = 0
                        var text = line
                        while (index != -1) {
                            index = text.indexOf(pmidLabel)
                            if (index >= 0)
                                pmidSet.add(parsePMID(text.substring(index + 5)))
                            text = text.substring(index + 1)
                        }
                    }
                }
            val pbIdentifiers = mutableListOf<PlaceholderNode>()
            pmidSet.forEach { id ->
                run {
                    val childNode = NodeIdentifier(
                        "Publication", "pub_id",
                       id.toString(), "PubMed"
                    )
                    pbIdentifiers.add(PlaceholderNode(parentNode, childNode, "HAS_PUBLICATION", "title"))
                }
            }
            return pbIdentifiers.toList()
        }
    }
}

data class GoSynonym(
    val synonymText: String,
    val synonymType: String
) {
    companion object : AbstractModel {
        fun resolveSynonyms(termLines: List<String>): List<GoSynonym> {
            val synonyms = mutableListOf<GoSynonym>()
            termLines.filter { line -> line.startsWith("synonym:") }
                .forEach { syn -> synonyms.add(resolveSynonym(syn)) }
            return synonyms.toList()
        }

        private fun resolveSynonym(line: String): GoSynonym {
            val text = resolveQuotedString(line)
            val startIndex = line.lastIndexOf('"') + 2
            val endIndex = startIndex + line.substring(startIndex).indexOf(' ')
            val type = line.substring(startIndex, endIndex)
            return GoSynonym(text, type)
        }
    }
}

data class Relationship(
    val type: String,
    val qualifier: String = "",
    val targetId: String,
    val description: String
) {
    companion object : AbstractModel {

        private fun relationshipFilter(line: String): Boolean =
            when (resolveFirstWord(line)) {
                "is_a", "intersection_of", "relationship" -> true
                else -> false
            }

        fun resolveRelationships(termlines: List<String>): List<Relationship> {
            val relationships = mutableListOf<Relationship>()
            termlines.filter { line -> relationshipFilter(line) }
                .forEach { line -> relationships.add(resolveRelationship(line)) }
            return relationships
        }

        private fun resolveRelationship(line: String): Relationship {
            val type = resolveFirstWord(line)
            val targetStart = line.indexOf("GO:")
            val targetId = line.substring(targetStart, targetStart + 10)
            val description = line.substring(targetStart + 13)
            return Relationship(type, resolveQualifier(line), targetId, description)
        }

        private fun resolveQualifier(line: String): String {
            val colonIndex = line.indexOf(":") + 2
            val goIndex = line.indexOf("GO:")
            return line.substring(colonIndex, goIndex).trim()
        }
    }
}

data class Xref(
    val source: String,
    val id: String,
    val description: String = ""
) {
    companion object : AbstractModel {
        fun resolveXrefs(termLines: List<String>): List<Xref> {
            val xrefs = mutableListOf<Xref>()
            termLines.filter { line -> line.startsWith("xref") }
                .forEach { line -> xrefs.add(resolveXref(line)) }
            return xrefs
        }

        private fun resolveXref(line: String): Xref {
            val sourceAndId = line.split(" ")[1].split(":")
            val source = sourceAndId[0]
            val id = sourceAndId[1]
            return Xref(source, id, resolveQuotedString(line))

        }
    }
}


fun main() {
    val synonyms = listOf<String>(
        "synonym: \"activation of receptor internalization\" NARROW []",
        "synonym: \"stimulation of receptor internalization\" NARROW []",
        "synonym: \"up regulation of receptor internalization\" EXACT []",
        "synonym: \"up-regulation of receptor internalization\" EXACT []",
        "synonym: \"upregulation of receptor internalization\" EXACT []"
    )
    GoSynonym.resolveSynonyms(synonyms).forEach { syn ->
        println("${syn.synonymText}   ${syn.synonymType}")
    }
    val relationships = listOf<String>(
        "is_a: GO:0006355 ! regulation of transcription, DNA-templated",
        "is_a: GO:1903047 ! mitotic cell cycle process",
        "intersection_of: GO:0006355 ! regulation of transcription, DNA-templated",
        "intersection_of: part_of GO:0000082 ! G1/S transition of mitotic cell cycle",
        "relationship: part_of GO:0000082 ! G1/S transition of mitotic cell cycle"
    )
    Relationship.resolveRelationships(relationships).forEach { rel ->
        println("${rel.type}   ${rel.qualifier}  ${rel.targetId}   ${rel.description}")
    }

    val xrefs = listOf<String>(
        "xref: EC:3.2.1.108",
        "xref: MetaCyc:LACTASE-RXN",
        "xref: Reactome:R-HSA-189062 \"lactose + H2O => D-glucose + D-galactose\"",
        "xref: Reactome:R-HSA-5658001 \"Defective LCT does not hydrolyze Lac\"",
        "xref: RHEA:10076"
    )
    Xref.resolveXrefs(xrefs).forEach { xref ->
        println("${xref.source}   ${xref.id}  ${xref.description}")
    }
}

