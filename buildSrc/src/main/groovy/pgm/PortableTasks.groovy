// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package pgm

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations

abstract class BuildPgmExtractor extends DefaultTask {
    @Inject abstract ExecOperations getExecOperations()
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE) abstract DirectoryProperty getSource()
    @Input @Optional abstract Property<String> getLlvmDirectory()
    @OutputDirectory abstract DirectoryProperty getOutput()

    @TaskAction
    void build() {
        def prefix = LlvmInstallation.require(llvmDirectory)
        def buildDirectory = output.get().asFile
        def bin = new File(buildDirectory, 'bin')
        execOperations.exec {
            commandLine 'cmake', '-S', source.get().asFile, '-B', buildDirectory,
                    "-DLLVM_DIR=${new File(prefix, 'lib/cmake/llvm')}",
                    "-DClang_DIR=${new File(prefix, 'lib/cmake/clang')}",
                    '-DCMAKE_BUILD_TYPE=Release', "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=${bin}",
                    "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE=${bin}"
        }
        execOperations.exec {
            commandLine 'cmake', '--build', buildDirectory, '--config', 'Release'
        }
    }
}

abstract class ExtractPgmDeclarations extends DefaultTask {
    @Inject abstract ExecOperations getExecOperations()
    @InputFile @PathSensitive(PathSensitivity.NONE) abstract RegularFileProperty getExtractor()
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE) abstract DirectoryProperty getHeaderDirectory()
    @Input @Optional abstract Property<String> getSdkDirectory()
    @Input @Optional abstract Property<String> getLlvmDirectory()
    @Input abstract Property<Boolean> getWindows()
    @Input abstract Property<String> getPlatform()
    @OutputFile abstract RegularFileProperty getInventory()

    @TaskAction
    void inspect() {
        def prefix = LlvmInstallation.require(llvmDirectory)
        def driver = new File(prefix, windows.get() ? 'bin/clang.exe' : 'bin/clang')
        if (!driver.isFile()) {
            throw new GradleException("Clang driver is missing: ${driver}")
        }
        def arguments = [extractor.get().asFile, headerDirectory.get().asFile, driver]
        if (sdkDirectory.isPresent()) {
            arguments.addAll(['-isysroot', sdkDirectory.get()])
        }
        def result = new ByteArrayOutputStream()
        execOperations.exec {
            if (windows.get()) {
                environment 'PATH', "${driver.parent}${File.pathSeparator}${System.getenv('PATH')}"
            }
            commandLine arguments
            standardOutput = result
        }
        def destination = inventory.get().asFile
        destination.parentFile.mkdirs()
        destination.bytes = result.toByteArray()
    }
}

abstract class EmitPgmBindings extends DefaultTask {
    @Inject abstract FileSystemOperations getFileSystemOperations()
    @InputFile @PathSensitive(PathSensitivity.NONE) abstract RegularFileProperty getInventory()
    @OutputDirectory abstract DirectoryProperty getOutput()

    @TaskAction
    void emit() {
        fileSystemOperations.delete { delete(output) }
        BindingEmitter.emit(inventory.get().asFile, output.get().asFile)
    }
}

final class LlvmInstallation {
    static File require(Property<String> path) {
        if (!path.isPresent()) {
            throw new GradleException('LLVM/Clang 23.1.4 development tools are required; set pgmLlvmDir or PGM_LLVM_DIR to their installation directory.')
        }
        def prefix = new File(path.get())
        if (!prefix.isAbsolute() || !prefix.isDirectory()) {
            throw new GradleException("Expected an absolute LLVM installation directory, got: ${prefix}")
        }
        return prefix
    }
}
