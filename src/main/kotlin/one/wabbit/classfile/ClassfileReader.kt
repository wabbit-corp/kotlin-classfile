package one.wabbit.classfile

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.util.EnumSet

private fun DataInputStream.readU8(): UByte = readUnsignedByte().toUByte()
private fun DataInputStream.readU16(): UShort = readUnsignedShort().toUShort()
private fun DataInputStream.readU32(): UInt = readInt().toUInt()
private fun DataInputStream.readU64(): ULong = readLong().toULong()

enum class ClassAccess(val mask: UShort) {
    Public(0x0001u),
    Final(0x0010u),
    Super(0x0020u),
    Interface(0x0200u),
    Abstract(0x0400u),
    Synthetic(0x1000u),
    Annotation(0x2000u),
    Enum(0x4000u),
    Module(0x8000u);

    companion object {
        @JvmStatic fun fromMask(mask: UShort): EnumSet<ClassAccess> {
            val set = EnumSet.noneOf(ClassAccess::class.java)
            for (access in entries) {
                if ((mask and access.mask).toUInt() != 0u) {
                    set.add(access)
                }
            }
            return set
        }
    }
}

enum class FieldAccess(val mask: UShort) {
    Public(0x0001u),
    Private(0x0002u),
    Protected(0x0004u),
    Static(0x0008u),
    Final(0x0010u),
    Volatile(0x0040u),
    Transient(0x0080u),
    Synthetic(0x1000u),
    Enum(0x4000u);

    companion object {
        @JvmStatic fun fromMask(mask: UShort): EnumSet<FieldAccess> {
            val set = EnumSet.noneOf(FieldAccess::class.java)
            for (access in entries) {
                if ((mask and access.mask).toUInt() != 0u) {
                    set.add(access)
                }
            }
            return set
        }
    }
}

enum class MethodAccess(val mask: UShort) {
    Public(0x0001u),
    Private(0x0002u),
    Protected(0x0004u),
    Static(0x0008u),
    Final(0x0010u),
    Synchronized(0x0020u),
    Bridge(0x0040u),
    Varargs(0x0080u),
    Native(0x0100u),
    Abstract(0x0400u),
    Strict(0x0800u),
    Synthetic(0x1000u);

    companion object {
        @JvmStatic fun fromMask(mask: UShort): EnumSet<MethodAccess> {
            val set = EnumSet.noneOf(MethodAccess::class.java)
            for (access in entries) {
                if ((mask and access.mask).toUInt() != 0u) {
                    set.add(access)
                }
            }
            return set
        }
    }
}

enum class KnownJavaVersion(val major: UShort) : Comparable<KnownJavaVersion> {
    V1_1(45u),
    V1_2(46u),
    V1_3(47u),
    V1_4(48u),
    V1_5(49u),
    V1_6(50u),
    V1_7(51u),
    V1_8(52u),
    V1_9(53u),
    V1_10(54u),
    V1_11(55u),
    V1_12(56u),
    V1_13(57u),
    V1_14(58u),
    V1_15(59u),
    V1_16(60u),
    V1_17(61u),
    V1_18(62u),
    V1_19(63u),
    V1_20(64u),
    V1_21(65u);

    companion object {
        fun toVersion(major: UShort): KnownJavaVersion? {
            for (version in entries) {
                if (version.major == major) {
                    return version
                }
            }
            return null
        }
    }
}

