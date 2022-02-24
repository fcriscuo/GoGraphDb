## GoGraphDb

GoGraphDb is a Kotlin 1.6 application that will import Gene Ontology (GO)
(http://geneontology.org/)
terms into a 
local Neo4j 4.4 (https://neo4j.com/) database. The input for this process is a 
text file in OBO 1.4 format (https://owlcollab.github.io/oboformat/doc/obo-syntax.html)
downloaded from the Gene Ontology website
(http://current.geneontology.org/ontology/go.obo). 
GO terms that are marked as obsolete, are not imported into the Neo4j database.

### Requirements
The application requires the user to define two (2) system environment properties,
NEO4J_ACCOUNT and NEO4J_PASSWORD. This allows for easier code sharing without
exposing Neo4j credentials. The application logs all Neo4j CYPHER commands to a log
file in the /tmp/logs/neo4j directory. The filenames for these log files contain
a timestamp component, so they are not overwritten by subsequent executions. The underlying Java Virtual Machine
for the Kotlin runtime environment is JVM 18.

### Execution
The application's main class is org.batteryparkdev.gographdb.go.GoDataImporter. The single input argument
is the full path name for the OBO-formatted input file.

### PubMed Support
GO terms may include PubMed (https://pubmed.ncbi.nlm.nih.gov/) identifiers that associate a GO term with 
a supporting publication.
The application will create a placeholder Publication node in the Neo4j database and complete a relationship to
the appropriate GoTerm node(s). It is intended that these empty Publication nodes be completed by an asynchronous 
data mining application. The rationale for a specialized importer is that NCBI limits web requests to 3 per second
(10 per second for registered users).This significantly expands the runtime for large data imports and introduces
reliability issues due to NCBI resource unavailability. The current design allows for the primary GO data to be
imported in a reasonable time period, while the Publication nodes ar completed asynchronously.

###Citations:
1.Ashburner et al. Gene ontology: tool for the unification of biology. Nat Genet. May 2000;25(1):25-9.
(https://www.ncbi.nlm.nih.gov/labs/pmc/articles/PMC3037419/)
2.The Gene Ontology resource: enriching a GOld mine. Nucleic Acids Res. Jan 2021;49(D1):D325-D334. 
(https://www.ncbi.nlm.nih.gov/labs/pmc/articles/PMC7779012/)