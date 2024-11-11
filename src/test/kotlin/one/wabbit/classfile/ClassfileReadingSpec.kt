package one.wabbit.classfile

import java.io.File
import kotlin.test.Test

fun File.sha256(): String {
    val bytes = readBytes()
    val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}

class ClassfileReadingSpec {
    // Class attributes: [
    //      SourceFile, EnclosingMethod,
    //      InnerClasses, Signature,
    //      NestHost, RuntimeVisibleAnnotations,
    //      BootstrapMethods, SourceDebugExtension,
    //      RuntimeInvisibleAnnotations, Deprecated,
    //      NestMembers, PermittedSubclasses, Record,
    //      MissingTypes, Module,
    //      Scala, TASTY, ScalaInlineInfo, ScalaSig,
    //      InconsistentHierarchy, CompileVersion]

    // Field attributes: [
    //      ConstantValue,
    //      Deprecated, Synthetic,
    //      Signature,
    //      RuntimeInvisibleAnnotations, RuntimeVisibleAnnotations,
    //      RuntimeVisibleTypeAnnotations, RuntimeInvisibleTypeAnnotations]

    // Method attributes: [
    //      Deprecated, Synthetic,
    //      Signature,
    //      RuntimeInvisibleParameterAnnotations, RuntimeInvisibleAnnotations,
    //      RuntimeVisibleAnnotations, RuntimeInvisibleTypeAnnotations,
    //      RuntimeVisibleTypeAnnotations, RuntimeVisibleParameterAnnotations,
    //      Code, Exceptions, MethodParameters,
    //      AnnotationDefault]

    @Test fun test() {
        val classAttributes = mutableSetOf<String>()
        val methodAttributes = mutableSetOf<String>()
        val fieldAttributes = mutableSetOf<String>()

        val out = File("../test-classfiles/")
        out.mkdir()

        for (path in out.walkTopDown()) {
            if (path.extension != "class") continue

//            val hash = path.sha256()
//            File(out, hash.take(2)).mkdir()
//            if (File(out, hash.take(2)).resolve("$hash.class").exists()) continue
//            path.copyTo(File(out, hash.take(2)).resolve("$hash.class"))

            println(path)
            val classfile = Classfile.fromFile(path)

            fun Attribute.name(): String =
                (classfile.constantPool[this.nameIndex.toInt()] as Constant.Utf8).bytes.decodeToString()

            classAttributes.addAll(classfile.attributes.map { it.name() })
            methodAttributes.addAll(classfile.methods.flatMap { it.attributes }.map { it.name() })
            fieldAttributes.addAll(classfile.fields.flatMap { it.attributes }.map { it.name() })
        }

        println("Class attributes: $classAttributes")
        println("Method attributes: $methodAttributes")
        println("Field attributes: $fieldAttributes")
    }
}
