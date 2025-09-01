package com.example.okhttp.instrument

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 自定义Transform，用于处理OkHttp字节码插装
 * 支持增量构建，使用Java原生API进行文件操作
 */
class OkHttpTransform : Transform() {
    // Transform名称，用于标识
    override fun getName(): String = "OkHttpTransform"

    // 输入类型：处理CLASS文件
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS

    // 作用范围：整个项目
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT

    // 支持增量构建
    override fun isIncremental(): Boolean = true

    /**
     * 执行字节码转换逻辑
     */
    override fun transform(invocation: TransformInvocation) {
        val outputProvider = invocation.outputProvider ?: return
        val isIncremental = invocation.isIncremental

        // 非增量模式下先清空输出目录
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        // 处理所有输入
        invocation.inputs.forEach { input ->
            // 处理目录类型输入（.class文件目录）
            input.directoryInputs.forEach { dirInput ->
                val inputDir = dirInput.file
                val outputDir = outputProvider.getContentLocation(
                    dirInput.name,
                    dirInput.contentTypes,
                    dirInput.scopes,
                    Format.DIRECTORY
                )

                if (isIncremental) {
                    // 增量模式：根据文件状态处理
                    when (dirInput.status) {
                        Status.ADDED, Status.MODIFIED -> processDirectory(inputDir, outputDir)
                        Status.REMOVED -> deleteDirectory(outputDir)
                        else -> {}
                    }
                } else {
                    // 非增量模式：全量处理
                    processDirectory(inputDir, outputDir)
                }
            }

            // 处理Jar类型输入（库文件）
            input.jarInputs.forEach { jarInput ->
                val outputJar = outputProvider.getContentLocation(
                    jarInput.name,
                    jarInput.contentTypes,
                    jarInput.scopes,
                    Format.JAR
                )

                if (isIncremental) {
                    // 增量模式：根据Jar状态处理
                    when (jarInput.status) {
                        Status.ADDED, Status.MODIFIED -> copyFile(jarInput.file, outputJar)
                        Status.REMOVED -> if (outputJar.exists()) outputJar.delete()
                        else -> {}
                    }
                } else {
                    // 非增量模式：直接复制
                    copyFile(jarInput.file, outputJar)
                }
            }
        }
    }

    /**
     * 处理目录中的class文件，递归遍历并修改字节码
     */
    private fun processDirectory(inputDir: File, outputDir: File) {
        if (!outputDir.exists()) {
            outputDir.mkdirs() // 确保输出目录存在
        }

        // 遍历目录下所有文件
        inputDir.walkTopDown().forEach { inputFile ->
            val relativePath = inputDir.toPath().relativize(inputFile.toPath())
            val outputFile = File(outputDir, relativePath.toString())

            if (inputFile.isDirectory) {
                // 创建对应的输出目录
                if (!outputFile.exists()) outputFile.mkdirs()
            } else if (inputFile.name.endsWith(".class")) {
                // 处理class文件（修改字节码）
                modifyClassFile(inputFile, outputFile)
            } else {
                // 复制非class文件
                copyFile(inputFile, outputFile)
            }
        }
    }

    /**
     * 修改单个class文件的字节码
     */
    private fun modifyClassFile(inputFile: File, outputFile: File) {
        try {
            // 读取原始字节码
            FileInputStream(inputFile).use { fis ->
                val cr = ClassReader(fis)
                val cw = ClassWriter(cr, ClassWriter.COMPUTE_FRAMES)
                
                // 应用字节码修改逻辑
                cr.accept(OkHttpClassVisitor(cw), ClassReader.EXPAND_FRAMES)

                // 输出修改后的字节码
                outputFile.parentFile.mkdirs()
                FileOutputStream(outputFile).use { fos ->
                    fos.write(cw.toByteArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 出错时复制原文件，保证构建不失败
            copyFile(inputFile, outputFile)
        }
    }

    /**
     * 复制文件（使用Java原生API）
     */
    private fun copyFile(source: File, target: File) {
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
     * 删除目录（递归删除所有内容）
     */
    private fun deleteDirectory(dir: File) {
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
}
    