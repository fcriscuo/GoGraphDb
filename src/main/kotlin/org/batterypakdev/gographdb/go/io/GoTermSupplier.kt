package org.batterypakdev.gographdb.go.io

import arrow.core.Either
import com.google.common.flogger.FluentLogger
import org.batterypakdev.gographdb.go.model.GoTerm
import java.io.File
import java.util.*
import java.util.function.Supplier

/*
Responsible for parsing complete Gene Ontology Terms from
a specified OBO-formatted file
Utilizes an intermediate object, Termlines, to accommodate the
variable number of synonyms and relationships in a GO Term
 */
class GoTermSupplier(filename: String) : Supplier<Either<Exception, GoTerm>> {
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()
    private val oboScanner = Scanner(File(filename))
    private val termLabel = "[Term]"

   fun hasMoreLines():Boolean = oboScanner.hasNextLine()

    override fun get(): Either<Exception, GoTerm> {
        val goTerm = generateGoTerm()
        logger.atFine().log("Supplier returning goterm ${goTerm.goId}")
        return when (goTerm.isValid()) {
            true -> Either.Right(goTerm)
            false -> Either.Left(Exception("EOF"))
        }
    }

    private fun scanLine(): String =
        when  (oboScanner.hasNextLine()) {
            true -> oboScanner.nextLine()
            false -> "EOF"
        }

    private fun generateGoTerm(): GoTerm {
        advanceToNextTerm()
        val termlines = collectLines()
        return GoTerm.generateGoTerm(termlines.getTermlinesContent())
    }

    private fun collectLines(): Termlines {
        val termlines = Termlines()
        var line = scanLine()
        while (line.equals("EOF").not().and(line.isNotBlank())) {
            termlines.addTermlines(line)
            line = scanLine()
        }
        return termlines
    }

    private fun advanceToNextTerm() {
       var line = " "
      //  while (line.startsWith(termLabel).not().and(line.isNotBlank())) {
        while (line.startsWith(termLabel).not()) {
            line = scanLine()
        }
    }
}

/*
Main function for integration testing
 */
fun main(args: Array<String>) {
    val filePathName = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    println("Processing OBO-formatted file: $filePathName")
    val supplier = GoTermSupplier(filePathName)
    for (i in 1..200) {
      when ( val result = supplier.get()) {
          is Either.Right -> {
              val goTerm = result.value
              println("GoTerm:  ${goTerm.goId}  ${goTerm.name}")
          }
          is Either.Left -> {
              println("Exception: ${result.value.message}")
          }
      }
    }
}

data class Termlines(
    private val lines: MutableList<String> = mutableListOf()
) {
    fun getTermlinesContent(): List<String> = lines.toList()

    fun addTermlines(line: String) = lines.add(line)

    fun hasContent(): Boolean = lines.isNotEmpty()

}