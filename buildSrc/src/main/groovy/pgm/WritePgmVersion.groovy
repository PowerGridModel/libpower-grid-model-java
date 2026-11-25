// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
//
// SPDX-License-Identifier: MPL-2.0

package pgm

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class WritePgmVersion extends DefaultTask {
    @Input
    abstract Property<String> getPgmVersion()

    @OutputFile
    abstract RegularFileProperty getVersionFile()

    @TaskAction
    void writeVersion() {
        versionFile.get().asFile.write(pgmVersion.get())
    }
}
