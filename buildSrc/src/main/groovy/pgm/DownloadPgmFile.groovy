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

abstract class DownloadPgmFile extends DefaultTask {
    @Input
    abstract Property<String> getDownloadUrl()

    @OutputFile
    abstract RegularFileProperty getDestination()

    @TaskAction
    void download() {
        def target = destination.get().asFile
        target.parentFile.mkdirs()
        URI.create(downloadUrl.get()).toURL().withInputStream { input ->
            target.withOutputStream { output ->
                output << input
            }
        }
    }
}
