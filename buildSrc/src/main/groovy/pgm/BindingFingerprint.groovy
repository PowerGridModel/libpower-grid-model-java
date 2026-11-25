// SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>
// SPDX-License-Identifier: MPL-2.0

package pgm

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.nio.file.Files
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

abstract class BindingFingerprint extends DefaultTask {
    @InputFile @PathSensitive(PathSensitivity.NONE) abstract RegularFileProperty getInventory()
    @InputDirectory @PathSensitive(PathSensitivity.RELATIVE) abstract DirectoryProperty getSourceDirectory()
    @InputFile @Optional @PathSensitive(PathSensitivity.NONE) abstract RegularFileProperty getReference()
    @Input abstract Property<String> getWheelVersion()
    @OutputFile abstract RegularFileProperty getDestination()

    @TaskAction
    void fingerprint() {
        def parser = new JsonSlurper()
        def inventoryData = parser.parse(inventory.get().asFile)
        def root = sourceDirectory.get().asFile.toPath()
        Map<String, String> sources = new TreeMap<>()
        Files.walk(root).withCloseable { paths ->
            paths.filter { Files.isRegularFile(it) }.forEach { path ->
                String name = root.relativize(path).toString().replace('\\', '/')
                if (name.endsWith('.java')) {
                    sources[name] = HexFormat.of().formatHex(MessageDigest.getInstance('SHA-256').digest(Files.readAllBytes(path)))
                }
            }
        }
        if (sources.isEmpty()) {
            throw new GradleException('No generated Java sources to fingerprint')
        }
        def actual = [target: inventoryData.target, wheelVersion: wheelVersion.get(),
            declarations: inventoryData.declarations.collectEntries { [(it.name): it] }, sources: sources]
        if (reference.isPresent()) {
            def expected = parser.parse(reference.get().asFile)
            compare(expected, actual)
            logger.lifecycle("PGM declarations and generated Java bytes match ${expected.target} on ${actual.target}.")
        }
        def file = destination.get().asFile
        file.parentFile.mkdirs()
        file.setText(JsonOutput.prettyPrint(JsonOutput.toJson(actual)) + '\n', 'UTF-8')
    }

    private static void compare(Map expected, Map actual) {
        String targets = "${expected.target} vs ${actual.target}"
        if (expected.wheelVersion != actual.wheelVersion) {
            throw new GradleException("${targets}: PGM wheel versions differ: ${expected.wheelVersion} vs ${actual.wheelVersion}")
        }
        for (def name in (expected.declarations.keySet() + actual.declarations.keySet()).toSorted()) {
            def before = expected.declarations[name]
            def after = actual.declarations[name]
            if (before?.shape != after?.shape) {
                throw new GradleException("${targets}: declaration ${name} differs: "
                    + "${before?.location ?: 'missing'} ${before?.shape ?: ''} vs "
                    + "${after?.location ?: 'missing'} ${after?.shape ?: ''}")
            }
        }
        for (def name in (expected.sources.keySet() + actual.sources.keySet()).toSorted()) {
            if (expected.sources[name] != actual.sources[name]) {
                throw new GradleException("${targets}: generated source ${name} differs: "
                    + "${expected.sources[name] ?: 'missing'} vs ${actual.sources[name] ?: 'missing'}")
            }
        }
    }
}
