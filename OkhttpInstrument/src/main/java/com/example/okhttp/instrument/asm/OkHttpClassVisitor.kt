package com.example.okhttp.instrument.asm

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * ASM ClassVisitor实现
 * 负责遍历类中的方法，为每个方法创建MethodVisitor处理字节码
 */
class OkHttpClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor) {

    /**
     * 访问类中的方法时调用
     * @return 自定义的MethodVisitor用于修改方法字节码
     */
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        // 获取父类的MethodVisitor
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        // 返回自定义的MethodVisitor处理字节码
        return OkHttpMethodVisitor(mv)
    }
}
    