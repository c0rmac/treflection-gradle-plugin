package ie.trinitcore.trinreflection.gradleplugin

import ie.trinitcore.trinreflection.api.*
import java.io.File

/** Internal (Trinitcore) reflection utilities. */
class TReflectionUtilities(
        val directory: String
) {

    private class Head(var position: Int, var scopeDepth: Int) {
        fun nextPosition() = position++
        fun previousPosition(): Int {
            position -= 1
            return position
        }
        fun registerChar(char: Char) {
            if (char == '{') scopeDepth++
            else if (char == '}') scopeDepth -= 1
        }
    }

    private class KotlinCombo(val functions: Array<TFunction>, val classes: Array<TClass>)

    companion object {
        private const val KEYWORD_FUNC = "fun"
        private const val KEYWORD_CLASS_INTERFACE = "interface"
        private const val KEYWORD_CLASS_STANDARD = "class"
        private const val KEYWORD_IMPORT = "import"
    }

    val classes: List<TClass>
    val functions: List<TFunction>

    init {
        val classes: MutableList<TClass> = mutableListOf()
        val functions: MutableList<TFunction> = mutableListOf()

        fun iterateSubDirs(files: Array<File>) {
            for (file in files) {
                if (file.isFile) {
                    if (file.extension == "kt") {
                        val code = file.readText()
                        val pkg = getQualifiedPackage(code)

                        if (pkg != "" && pkg != CodeGeneratingSerializer.DEFAULT_PACKAGE) {
                            val combo = parseKotlin(pkg, code, Head(0, 0))
                            classes.addAll(combo.classes)
                            functions.addAll(combo.functions)
                        }
                    }
                } else if (file.isDirectory) {
                    iterateSubDirs(file.listFiles())
                }
            }
        }

        val targetDir = File(directory)
        val files = targetDir.listFiles()
        iterateSubDirs(files)

        this.classes = classes
        this.functions = functions
    }

    fun useForPreCompilation() {
        val context = TReflectionContext(classes.toTypedArray(), functions.toTypedArray())

        val serializer = CodeGeneratingSerializer(arrayOf(directory))
        serializer.persist(context)
    }

    private fun getQualifiedPackage(code: String): String {
        var qualifiedPackage = ""
        var keyword = ""

        var searchingForKeyword = true

        var i = 0
        while (true) {
            val char = code.getOrNull(i++) ?: break

            if (searchingForKeyword) {
                if (char != ' ') keyword += char
                else searchingForKeyword = false
            } else {
                if (keyword == "package") {
                    if (char == ';' || char == '\n') break
                    else qualifiedPackage += char
                } else break
            }
        }
        return qualifiedPackage
    }

    private fun parseKotlin(
        qualifiedPackage: String,
        code: String,
        head: Head,
        /** Class name mapped to Qualified name */
            imports: HashMap<String, String> = hashMapOf()
    ): KotlinCombo {
        val functions = mutableListOf<TFunction>()
        val classes = mutableListOf<TClass>()

        var keyword = ""

        while (true) {
            val char = code.getOrNull(head.nextPosition()) ?: break
            head.registerChar(char)

            if (char == ' ' || char == '\n') {
                when (keyword) {
                    KEYWORD_CLASS_INTERFACE -> classes.add(parseClass(qualifiedPackage, code, head, TClassType.INTERFACE, imports))
                    KEYWORD_CLASS_STANDARD -> classes.add(parseClass(qualifiedPackage, code, head, TClassType.STANDARD, imports))
                    KEYWORD_FUNC -> functions.add(parseFunction(qualifiedPackage, code, head, imports))
                    KEYWORD_IMPORT -> parseImport(code, head).let { imports[it.split(".").last()] = it }
                }
                keyword = ""
            } else keyword += char
        }

        return KotlinCombo(functions.toTypedArray(), classes.toTypedArray())
    }

    private fun parseImport(code: String, head: Head): String {
        var qualifiedName = ""

        while (true) {
            val char = code.getOrNull(head.nextPosition()) ?: break
            head.registerChar(char)

            if (char == '\n' || char == ';') break
            else qualifiedName += char
        }

        return qualifiedName
    }

    private fun parseClass(qualifiedPackage: String, code: String, head: Head, type: TClassType, imports: HashMap<String, String>): TClass {
        var functions: Array<TFunction> = emptyArray()
        var classes: Array<TClass> = emptyArray()

        var name = ""
        var searchingForName = true

        while (true) {
            val char = code.getOrNull(head.nextPosition()) ?: break
            head.registerChar(char)
            if (searchingForName) {
                if (
                        char == '{' ||
                        /* Excludes the type parameters */ char == '<'
                        || char == '('
                        || char == ':'
                        || char == ' '
                        || char == ')'
                        || char == '\n'
                ) {
                    searchingForName = false
                    continue
                } else if (char != ' ') {
                    name += char
                }
            } else {
                val combo = parseKotlin(qualifiedPackage, code, head, imports)
                functions = combo.functions
                classes = combo.classes
            }
        }

        return TClass(qualifiedPackage, name, functions, classes, type)
    }

    private fun parseFunction(qualifiedPackage: String, code: String, head: Head, imports: HashMap<String, String>): TFunction {
        var parsingParams = false
        var searchingForReturnTypeDelimiter = false
        var parsingReturnType = false
        var parsingReturnTypeParams = false

        var name = ""
        var returnTypeQualifiedName = ""
        var typeParam = ""
        val typeParams = mutableListOf<String>()

        val params = mutableListOf<TParam>()

        while (true) {
            val char = code.getOrNull(head.nextPosition()) ?: break
            head.registerChar(char)
            if (char == '(' && !parsingParams) {
                parsingParams = true
            } else if (char == ')') {
                parsingParams = false
                searchingForReturnTypeDelimiter = true
            }
            if (parsingParams) {
                params.add(parseParam("$qualifiedPackage.$name", code, head))
            } else if (searchingForReturnTypeDelimiter) {
                if (char == ':') {
                    searchingForReturnTypeDelimiter = false
                    parsingReturnType = true
                } else if (char == '{' || char == '=') break
            } else if (parsingReturnType) {
                if (char == '\n' || char == '{' || char == '=') break
                else if (char == ' ') continue
                else if (char == '<') parsingReturnTypeParams = true
                else if (char == ',' || char == '>') {
                    typeParams.add(imports[typeParam] ?: typeParam)
                    typeParam = ""
                    if (char == '>') break
                } else if (parsingReturnTypeParams) typeParam += char
                else returnTypeQualifiedName += char
            } else {
                name += char
            }
        }

        return TFunction(
                qualifiedPackage,
                name,
                params.toTypedArray(),
                TReturnType(
                        imports[returnTypeQualifiedName] ?: returnTypeQualifiedName,
                        typeParams.toTypedArray()
                )
        )
    }

    private fun parseParam(qualifiedPackage: String, code: String, head: Head): TParam {
        var searchingForType = false
        var paramType = ""
        var paramName = ""

        while (true) {
            val char = code.getOrNull(head.nextPosition()) ?: break
            head.registerChar(char)
            if (char == ')' || char == ',') {
                head.previousPosition()
                break
            }
            else if (char != ' ' && char != '\n') {
                if (char == ':') {
                    searchingForType = true
                } else {
                    if (searchingForType) paramType += char
                    else paramName += char
                }
            }
        }

        return TParam(qualifiedPackage, paramName, paramType)
    }

}