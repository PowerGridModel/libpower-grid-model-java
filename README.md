<!--
SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>

SPDX-License-Identifier: MPL-2.0
-->

[![Power Grid Model logo](https://raw.githubusercontent.com/PowerGridModel/.github/main/artwork/svg/color.svg)](#)

# libpower-grid-model-java

## Usage

The PGM methods are available through class `org.lfenergy.pgm.PowerGridModelC`. The method names are identical to the method names in the C-documentation.

Use `PGMLoader.autoload()` to load the packaged native library, or call
`System.load(...)` from the same class loader as the bindings and then use
`PGMLoader.check(true)` to verify its version. On Java 25, enable native access
for the module using the bindings (use `--enable-native-access=ALL-UNNAMED` on the classpath).

## Supported architectures

- macOS ARM64
- macOS x86-64
- Linux ARM64
- Linux x86-64
- Windows x86-64

## Building from source

Use a native Java 25 JDK, CMake 3.20 or newer, a native C/C++ toolchain with
system headers, and **LLVM/Clang 23.1.4** with the LibTooling development
libraries and CMake package files. Set `PGM_LLVM_DIR` (or `-PpgmLlvmDir=...`)
to that installation's absolute prefix. On macOS, install the Xcode command-line
tools for the SDK. On Ubuntu, the official LLVM archive also needs the
`libxml2-dev`, `libzstd-dev`, and `zlib1g-dev` packages. On Windows, use an
x86-64 toolchain with the Windows SDK and MSVC headers.
Then run `./gradlew build --configuration-cache` (or
`.\gradlew.bat build --configuration-cache` on Windows). Consumers of the JAR
do not need Clang, CMake, or jextract.

The pinned Linux x86-64 wheel's public C headers drive generation on every host.
Clang parses them with the host's target and system headers. Fixed-width integers,
`size_t`, supported C primitives, pointers, opaque handles, enum constants,
pointer globals, and fixed-prototype callbacks are supported. Plain `long`
and `unsigned long`, variadic functions, and aggregates or enums passed by
value are not. On a wheel upgrade, review unsupported declarations at the
reported header location instead of dropping exports. The supported targets
must expose equivalent declarations and generate identical Java files.

PGM-facing function, layout, and callback helpers are retained. The generated
API no longer exposes incidental system headers, the host-specific `C_LONG`
layout, or setters for const metadata globals. Const metadata `$segment()`
helpers expose a read-only pointer slot. A pointer returned by a binding does
not transfer ownership or establish a safe accessible extent. In particular,
read logger callback data only during the callback and only for the supplied
length, keep the stub's arena alive, and do not throw from a callback.

### Verifying a build on another supported host

At the same Git commit and with the same pinned tools, compare against the
reference `build/pgm/binding-fingerprint.json` from `./gradlew fingerprintPGMBindings`.
Save the reference outside the local build output and pass it to the same task
as `-PpgmBindingReference=/absolute/path/to/reference.json`. It compares normalized
declarations and SHA-256 digests of the generated Java files, using the same
Linux x86-64 wheel as input on both hosts.

Verify the reference JAR's checksum separately, then compile and run the smoke
test against that exact JAR without rebuilding it:

```sh
javac -cp path/to/reference.jar -d build/pgm/verification buildSrc/verification/BindingSmoke.java
java --enable-native-access=ALL-UNNAMED -cp "path/to/reference.jar:build/pgm/verification" BindingSmoke
```

Use `;` instead of `:` as the classpath separator on Windows. To test explicit
loading in a fresh JVM, rerun the `java` command with the path to the matching
native library (extracted from the reference JAR) after `BindingSmoke`.

## Checklist after creating a repository from this template

Update the following items manually before publishing your repository:

- [x] Rename the repository and update all references in this file.
- [x] Replace all `pgm-template-repo` references across the repository, especially in this README and REUSE.toml.
- [x] Replace the project title and description in this README.
- [ ] Ensure all third-party licenses are present in [LICENSES](https://github.com/PowerGridModel/libpower-grid-model-java/tree/main/LICENSES).

### Optional checks

Change these if applicable.

- [ ] Replace Power Grid Model project-specific links (for example contributing, support, release, security, and code of conduct links) 
with links for your project.
- [ ] Add logos and badges corresponding to relevant pages
- [ ] Verify [LICENSE](https://github.com/PowerGridModel/libpower-grid-model-java/blob/main/LICENSE) copyright holder and year(s).
Also verify SPDX copyright headers in source and documentation files.
- [ ] Include documents present in [Home page of PGM org](https://github.com/PowerGridModel/.github/) in documentation if they are created.

## License

This project is licensed under the Mozilla Public License, version 2.0 - see
[LICENSE](https://github.com/PowerGridModel/libpower-grid-model-java/blob/main/LICENSE) for details.

## Licenses third-party libraries

This project includes third-party libraries,
which are licensed under their own respective Open-Source licenses.
SPDX-License-Identifier headers are used to show which license is applicable.
The concerning license files can be found in the
[LICENSES](https://github.com/PowerGridModel/libpower-grid-model-java/tree/main/LICENSES) directory.

## Contributing

Please read [CODE_OF_CONDUCT](https://github.com/PowerGridModel/.github/blob/main/CODE_OF_CONDUCT.md) and [CONTRIBUTING](https://github.com/PowerGridModel/.github/blob/main/CONTRIBUTING.md) for details on the process 
for submitting pull requests to us.

## Citations

If you are using Power Grid Model in your research work, please consider citing our library using the following
references.

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.8054429.svg)](https://zenodo.org/record/8054429)

```bibtex
@software{Xiang_PowerGridModel_power-grid-model,
  author = {Xiang, Yu and Salemink, Peter and van Westering, Werner and Bharambe, Nitish and Govers, Martinus G.H. and van den Bogaard, Jonas and Stoeller, Bram and Wang, Zhen and Guo, Jerry Jinfeng and Figueroa Manrique, Santiago and Jagutis, Laurynas and Wang, Chenguang and van Raalte, Marc and {Contributors to the LF Energy project Power Grid Model}},
  doi = {10.5281/zenodo.8054429},
  license = {MPL-2.0},
  title = {{PowerGridModel/power-grid-model}},
  url = {https://github.com/PowerGridModel/power-grid-model}
}
@inproceedings{Xiang2023,
  author = {Xiang, Yu and Salemink, Peter and Stoeller, Bram and Bharambe, Nitish and van Westering, Werner},
  booktitle={27th International Conference on Electricity Distribution (CIRED 2023)},
  title={Power grid model: a high-performance distribution grid calculation library},
  year={2023},
  volume={2023},
  number={},
  pages={1089-1093},
  keywords={},
  doi={10.1049/icp.2023.0633}
}
```

## Contact

Please read [SUPPORT](https://github.com/PowerGridModel/.github/blob/main/SUPPORT.md) for how to connect and get into
contact with the Power Grid Model project.
