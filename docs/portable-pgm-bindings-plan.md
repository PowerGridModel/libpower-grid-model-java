<!--
SPDX-FileCopyrightText: Contributors to the Power Grid Model project <powergridmodel@lfenergy.org>

SPDX-License-Identifier: MPL-2.0
-->

# Plan: Portable Java bindings for the PGM C API

Replace jextract with a PGM-specific generator that uses Clang to read the public C headers and emits one set of Java Foreign Function & Memory (FFM) bindings. The headers from the pinned PGM release remain the source of truth. A new export that follows the agreed rules should get a low-level Java binding automatically on the next PGM upgrade, without a hand-maintained function list or a separate upstream manifest.

## Assumptions and proposed agreement with PGM

- Support the currently packaged 64-bit targets: Windows x86-64, macOS x86-64 and ARM64, and Linux x86-64 and ARM64. Do not assume that every 64-bit target is supported. Continue shipping and loading the matching native PGM library; the JDK's native linker handles each platform's calling convention.
- The public declarations must describe the same exports and compatible value representations on all supported targets. Platform-specific export annotations are fine; platform-specific signatures or exports are not part of this proposal.
- Allow fixed-width integers and the existing PGM aliases. Allow `size_t` explicitly: it is unsigned and 64 bits on these targets, even though its underlying C type differs. Disallow plain `long` and `unsigned long` in public signatures, including behind aliases, because their widths differ between Windows and the other targets.
- Support the declaration forms already used by the API: functions, typedefs (including opaque handles), enum constants, exported pointer globals, and function-pointer callbacks. Treat new forms, such as variadic functions or structs and enums passed by value, as unsupported until deliberately added. Reject unsupported exports with the declaration name and source location rather than silently skipping them.

## Generation

1. Use Clang at generation time to preprocess and parse `power_grid_model_c.h` and `power_grid_model_c/dataset_definitions.h`. Let Clang handle includes, macro expansion, enum values, and C declarators rather than implementing a C parser. Neither Clang nor jextract should be needed by consumers of the published Java artifact.
2. Read structured declarations and retain the declared type or typedef chain. In particular, do not flatten `size_t` to Linux's `unsigned long` and use that name to look up a runtime layout. Validate declarations against the agreed type and declaration rules. Pin the Clang version used for generation.
3. Emit the Java FFM layouts and calls from the approved types:

   | C declaration | Java carrier and layout |
   | --- | --- |
   | `PGM_ID` (`int32_t`) | `int`, 32-bit integer layout |
   | `PGM_Idx` (`int64_t`) | `long`, 64-bit integer layout |
   | `size_t` | `long`, native linker's `size_t` layout |
   | `int`, `double` | `int` and `double`, using the corresponding native layouts |
   | Object and function pointers | `MemorySegment`, address layout; retain the pointee or callback signature where needed |

4. Integrate generation with Gradle. Check compatibility with the current `PowerGridModelC` API before removing the jextract download and generation tasks; identify any intentional Java API changes. Keep ownership, lifetimes, and other high-level Java behavior explicit rather than inferring them from C signatures.

## Verification

- Test that a new supported declaration produces a binding and an unsupported declaration fails with an actionable diagnostic. Check that the supported targets expose equivalent declarations after normalization, so a conditional export cannot be missed by parsing on only one host.
- Build one Java artifact and test that exact artifact against the matching native library on each supported OS/architecture pair. Cover a `size_t` return (for example, `PGM_meta_component_size`), `PGM_get_indexer` reading `PGM_ID` values and writing `PGM_Idx` values, an exported dataset metadata pointer, and a logger callback.
- Remove jextract only after the generated API and cross-platform tests pass.
