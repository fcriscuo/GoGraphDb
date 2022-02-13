package org.batterypakdev.gographdb.go.io

data class Termlines(
    private val lines:MutableList<String> = mutableListOf()
) {
    fun getTermlinesContent():List<String> = lines.toList()

    fun addTermlines(line:String) = lines.add(line)

    fun hasContent():Boolean = lines.isNotEmpty()

}