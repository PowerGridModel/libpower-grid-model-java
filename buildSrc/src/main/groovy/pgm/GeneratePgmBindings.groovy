// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
//
// SPDX-License-Identifier: MPL-2.0

package pgm

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

abstract class GeneratePgmBindings extends DefaultTask {
    @Inject
    abstract ExecOperations getExecOperations()

    @InputFiles
    abstract ConfigurableFileCollection getJextractExecutables()

    @InputFile
    abstract RegularFileProperty getPgmHeader()

    @InputFile
    abstract RegularFileProperty getDatasetDefinitionsHeader()

    @OutputDirectory
    abstract DirectoryProperty getGeneratedJavaDirectory()

    @TaskAction
    void generate() {
        def jextractBins = jextractExecutables.files
        if (jextractBins.size() != 1) {
            throw new GradleException(
                    "Expected exactly one Jextract executable, " +
                            "but found ${jextractBins.size()}: $jextractBins"
            )
        }
        execOperations.exec {
            commandLine jextractBins.first(),
                    "--output", generatedJavaDirectory.get().asFile,
                    "--target-package", "org.lfenergy.pgm",
                    "--header-class-name", "PowerGridModelC",
                    pgmHeader.get().asFile,
                    datasetDefinitionsHeader.get().asFile
        }
    }
}
