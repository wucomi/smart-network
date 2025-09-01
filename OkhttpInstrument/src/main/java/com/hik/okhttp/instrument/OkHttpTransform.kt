package com.hik.okhttp.instrument

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.io.FileUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * OkHttp字节码转换类，用于在编译期修改OkHttpClient.Builder的build()方法调用
 * 支持增量编译，仅处理变更的文件以提升构建性能
 */
class OkHttpTransform : Transform() {
    /**
     * 转换名称，用于标识当前Transform，在构建日志和任务列表中可见
     */
    override fun getName(): String = "OkHttpTransform"
    
    /**
     * 输入类型：指定当前Transform处理的文件类型为CLASS文件
     * 对应Android编译流程中经过javac/kotlin编译后的字节码文件
     */
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> =
        TransformManager.CONTENT_CLASS
    
    /**
     * 作用范围：指定当前Transform处理的内容范围为整个项目
     * 包括项目自身代码、本地库、第三方库等
     */
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> =
        TransformManager.SCOPE_FULL_PROJECT
    
    /**
     * 是否支持增量编译：返回true表示支持
     * 增量编译仅处理变更的文件，大幅提升二次构建速度
     */
    override fun isIncremental(): Boolean = true

    /**
     * 转换执行的核心方法，处理输入文件并生成输出文件
     * @param invocation 转换调用上下文，包含输入、输出、构建状态等信息
     */
    override fun transform(invocation: TransformInvocation) {
        // 获取输出提供者，用于创建输出文件/目录
        val outputProvider = invocation.outputProvider ?: return
        
        // 判断当前构建是否为增量模式
        val isIncrementalBuild = invocation.isIncremental
        
        // 非增量模式下，先清空所有输出目录，避免旧文件残留导致冲突
        if (!isIncrementalBuild) {
            outputProvider.deleteAll()
        }
        
        // 遍历所有输入内容（目录和Jar包）
        invocation.inputs.forEach { input ->
            // 处理目录类型的输入（主要是项目自身编译产生的class文件）
            input.directoryInputs.forEach { dirInput ->
                processDirectoryInput(dirInput, outputProvider, isIncrementalBuild)
            }
            
            // 处理Jar类型的输入（主要是依赖库的class文件）
            input.jarInputs.forEach { jarInput ->
                processJarInput(jarInput, outputProvider, isIncrementalBuild)
            }
        }
    }
    
    /**
     * 处理目录类型的输入
     * @param dirInput 目录输入，包含待处理的class文件目录
     * @param outputProvider 输出提供者，用于获取输出目录
     * @param isIncremental 是否为增量构建
     */
    private fun processDirectoryInput(
        dirInput: DirectoryInput,
        outputProvider: TransformOutputProvider,
        isIncremental: Boolean
    ) {
        // 获取输入目录和输出目录
        val inputDir = dirInput.file
        val outputDir = outputProvider.getContentLocation(
            dirInput.name,
            dirInput.contentTypes,
            dirInput.scopes,
            Format.DIRECTORY
        )
        
        if (isIncremental) {
            // 增量构建：根据文件状态处理
            when (dirInput.status) {
                Status.ADDED, Status.MODIFIED -> {
                    // 新增或修改的目录：处理目录下的所有文件
                    processDirectory(inputDir, outputDir)
                }
                Status.REMOVED -> {
                    // 移除的目录：删除对应的输出目录
                    if (outputDir.exists()) {
                        deleteDirectory(outputDir)
                    }
                }
                // 其他状态（如NOTCHANGED）不处理
                else -> {}
            }
        } else {
            // 非增量构建：全量处理目录下的所有文件
            processDirectory(inputDir, outputDir)
        }
    }
    
    /**
     * 处理Jar类型的输入
     * @param jarInput Jar输入，包含待处理的Jar文件
     * @param outputProvider 输出提供者，用于获取输出Jar路径
     * @param isIncremental 是否为增量构建
     */
    private fun processJarInput(
        jarInput: JarInput,
        outputProvider: TransformOutputProvider,
        isIncremental: Boolean
    ) {
        // 获取输出Jar文件
        val outputJar = outputProvider.getContentLocation(
            jarInput.name,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR
        )
        
        if (isIncremental) {
            // 增量构建：根据Jar文件状态处理
            when (jarInput.status) {
                Status.ADDED, Status.MODIFIED -> {
                    // 新增或修改的Jar：复制Jar文件（实际项目可能需要解压处理内部class）
                    copyFile(jarInput.file, outputJar)
                }
                Status.REMOVED -> {
                    // 移除的Jar：删除对应的输出Jar
                    if (outputJar.exists()) {
                        outputJar.delete()
                    }
                }
                // 其他状态不处理
                else -> {}
            }
        } else {
            // 非增量构建：直接复制Jar文件
            copyFile(jarInput.file, outputJar)
        }
    }
    
    /**
     * 处理目录中的所有class文件，递归遍历并修改符合条件的文件
     * @param inputDir 输入目录
     * @param outputDir 输出目录
     */
    private fun processDirectory(inputDir: File, outputDir: File) {
        // 确保输出目录存在
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        
        // 递归遍历输入目录下的所有文件
        inputDir.walkTopDown().forEach { inputFile ->
            // 计算输入文件相对于输入目录的相对路径，用于保持目录结构
            val relativePath = inputDir.toPath().relativize(inputFile.toPath()).toString()
            val outputFile = File(outputDir, relativePath)
            
            if (inputFile.isFile && inputFile.name.endsWith(".class")) {
                // 处理class文件：使用ASM修改字节码
                modifyClassFile(inputFile, outputFile)
            } else {
                // 处理非class文件或目录：直接复制保持结构
                if (inputFile.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    copyFile(inputFile, outputFile)
                }
            }
        }
    }
    
    /**
     * 修改单个class文件的字节码
     * @param inputFile 输入的class文件
     * @param outputFile 输出的修改后class文件
     */
    private fun modifyClassFile(inputFile: File, outputFile: File) {
        try {
            // 读取输入class文件的字节流
            FileInputStream(inputFile).use { fis ->
                // 创建ClassReader用于解析class文件
                val classReader = ClassReader(fis)
                // 创建ClassWriter用于生成修改后的字节码
                // COMPUTE_FRAMES：自动计算栈帧信息，简化字节码修改
                val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)
                
                // 使用自定义的ClassVisitor处理字节码
                classReader.accept(OkHttpClassVisitor(classWriter), ClassReader.EXPAND_FRAMES)
                
                // 确保输出文件的父目录存在
                outputFile.parentFile.mkdirs()
                // 写入修改后的字节码到输出文件
                FileOutputStream(outputFile).use { fos ->
                    fos.write(classWriter.toByteArray())
                }
            }
        } catch (e: Exception) {
            // 处理异常：打印日志并复制原文件，避免构建失败
            e.printStackTrace()
            copyFile(inputFile, outputFile)
        }
    }
}
    