data class Classfile(
    val minorVersion: UShort,
    val majorVersion: UShort,
    val constantPool: List<Constant>,
    val accessFlags: EnumSet<ClassAccess>,
    val thisClass: UShort,
    val superClass: UShort,
    val interfaces: List<UShort>,
    val fields: List<Field>,
    val methods: List<Method>,
    val attributes: List<Attribute>,
) {
    companion object {
        fun fromFile(file: File): Classfile {
            FileInputStream(file).use { stream ->
                val reader = DataInputStream(BufferedInputStream(stream))

                val magic = reader.readU32()
                if (magic != 0xCAFEBABEu) {
                    throw IllegalArgumentException("Invalid magic number: $magic")
                }

                val minorVersion = reader.readU16()
                val majorVersion = reader.readU16()

                //println("  majorVersion: $majorVersion -> ${KnownJavaVersion.toVersion(majorVersion)}")

                val constantPool = readConstantPool(reader)
                val accessFlags = ClassAccess.fromMask(reader.readU16())
                val thisClass = reader.readU16()
                val superClass = reader.readU16()
                val interfaces = (0 ..< reader.readU16().toInt()).map { reader.readU16() }
                val fields     = (0 ..< reader.readU16().toInt()).map {
                    val field = Field(
                        accessFlags = FieldAccess.fromMask(reader.readU16()),
                        nameIndex = reader.readU16(),
                        descriptorIndex = reader.readU16(),
                        attributes = (0 ..< reader.readU16().toInt()).map {
                            readAttribute(constantPool, reader)
                        }
                    )

                    val name = constantPool[field.nameIndex.toInt()]
                    val descriptor = constantPool[field.descriptorIndex.toInt()]
                    val attrs = field.attributes.map { constantPool[it.nameIndex.toInt()] }
                    //println("  field $name : $descriptor -> $attrs")

                    field
                }
                val methods    = (0 ..< reader.readU16().toInt()).map {
                    val method = Method(
                        accessFlags = MethodAccess.fromMask(reader.readU16()),
                        nameIndex = reader.readU16(),
                        descriptorIndex = reader.readU16(),
                        attributes = (0 ..< reader.readU16().toInt()).map {
                            readAttribute(constantPool, reader)
                        }
                    )

                    val name = constantPool[method.nameIndex.toInt()]
                    val descriptor = constantPool[method.descriptorIndex.toInt()]
                    val attrs = method.attributes.map { constantPool[it.nameIndex.toInt()] }
                    //println("  method $name : $descriptor -> $attrs")

                    method
                }
                val attributes = (0 ..< reader.readU16().toInt()).map {
                    readAttribute(constantPool, reader)
                }

                //println("attributes: ${attributes.map { constantPool[it.nameIndex.toInt()] }}")

                return Classfile(
                    minorVersion = minorVersion,
                    majorVersion = majorVersion,
                    constantPool = constantPool,
                    accessFlags = accessFlags,
                    thisClass = thisClass,
                    superClass = superClass,
                    interfaces = interfaces,
                    fields = fields,
                    methods = methods,
                    attributes = attributes,
                )
            }
        }

        fun readAttribute(constantPool: List<Constant>, reader: DataInputStream): Attribute {
            val nameIndex = reader.readU16()
            val size = reader.readU32().toInt()
            val name = (constantPool[nameIndex.toInt()] as Constant.Utf8).bytes.decodeToString()

            when (name) {
                "ConstantValue" -> {
                    val constantValueIndex = reader.readU16()
                    require(size == 2) { "Invalid size for ConstantValue attribute" }
                    return Attribute.ConstantValue(nameIndex, constantValueIndex)
                }

                "Code" -> {
                    val maxStack = reader.readU16()
                    val maxLocals = reader.readU16()
                    val code = ByteArray(reader.readU32().toInt())
                    reader.readFully(code)

                    val exceptionTable = (0 ..< reader.readU16().toInt()).map {
                        Exception(reader.readU16(), reader.readU16(), reader.readU16(), reader.readU16())
                    }

                    val attributes = (0 ..< reader.readU16().toInt()).map {
                        readAttribute(constantPool, reader)
                    }

                    return Attribute.Code(nameIndex, maxStack, maxLocals, code, exceptionTable, attributes)
                }

//                "StackMapTable" -> {
//                    val entries = (0 ..< reader.readU16().toInt()).map {
//                        val frameType = reader.readU8().toInt()
//
//                        if (frameType in 0 .. 63) {
//                            // SAME type
//                            return@map StackMapFrame(frameType.toUByte(), 0u, emptyList(), emptyList())
//                        } else if (frameType in 64 .. 127) {
//                            // SAME_LOCALS_1_STACK_ITEM
//                            val stack = listOf(readVerificationType(reader))
//                            return@map StackMapFrame(frameType.toUByte(), 0u, emptyList(), stack)
//                        } else if (frameType in 128 .. 246) {
//                            // RESERVED
//                            throw IllegalArgumentException("Invalid frame type: $frameType")
//                        } else if (frameType in 247 .. 250) {
//                            // SAME_LOCALS_1_STACK_ITEM_EXTENDED
//                            val offsetDelta = reader.readU16()
//                            val stack = listOf(readVerificationType(reader))
//                            return@map StackMapFrame(frameType.toUByte(), offsetDelta, emptyList(), stack)
//                        } else if (frameType in 251 .. 254) {
//                            // CHOP
//                            val offsetDelta = reader.readU16()
//                            return@map StackMapFrame(frameType.toUByte(), offsetDelta, emptyList(), emptyList())
//                        } else if (frameType == 255) {
//                            // APPEND
//                            val offsetDelta = reader.readU16()
//                            val locals = (0 ..< reader.readU16().toInt()).map { readVerificationType(reader) }
//                            return@map StackMapFrame(frameType.toUByte(), offsetDelta, locals, emptyList())
//                        } else {
//                            throw IllegalArgumentException("Invalid frame type: $frameType")
//                        }
//                    }
//                    return Attribute.StackMapTable(nameIndex, entries)
//                }

                "Exceptions" -> {
                    val exceptionIndexTable = (0 ..< reader.readU16().toInt()).map { reader.readU16() }
                    return Attribute.Exceptions(nameIndex, exceptionIndexTable)
                }

                "Synthetic" -> {
                    require(size == 0) { "Invalid size for Synthetic attribute" }
                    return Attribute.Synthetic(nameIndex)
                }

                "Signature" -> {
                    val signatureIndex = reader.readU16()
                    require(size == 2) { "Invalid size for Signature attribute" }
                    return Attribute.Signature(nameIndex, signatureIndex)
                }

                "SourceFile" -> {
                    val sourceFileIndex = reader.readU16()
                    require(size == 2) { "Invalid size for SourceFile attribute" }
                    return Attribute.SourceFile(nameIndex, sourceFileIndex)
                }

                else -> {
                    val info = ByteArray(size)
                    reader.readFully(info)
                    return Attribute.Unknown(nameIndex, info)
                }
            }

//            val info = ByteArray()
//            reader.readFully(info)
//            return Attribute(nameIndex, info)
        }

        fun readConstantPool(reader: DataInputStream): List<Constant> {
            val count = reader.readUnsignedShort()
            val pool = mutableListOf<Constant>()

            pool.add(Constant.Utf8(0u, byteArrayOf()))

            var i = 1
            while (i < count) {
                val tag = reader.readUnsignedByte().toUByte()
                val constant = when (tag.toUInt()) {
                    1u -> {
                        val length = reader.readUnsignedShort()
                        val bytes = ByteArray(length.toInt())
                        reader.readFully(bytes)
                        Constant.Utf8(tag, bytes)
                    }
                    3u -> Constant.Integer(tag, reader.readInt())
                    4u -> Constant.Float(tag, reader.readFloat())
                    5u -> Constant.Long(tag, reader.readLong())
                    6u -> Constant.Double(tag, reader.readDouble())
                    7u -> Constant.Class(tag, reader.readU16())
                    8u -> Constant.String(tag, reader.readU16())
                    9u -> Constant.FieldRef(tag, reader.readU16(), reader.readU16())
                    10u -> Constant.MethodRef(tag, reader.readU16(), reader.readU16())
                    11u -> Constant.InterfaceMethodRef(tag, reader.readU16(), reader.readU16())
                    12u -> Constant.NameAndType(tag, reader.readU16(), reader.readU16())
                    15u -> Constant.MethodHandle(tag, reader.readU8(), reader.readU16())
                    16u -> Constant.MethodType(tag, reader.readU16())
                    17u -> Constant.Dynamic(tag, reader.readU16(), reader.readU16())
                    18u -> Constant.InvokeDynamic(tag, reader.readU16(), reader.readU16())
                    19u -> Constant.Module(tag, reader.readU16())
                    20u -> Constant.Package(tag, reader.readU16())
                    else -> throw IllegalArgumentException("Invalid constant pool tag: $tag")
                }

                pool.add(constant)
                if (tag.toUInt() == 5u || tag.toUInt() == 6u) {
                    pool.add(Constant.Utf8(0u, byteArrayOf()))
                    i++
                }
                i++
            }
            return pool
        }
    }
}

