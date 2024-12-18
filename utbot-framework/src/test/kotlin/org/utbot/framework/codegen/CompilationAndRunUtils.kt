package org.utbot.framework.codegen

import org.utbot.framework.plugin.api.CodegenLanguage
import java.io.File
import java.nio.file.Path
import mu.KotlinLogging
import org.apache.commons.io.FileUtils

data class ClassUnderTest(
    val testClassSimpleName: String,
    val packageName: String,
    val generatedTestFile: File
)

private val logger = KotlinLogging.logger {}

fun writeTest(
    testContents: String,
    testClassName: String,
    buildDirectory: Path,
    generatedLanguage: CodegenLanguage
): File {
    val classUnderTest = ClassUnderTest(
        testClassName.substringAfterLast("."),
        testClassName.substringBeforeLast(".").replace("class ", ""),
        File(buildDirectory.toFile(), "${testClassName.substringAfterLast(".")}${generatedLanguage.extension}")
    )

    val targetDir = classUnderTest.generatedTestFile.parentFile
    targetDir.mkdirs()
    logger.info {
        "File size for ${classUnderTest.testClassSimpleName}: ${FileUtils.byteCountToDisplaySize(testContents.length.toLong())}"
    }
    classUnderTest.generatedTestFile.writeText(testContents)
    return classUnderTest.generatedTestFile
}

fun compileTests(
    buildDirectory: String,
    sourcesFiles: List<String>,
    generatedLanguage: CodegenLanguage
) {
    val classpath = System.getProperty("java.class.path")
    val command = generatedLanguage.getCompilationCommand(buildDirectory, classpath, sourcesFiles)

    logger.trace { "Command to compile [${sourcesFiles.joinToString(" ")}]: [${command.joinToString(" ")}]" }
    val exitCode = execCommandLine(command, "Tests compilation")
    logger.info { "Compilation exit code: $exitCode" }
}

fun runTests(
    buildDirectory: String,
    testsNames: List<String>,
    testFramework: TestFramework,
    generatedLanguage: CodegenLanguage
) {
    val classpath = System.getProperty("java.class.path") + File.pathSeparator + buildDirectory
    val executionInvoke = generatedLanguage.executorInvokeCommand

    val command = testFramework.getRunTestsCommand(executionInvoke, classpath, testsNames, buildDirectory)

    logger.trace { "Command to run test: [${command.joinToString(" ")}]" }

    // We use argument file to pass classpath, so we should just call execCommand.
    // Because of some reason, it is impossible to use the same approach with Junit4 and TestNg, therefore,
    // we work with classpath using environment variable.
    val exitCode = if (testFramework is Junit5) {
        execCommandLine(command, "Tests execution")
    } else {
        setClassPathAndExecCommandLine(command, "Tests execution", classpath)
    }

    logger.info { "Run for [${testsNames.joinToString(" ")}] completed with exit code: $exitCode" }
}

/**
 * Constructs process with [command].
 *
 * If the [classpath] is not null, sets it in environment variable. Use it to avoid CMD limitation
 * for the length of the command.
 */
private fun constructProcess(command: List<String>, classpath: String? = null): ProcessBuilder {
    val process = ProcessBuilder(command).redirectErrorStream(true)

    classpath?.let { process.environment()["CLASSPATH"] = classpath }

    return process
}

private fun generateReportByProcess(process: Process, executionName: String, command: List<String>): Int {
    val report = process.inputStream.reader().readText()

    logger.info { "$executionName report: [$report]" }

    val exitCode = process.waitFor()

    if (exitCode != 0) {
        logger.warn { "Exit code for process run: $exitCode" }
        logger.warn { "The command line led to the pipeline failure: [${command.joinToString(" ")}]" }
        throw RuntimeException("$executionName failed  with non-zero exit code = $exitCode")
    }

    return exitCode
}

private fun execCommandLine(command: List<String>, executionName: String): Int {
    val process = constructProcess(command).start()

    return generateReportByProcess(process, executionName, command)
}

@Suppress("SameParameterValue")
/**
 * Sets [classpath] into an environment variable and executes the given [command].
 * It is used for JUnit4 and TestNg run.
 */
private fun setClassPathAndExecCommandLine(command: List<String>, executionName: String, classpath: String): Int {
    val process = constructProcess(command, classpath).start()

    return generateReportByProcess(process, executionName, command)
}
