package com.hik.okhttp.instrument

import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

/**
 * 自定义ClassVisitor，用于遍历类结构并处理其中的方法
 * 继承自ASM的ClassVisitor，使用访问者模式解析类信息
 */
class OkHttpClassVisitor(classVisitor: ClassVisitor) : ClassVisitor(Opcodes.ASM9, classVisitor) {

    /**
     * 当访问到类中的方法时调用
     * @param access 方法访问标志（如public、private、static等）
     * @param name 方法名
     * @param descriptor 方法描述符（包含参数类型和返回值类型）
     * @param signature 方法签名（泛型信息）
     * @param exceptions 方法抛出的异常列表
     * @return 用于处理当前方法的MethodVisitor
     */
    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        // 获取父类的MethodVisitor
        val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        // 返回自定义的MethodVisitor处理当前方法
        return OkHttpMethodVisitor(methodVisitor)
    }
}
    