package ie.trinitcore.trinreflection.gradleplugin

import ie.trinitcore.trinreflection.api.*
import java.io.File

interface Serializer {
    val buildDirs: Array<String>
    fun persist(tReflectionContext: TReflectionContext)
}

class CodeGeneratingSerializer(
    /** The target paths for which hold directory paths com.precompilation. A _Generated.kt file will be created in the
     * com/clickcostz/precompilation directory */
    override val buildDirs: Array<String>
) : Serializer {

    companion object {
        const val DEFAULT_PACKAGE = "ie.trinitcore.trinreflection.gen"
        const val DEFAULT_SUB_DIR = "ie/trinitcore/trinreflection/gen"
        const val DEFAULT_FILE_NAME = "_Generated.kt"
        const val COMP_PKG = "ie.trinitcore.trinreflection.api"
    }

    override fun persist(tReflectionContext: TReflectionContext) {
        var body = ""
        // TUS : Generate imports
        fun appendImport(qualifiedPackage: String, name: String) {
            body += "import ${qualifiedPackage}.${name}\n"
        }

        // tReflectionContext.classes.forEach { appendImport(it.qualifiedPackage, it.name) }
        // tReflectionContext.functions.forEach { appendImport(it.qualifiedPackage, it.name) }
        appendImport(COMP_PKG, "TClass")
        appendImport(COMP_PKG, "TFunction")
        appendImport(COMP_PKG, "TClassType")
        appendImport(COMP_PKG, "TParam")
        appendImport(COMP_PKG, "TReturnType")
        // DEIREADH : Generate imports

        body += "\n"

        // TUS : Define serializers
        fun <T>getSerializedArray(iterable: Array<T>, itemBlock: (item: T) -> String): String {
            var serialized = "arrayOf(\n"
            iterable.forEach { serialized += "${itemBlock(it)},\n" }
            serialized += ")"

            return serialized
        }

        fun getSerializedTReturnType(tReturnType: TReturnType): String {
            var serialized = "TReturnType(\n"
            serialized += "\"${tReturnType.qualifiedName}\",\n"
            serialized += getSerializedArray(tReturnType.typeParameters) {
                "\"$it\""
            }
            serialized += ")"

            return serialized
        }

        fun getSerializedTParam(tParam: TParam): String {
            var serialized = "TParam(\n"
            serialized += "\"${tParam.qualifiedPackage}\",\n"
            serialized += "\"${tParam.name}\",\n"
            serialized += "\"${tParam.type}\",\n"
            serialized += ")"
            
            return serialized
        }

        fun getSerializedTFunction(tFunction: TFunction): String {
            var serialized = "TFunction(\n"
            serialized += "\"${tFunction.qualifiedPackage}\",\n"
            serialized += "\"${tFunction.name}\",\n"
            serialized += getSerializedArray(tFunction.params) {
                getSerializedTParam(it)
            }
            serialized += ",\n"
            serialized += getSerializedTReturnType(tFunction.returnType)
            serialized += ")"

            return serialized
        }

        fun getSerializedTClass(tClass: TClass): String {
            var serialized = "TClass(\n"
            serialized += "\"${tClass.qualifiedPackage}\",\n"
            serialized += "\"${tClass.name}\",\n"
            serialized += getSerializedArray(tClass.functions) {
                getSerializedTFunction(it)
            }
            serialized += ",\n"
            serialized += getSerializedArray(tClass.classes) {
                getSerializedTClass(it)
            }
            serialized += ",\n"
            serialized += "TClassType."+tClass.type.name + "\n"
            serialized += ")"

            return serialized
        }
        // DEIREADH : Define serializers

        // TUS : Declare tClasses
        tReflectionContext.classes.forEachIndexed { index, tClass -> body += "val tClass${index} = ${getSerializedTClass(tClass)}\n" }
        body += "\n\n"
        // DEIREADH : Declare tClasses

        // TUS : Generate mapping
        body += "internal val kClassesToTClasses = mapOf(\n"
        tReflectionContext.classes.forEachIndexed { index, tClass -> body += "${tClass.qualifiedPackage}.${tClass.name}::class to tClass${index},\n" }
        body += ")\n\n"

        body += "internal val qualifiedPackagesToKClasses = mapOf(\n"
        tReflectionContext.classes.forEachIndexed { index, tClass -> body += "\"${tClass.qualifiedPackage}.${tClass.name}\" to ${tClass.qualifiedPackage}.${tClass.name}::class,\n" }
        body += ")"
        // DEIREADH : Generate mapping

        for (buildDir in buildDirs) {
            val document = "package $DEFAULT_PACKAGE\n\n${body}"
            val filePath = "$buildDir/$DEFAULT_SUB_DIR/$DEFAULT_FILE_NAME"
            val file = File(filePath)
            file.delete()
            file.createNewFile()
            file.writeText(document)
        }
    }

}