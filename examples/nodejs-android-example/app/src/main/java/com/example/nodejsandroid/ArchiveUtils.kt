package com.example.nodejsandroid

import android.content.Context
import android.system.Os
import org.tukaani.xz.XZInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream

fun unTarXz(
    context: Context,
    assetName: String,
    toPath: String,
    prefix: String? = null
) {
    context.assets.open(assetName).use { fileStream ->
        XZInputStream(fileStream).use { xzIn ->
            TarArchiveInputStream(xzIn).use { tar ->
                var entry = tar.nextTarEntry

                while (entry != null) {
                    var name = entry.name

                    if (name.startsWith("./")) {
                        name = name.removePrefix("./")
                    }

                    val relativePath = if (!prefix.isNullOrEmpty()) {
                        if (!name.startsWith(prefix)) {
                            entry = tar.nextTarEntry
                            continue
                        }
                        name.removePrefix(prefix)
                    } else {
                        name
                    }

                    val destFile = File(toPath, relativePath)

                    when {
                        entry.isDirectory -> {
                            destFile.mkdirs()
                        }

                        entry.isSymbolicLink -> {
                            destFile.parentFile?.mkdirs()
                            try {
                                val targetName = entry.linkName
                                if (targetName.isNotEmpty()) {
                                    if (destFile.exists()) destFile.delete()

                                    val targetPath = File(destFile.parentFile, targetName).path

                                    Os.symlink(targetPath, destFile.absolutePath)
                                }
                            } catch (e: Exception) {
                                println("Failed symlink: ${entry.name} -> ${entry.linkName}: ${e.message}")
                            }
                        }

                        else -> {
                            destFile.parentFile?.mkdirs()

                            FileOutputStream(destFile).use { out ->
                                tar.copyTo(out)
                            }

                            if ((entry.mode and 0x40) != 0) {
                                destFile.setExecutable(true)
                            }
                        }
                    }

                    entry = tar.nextTarEntry
                }
            }
        }
    }
}