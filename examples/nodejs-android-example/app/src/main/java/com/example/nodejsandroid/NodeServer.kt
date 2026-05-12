package com.example.nodejsandroid

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

class NodeServer(private val context: Context) {

    fun start() {
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            Assets.ensureRuntime(context)
            Assets.ensureProject(context)

            startNode()
        }
    }

    private fun startNode() {
        val filesDir = context.filesDir.absolutePath
        val homeDir = File(context.filesDir, "home")
        val projectDir = File(homeDir, "nodejs-project")
        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
        val process = ProcessBuilder(
            "$nativeLibraryDir/libnode.so",
            "main.js"
        ).apply {
            directory(projectDir)

            val env = environment()
            env["HOME"] = homeDir.absolutePath
            env["PREFIX"] = "$filesDir/usr"
            env["PATH"] = "$filesDir/usr/bin"
            env["LD_LIBRARY_PATH"] = "$filesDir/usr/lib:$nativeLibraryDir"
            env["NODE_PATH"] = "$filesDir/usr/lib/node_modules"
            env["TMPDIR"] = context.cacheDir.absolutePath
        }.start()

        process.inputStream.bufferedReader().forEachLine {
            Log.i("NODE", it)
        }
    }
}