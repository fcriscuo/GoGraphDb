package org.batterypakdev.gographdb.go.io

import com.google.common.flogger.FluentLogger
import java.io.File
import java.util.*
import java.util.function.Supplier

class OboFileLineSupplier(filename: String) : Supplier<String>{
    private val logger: FluentLogger = FluentLogger.forEnclosingClass()
    private val oboScanner = Scanner(File(filename))

    override fun get(): String =
      when  (oboScanner.hasNextLine()) {
          true -> oboScanner.nextLine()
          false -> "EOF"
      }
}

fun main(args: Array<String>) {
    val filePathName = if (args.isNotEmpty()) args[0] else "./data/sample_go.obo"
    println("Processing OBO-formatted file: $filePathName")
    val supplier = OboFileLineSupplier(filePathName)
    for (i in 1 .. 200 ){
       println(supplier.get())
    }
}