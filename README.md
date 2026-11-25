<!--
SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>

SPDX-License-Identifier: MPL-2.0
-->

[![Power Grid Model logo](https://raw.githubusercontent.com/PowerGridModel/.github/main/artwork/svg/color.svg)](#)

# libpower-grid-model-java

## Usage

The PGM methods are available through class `org.lfenergy.pgm.PowerGridModelC`. The method names are identical to the method names in the C-documentation.

To load the native library there are two options:
- on supported architectures, the library can be autoloaded using `new PGMLoader().autoload()`
- on other architectures, you can build and load the library yourself using `System.load(...)`, and verify it using `new PGMLoader().check(true)`.

## Supported architectures

- macOS ARM64
- macOS x86-64
- Linux ARM64
- Linux x86-64
- Windows x86-64

## Building from source

Install Java 25, CMake 3.20+, a native C/C++ toolchain with system headers, and
**LLVM/Clang 23.1.4** with LibTooling and CMake package files.
Use Xcode command-line tools on macOS, the Windows SDK and MSVC headers on
Windows, or `libxml2-dev`, `libzstd-dev`, and `zlib1g-dev` with the official
LLVM archive on Ubuntu.

Set `PGM_LLVM_DIR` to the absolute LLVM install path (or pass `-PpgmLlvmDir=...`)
and run `./gradlew build --configuration-cache` (`gradlew.bat` on Windows).
The pinned Linux x86-64 wheel supplies the C headers on every host. JAR
consumers don't need LLVM or CMake.

The generated API omits system-header helpers, `C_LONG`, and setters for const metadata.

### Verifying a build on another supported host

Verify the reference JAR's checksum and smoke-test that same JAR:

```sh
javac -cp path/to/reference.jar -d build/pgm/verification buildSrc/verification/BindingSmoke.java
java --enable-native-access=ALL-UNNAMED -cp "path/to/reference.jar:build/pgm/verification" BindingSmoke
```

On Windows, use `;` in the classpath. To test explicit loading, pass the path
to the matching library extracted from the JAR as an argument to `BindingSmoke`.

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