sealed interface Constant {
    val tag: UByte

    data class Utf8(override val tag: UByte, val bytes: ByteArray) : Constant {
        override fun toString(): kotlin.String {
            val text = bytes.decodeToString()
            return text
        }
    }
    data class Integer(override val tag: UByte, val value: kotlin.Int) : Constant
    data class Float(override val tag: UByte, val value: kotlin.Float) : Constant
    data class Long(override val tag: UByte, val value: kotlin.Long) : Constant
    data class Double(override val tag: UByte, val value: kotlin.Double) : Constant
    data class Class(override val tag: UByte, val nameIndex: UShort) : Constant
    data class String(override val tag: UByte, val stringIndex: UShort) : Constant
    data class FieldRef(override val tag: UByte, val classIndex: UShort, val nameAndTypeIndex: UShort) : Constant
    data class MethodRef(override val tag: UByte, val classIndex: UShort, val nameAndTypeIndex: UShort) : Constant
    data class InterfaceMethodRef(override val tag: UByte, val classIndex: UShort, val nameAndTypeIndex: UShort) : Constant
    data class NameAndType(override val tag: UByte, val nameIndex: UShort, val descriptorIndex: UShort) : Constant
    data class MethodHandle(override val tag: UByte, val kind: UByte, val referenceIndex: UShort) : Constant
    data class MethodType(override val tag: UByte, val descriptorIndex: UShort) : Constant
    data class Dynamic(override val tag: UByte, val bootstrapMethodAttrIndex: UShort, val nameAndTypeIndex: UShort) : Constant
    data class InvokeDynamic(override val tag: UByte, val bootstrapMethodAttrIndex: UShort, val nameAndTypeIndex: UShort) : Constant
    data class Module(override val tag: UByte, val nameIndex: UShort) : Constant
    data class Package(override val tag: UByte, val nameIndex: UShort) : Constant
}

