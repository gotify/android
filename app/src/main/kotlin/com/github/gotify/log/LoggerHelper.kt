package com.github.gotify.log

import android.content.Context
import java.io.File
import org.tinylog.kotlin.Logger

class LoggerHelper {
    companion object {
        fun read(ctx: Context): String = folder(ctx)
            .listFiles()
            .orEmpty()
            .flatMap { it.readLines() }
            .fold(mutableListOf<String>()) { newLines, line -> groupExceptions(newLines, line) }
            .takeLast(200)
            .reversed()
            .joinToString(separator = "\n")

        private fun groupExceptions(
            newLines: MutableList<String>,
            line: String
        ): MutableList<String> {
            if (newLines.isNotEmpty() && (line.startsWith('\t') || line.startsWith("Caused"))) {
                newLines[newLines.lastIndex] += '\n' + line
            } else {
                newLines.add(line)
            }
            return newLines
        }

        fun clear(ctx: Context) {
            folder(ctx).listFiles()?.forEach { it.writeText("") }
            Logger.info("Logs cleared")
        }

        fun init(ctx: Context) {
            val file = folder(ctx)
            file.mkdirs()
            System.setProperty("tinylog.directory", file.absolutePath)
        }

        private fun folder(ctx: Context): File = File(ctx.filesDir, "log")
    }
}
