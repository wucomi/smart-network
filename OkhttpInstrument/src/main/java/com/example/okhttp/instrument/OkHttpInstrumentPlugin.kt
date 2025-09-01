package com.example.okhttp.instrument

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * OkHttp字节插装插件入口
 * 负责将自定义Transform注册到Android构建流程
 */
class OkHttpInstrumentPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 验证是否为Android项目
        val androidExtension = project.extensions.findByType(BaseExtension::class.java)
            ?: throw IllegalStateException("该插件仅支持Android项目，请在app模块中应用")

        // 注册Transform处理字节码
        androidExtension.registerTransform(OkHttpTransform())
        
        project.logger.lifecycle("✅ OkHttp Instrument插件已成功应用")
    }
}
    