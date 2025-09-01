package com.hik.okhttp.instrument

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * OkHttp字节插装插件入口类
 * 负责在Android项目中注册自定义的Transform
 */
class OkHttpInstrumentPlugin : Plugin<Project> {
    /**
     * 应用插件时执行的方法
     * @param project 当前应用插件的项目
     */
    override fun apply(project: Project) {
        // 获取Android扩展配置，确保插件只应用于Android项目
        val androidExtension = project.extensions.findByType(BaseExtension::class.java)
            ?: throw IllegalStateException("OkHttpInstrumentPlugin must be applied to an Android project")
        
        // 注册自定义的Transform，使其参与Android构建流程
        androidExtension.registerTransform(OkHttpTransform())
    }
}
    