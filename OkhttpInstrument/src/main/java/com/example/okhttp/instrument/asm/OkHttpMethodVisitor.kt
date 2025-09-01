package com.example.okhttp.instrument.asm

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * ASM MethodVisitor实现
 * 负责识别并替换OkHttpClient$Builder.build()方法调用
 */
class OkHttpMethodVisitor(methodVisitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, methodVisitor) {

    /**
     * 访问方法调用指令时触发
     * 在这里识别并替换目标方法调用
     */
    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        // 匹配目标方法：OkHttpClient$Builder.build()
        // 条件：调用指令为INVOKEVIRTUAL，类名、方法名、方法描述符匹配
        if (opcode == Opcodes.INVOKEVIRTUAL
            && owner == "okhttp3/OkHttpClient$Builder"
            && name == "build"
            && descriptor == "()Lokhttp3/OkHttpClient;"
        ) {
            // 替换逻辑：先调用smartNetwork()，再调用其build()
            
            // 1. 调用扩展函数：OkHttpClient.Builder.smartNetwork()
            // Kotlin扩展函数编译后为静态方法，位于SmartNetworkKt类
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "com/wcm/smart_network/okhttp/SmartNetworkKt", // 扩展函数所在类
                "smartNetwork", // 扩展函数名
                "(Lokhttp3/OkHttpClient$Builder;)Lcom/wcm/smart_network/okhttp/SmartNetworkBuilder;", // 方法描述符
                false
            )

            // 2. 调用SmartNetworkBuilder的build()方法
            super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "com/wcm/smart_network/okhttp/SmartNetworkBuilder", // 目标类
                "build", // 方法名
                "()Lokhttp3/OkHttpClient;", // 方法描述符
                false
            )
            return // 跳过原方法调用
        }

        // 非目标方法调用，直接执行原指令
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
}
    