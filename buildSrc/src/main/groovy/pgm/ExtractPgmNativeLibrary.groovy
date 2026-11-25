// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
//
// SPDX-License-Identifier: MPL-2.0

package pgm

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class ExtractPgmNativeLibrary extends DefaultTask {
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getWheel()

    @Input
    abstract Property<String> getEntryPath()

    @OutputFile
    abstract RegularFileProperty getDestination()

    @TaskAction
    void extract() {
        def wheelFile = wheel.get().asFile
        def target = destination.get().asFile.toPath()

        new ZipFile(wheelFile).withCloseable { archive ->
            def entry = archive.getEntry(entryPath.get())
            if (entry == null || entry.isDirectory()) {
                throw new GradleException("Expected native library '${entryPath.get()}' in ${wheelFile}")
            }
            Files.createDirectories(target.parent)
            archive.getInputStream(entry).withCloseable { input ->
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
