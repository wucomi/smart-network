package com.hik.okhttp.instrument

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * 自定义MethodVisitor，用于修改方法中的字节码指令
 * 主要功能：将OkHttpClient.Builder.build()调用替换为.smartNetwork().build()
 */
class OkHttpMethodVisitor(methodVisitor: MethodVisitor) : MethodVisitor(Opcodes.ASM9, methodVisitor) {

    /**
     * 当访问到方法中的方法调用指令时调用
     * @param opcode 指令操作码（如INVOKEVIRTUAL、INVOKESTATIC等）
     * @param owner 方法所属类的全限定名（内部类用$分隔）
     * @param name 方法名
     * @param descriptor 方法描述符
     * @param isInterface 被调用方法是否来自接口
     */
    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        // 匹配目标方法：OkHttpClient$Builder.build()
        // 条件说明：
        // 1. opcode == Opcodes.INVOKEVIRTUAL：实例方法调用
        // 2. owner == "okhttp3/OkHttpClient$Builder"：方法所属类为OkHttpClient的内部类Builder
        // 3. name == "build"：方法名为build
        // 4. descriptor == "()Lokhttp3/OkHttpClient;"：方法无参数，返回OkHttpClient对象
        if (opcode == Opcodes.INVOKEVIRTUAL
            && owner == "okhttp3/OkHttpClient$Builder"
            && name == "build"
            && descriptor == "()Lokhttp3/OkHttpClient;"
        ) {
            // 替换为：先调用smartNetwork()扩展函数，再调用其build()方法
            
            // 第一步：调用OkHttpClient.Builder的smartNetwork()扩展函数
            // Kotlin扩展函数编译后为静态方法，位于SmartNetworkKt类
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,  // 静态方法调用
                "com/wcm/smart_network/okhttp/SmartNetworkKt",  // 扩展函数所在类
                "smartNetwork",  // 扩展函数名
                "(Lokhttp3/OkHttpClient$Builder;)Lcom/wcm/smart_network/okhttp/SmartNetworkBuilder;",  // 方法描述符
                false  // 非接口方法
            )
            
            // 第二步：调用SmartNetworkBuilder的build()方法
            super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,  // 实例方法调用
                "com/wcm/smart_network/okhttp/SmartNetworkBuilder",  // 方法所属类
                "build",  // 方法名
                "()Lokhttp3/OkHttpClient;",  // 方法描述符
                false  // 非接口方法
            )
            return  // 跳过原方法调用
        }
        
        // 非目标方法调用，直接执行原指令
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
}
    