data class Field(
    val accessFlags: EnumSet<FieldAccess>,
    val nameIndex: UShort,
    val descriptorIndex: UShort,
    val attributes: List<Attribute>,
)

data class Method(
    val accessFlags: EnumSet<MethodAccess>,
    val nameIndex: UShort,
    val descriptorIndex: UShort,
    val attributes: List<Attribute>,
)

sealed interface Attribute {
    val nameIndex: UShort

    data class Unknown(
        override val nameIndex: UShort,
        val info: ByteArray) : Attribute

    data class ConstantValue(
        override val nameIndex: UShort,
        val constantValueIndex: UShort) : Attribute

    data class Code(
        override val nameIndex: UShort,
        val maxStack: UShort, val maxLocals: UShort,
        val code: ByteArray,
        val exceptionTable: List<Exception>,
        val attributes: List<Attribute>) : Attribute

    data class StackMapTable(
        override val nameIndex: UShort,
        val entries: List<StackMapFrame>) : Attribute

    data class Exceptions(
        override val nameIndex: UShort,
        val exceptionIndexTable: List<UShort>) : Attribute

    data class InnerClasses(
        override val nameIndex: UShort,
        val classes: List<InnerClass>) : Attribute

    data class EnclosingMethod(
        override val nameIndex: UShort,
        val classIndex: UShort,
        val methodIndex: UShort) : Attribute

    data class Synthetic(
        override val nameIndex: UShort) : Attribute

    data class Signature(
        override val nameIndex: UShort,
        val signatureIndex: UShort) : Attribute

    data class SourceFile(
        override val nameIndex: UShort,
        val sourceFileIndex: UShort) : Attribute
}

data class Exception(
    val startPc: UShort,
    val endPc: UShort,
    val handlerPc: UShort,
    val catchType: UShort,
)

data class StackMapFrame(
    val frameType: UByte,
    val offsetDelta: UShort,
    val locals: List<VerificationType>,
    val stack: List<VerificationType>,
)

sealed interface VerificationType {
    data object Top : VerificationType
    data object Integer : VerificationType
    data object Float : VerificationType
    data object Double : VerificationType
    data object Long : VerificationType
    data object Null : VerificationType
    data object UninitializedThis : VerificationType
    data class Object(val cpoolIndex: UShort) : VerificationType
    data class Uninitialized(val offset: UShort) : VerificationType
}

data class InnerClass(
    val innerClassInfoIndex: UShort,
    val outerClassInfoIndex: UShort,
    val innerNameIndex: UShort,
    val innerClassAccessFlags: EnumSet<ClassAccess>,
)
