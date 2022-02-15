package org.batterypakdev.gographdb.go.model

import org.batterypakdev.gographdb.go.model.GoTerm.Companion.resolvePubMedIdentifiers
import org.batterypakdev.gographdb.go.model.GoTerm.Companion.resolveQuotedString

/**
 * Created by fcriscuo on 2021Jul29
 */
interface AbstractModel {
    /*
   def isValidString(x:String):Boolean = x != null && x.trim.length> 0
   */
    fun isValidString(s: String?):Boolean =  !s.isNullOrBlank()

    fun parseStringOnSpace(s:String): List<String> = parseStringOnDelimiter(s, " ")

    fun parseStringOnSemiColon(s: String): List<String> = parseStringOnDelimiter(s, ";")

    fun parseStringOnColon(s: String): List<String> = parseStringOnDelimiter(s, ":")

    fun parseStringOnPipe(s: String): List<String> = parseStringOnDelimiter(s, "|")

    fun parseStringOnComma(s: String): List<String> = parseStringOnDelimiter(s, ",")

    fun parseStringOnTab(s: String): List<String> = parseStringOnDelimiter(s, "\t")

    fun parseStringOnEquals(s:String): Pair<String,String>? {
        val list = parseStringOnDelimiter(s,"=")
        if ( list.size == 2)
            return Pair(list[0], list[1])
        return null
    }

    fun resolveFirstWord(text:String):String =
        parseStringOnSpace(text).first().replace(":","")

    /*
    Function to remove the first word in space delimited String
    Used to remove GO label (e.g. name)
     */
    fun removeFirstWord(text:String):String =
        mutableListOf(parseStringOnSpace(text)).removeFirst()
            .joinToString(" ")

    /*
    Function to resolve the first quoted String in a supplied text
    Quote marks are not included
    Used to identify quote text in GO terms
     */
    fun resolveQuotedString(text: String): String {
        val firstQuote = text.indexOf('"')
        val lastQuote = text.lastIndexOf('"')
        return when (lastQuote>firstQuote) {
            true ->text.substring(firstQuote+1,lastQuote)
            false ->""
        }
    }

    /*
    The PubMed Id can vary in size from 6 to 8 digits
     */
    fun parsePMID(pmid:String):Int {
        var id:String =""
        var index = 0
        while(pmid[index] in '0'..'9') {
            id = id.plus(pmid[index])
            index +=1
        }
        return id.toInt()
    }


    fun convertYNtoBoolean(ynValue:String): Boolean =
        ynValue.lowercase() == "y"

    fun isNumeric(str: String) = str.all { it in '0'..'9' }

    private fun parseStringOnDelimiter(s: String, delimiter: String): List<String> =
        when(isEmptyString(s)) {
            true  -> emptyList()
           false -> s.replace("\"", "").split(delimiter).map { it.trim() }
        }

    fun isHumanSpeciesId(speciesId: String): Boolean = speciesId.trim().equals("9606")

    fun isEmptyString(s: String): Boolean = s.trim().isEmpty()

    fun booleanFromInt(i: Int): Boolean = i == 1

    private fun reduceListToDelimitedString(list: List<String>, delimiter: String): String =
        list.joinToString { delimiter }

    fun reduceListToPipeDelimitedString(list: List<String>) = reduceListToDelimitedString(list, "|")

    /*
    Function to return last element in a List
    Needed to support Java clients
     */
    fun <T> getLastListElement(list: List<T>): T = list.last()

    fun parseDoubleString(ds: String): Double = ds.replace(',', '.').toDouble()

    /*
    Function to convert a floating point String into a Float
    returns 0.0 if the String is not in a floating point format
     */
    fun parseValidFloatFromString(fs: String): Float =
        when (Regex("[-+]?[0-9]*\\.?[0-9]+").matches(fs)) {
            true -> fs.toFloat()
            else -> 0.0F
        }

    fun parseValidDoubleFromString(fs: String): Double =
        when (Regex("[-+]?[0-9]*\\.?[0-9]+").matches(fs)) {
            true -> fs.toDouble()
            else -> 0.0
        }
    /*
    Function to convert an Integer String to an Integer
    returns 0 if the field is not numeric
     */

    fun parseValidIntegerFromString(s: String): Int =
        when (s.toIntOrNull()) {
            null -> 0
            else  -> s.toInt()
        }
    /*
   Double quotes (i.e. ") inside a text field causes Cypher
   processing errors
    */
    fun removeInternalQuotes(text: String): String =
        text.replace("\"", "'")

}
 fun main() {
     val goTerm = "def: \"Catalysis of the reaction: all-trans-decaprenyl diphosphate + 4-hydroxybenzoate = 3-decaprenyl-4-hydroxybenzoate + diphosphate.\" [MetaCyc:RXN-9230]"
     println(resolveQuotedString(goTerm))
     val pmidLine1 = "def: \"The fusion of the plasma membrane of the sperm with the outer acrosomal membrane.\" [GOC:dph, PMID:3886029, PMID:1234567]"
     val pmidLine2 = "def: \"The acrosomal membrane region that underlies the acrosomal vesicle and is located toward the sperm nucleus. This region is responsible for molecular interactions allowing the sperm to penetrate the zona pellucida and fuses with the egg plasma membrane.\" [GOC:dph, PMID:3899643, PMID:8936405]"
     val pmidLine3 = "intersection_of: GO:0061025 ! membrane fusion"
     val pmidLines = listOf<String>(pmidLine1, pmidLine3, pmidLine2)
     resolvePubMedIdentifiers(pmidLines).stream().forEach { pmid -> println("PMID: $pmid") }
 }