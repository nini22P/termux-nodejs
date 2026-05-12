package com.example.nodejsandroid

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream

object Assets {

    fun ensureRuntime(context: Context) {
        val abi = android.os.Build.SUPPORTED_ABIS.first()

        val bundleName = when (abi) {
            "arm64-v8a" -> "nodejs-aarch64.tar.xz"
            "armeabi-v7a" -> "nodejs-arm.tar.xz"
            "x86_64" -> "nodejs-x86_64.tar.xz"
            "x86" -> "nodejs-i686.tar.xz"
            else -> error("Unsupported ABI")
        }

        if (shouldExtract(context, "runtime_$abi")) {
            Log.i("ASSETS", "Extracting runtime: $bundleName")
            unTarXz(
                context,
                bundleName,
                context.filesDir.absolutePath,
                prefix = "data/data/com.termux/files/"
            )
            markExtracted(context, "runtime_$abi")
        }
    }

    fun ensureProject(context: Context) {
        if (!shouldExtract(context, "project")) return

        val homeDir = File(context.filesDir.absolutePath, "home")
        val projectDir = File(homeDir, "nodejs-project")

        projectDir.deleteRecursively()
        projectDir.mkdirs()

        copyAssetDir(context, "nodejs-project", projectDir)
        markExtracted(context, "project")
    }

    private fun copyAssetDir(context: Context, assetPath: String, destDir: File) {
        context.assets.list(assetPath)?.forEach { fileName ->
            val assetFullPath = "$assetPath/$fileName"
            val destFile = File(destDir, fileName)

            val subFiles = context.assets.list(assetFullPath)
            if (!subFiles.isNullOrEmpty()) {
                destFile.mkdirs()
                copyAssetDir(context, assetFullPath, destFile)
            } else {
                destFile.parentFile?.mkdirs()
                context.assets.open(assetFullPath).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun shouldExtract(context: Context, key: String): Boolean {
        val prefs = context.getSharedPreferences("ASSETS_PREFS", Context.MODE_PRIVATE)
        val last = prefs.getLong(key, 0L)
        val current = getApkUpdateTime(context)
        return last != current
    }

    private fun markExtracted(context: Context, key: String) {
        val prefs = context.getSharedPreferences("ASSETS_PREFS", Context.MODE_PRIVATE)
        prefs.edit { putLong(key, getApkUpdateTime(context)) }
    }

    private fun getApkUpdateTime(context: Context): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.lastUpdateTime
        } catch (e: PackageManager.NameNotFoundException) {
            1L
        }
    }
}