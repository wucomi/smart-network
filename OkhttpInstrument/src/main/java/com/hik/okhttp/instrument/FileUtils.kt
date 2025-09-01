package com.hik.okhttp.instrument

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 复制文件
 */
fun copyFile(source: File, target: File) {
    if (!target.parentFile.exists()) {
        target.parentFile.mkdirs()
    }
    Files.copy(
        source.toPath(),
        target.toPath(),
        StandardCopyOption.REPLACE_EXISTING
    )
}

/**
 * 删除目录
 */
fun deleteDirectory(dir: File) {
    if (dir.exists()) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                deleteDirectory(file)
            } else {
                file.delete()
            }
        }
        dir.delete()
    }
}