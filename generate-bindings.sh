#!/usr/bin/env bash

VERSION="${VERSION:-1.13.109}"

mkdir -p pgm_build
rm -rf pgm_build/*
pushd pgm_build

mkdir -p wheels
wget -O wheels/macosx-arm64.whl "https://github.com/PowerGridModel/power-grid-model/releases/download/v$VERSION/power_grid_model-$VERSION-py3-none-macosx_13_0_arm64.whl"
wget -O wheels/macosx-x86_64.whl "https://github.com/PowerGridModel/power-grid-model/releases/download/v$VERSION/power_grid_model-$VERSION-py3-none-macosx_13_0_x86_64.whl"
wget -O wheels/linux-aarch64.whl "https://github.com/PowerGridModel/power-grid-model/releases/download/v$VERSION/power_grid_model-$VERSION-py3-none-manylinux_2_24_aarch64.manylinux_2_28_aarch64.whl"
wget -O wheels/linux-x86_64.whl "https://github.com/PowerGridModel/power-grid-model/releases/download/v$VERSION/power_grid_model-$VERSION-py3-none-manylinux_2_26_x86_64.manylinux_2_28_x86_64.whl"
wget -O wheels/windows-amd64.whl "https://github.com/PowerGridModel/power-grid-model/releases/download/v$VERSION/power_grid_model-$VERSION-py3-none-win_amd64.whl"

mkdir lib
unzip -p wheels/macosx-arm64.whl power_grid_model/_core/power_grid_model_c/lib/libpower_grid_model_c.dylib \
    > lib/power_grid_model_c_arm64_macosx.dylib

unzip -p wheels/macosx-x86_64.whl power_grid_model/_core/power_grid_model_c/lib/libpower_grid_model_c.dylib \
    > lib/power_grid_model_c_x86_64_macosx.dylib

unzip -p wheels/linux-aarch64.whl power_grid_model/_core/power_grid_model_c/lib64/libpower_grid_model_c.so \
    > lib/power_grid_model_c_arm64_linux.so

unzip -p wheels/linux-x86_64.whl power_grid_model/_core/power_grid_model_c/lib64/libpower_grid_model_c.so \
    > lib/power_grid_model_c_x86_64_linux.so

unzip -p wheels/windows-amd64.whl power_grid_model/_core/power_grid_model_c/bin/power_grid_model_c.dll \
    > lib/power_grid_model_c_x86_64_windows.dll

mkdir tmp
unzip -d tmp wheels/macosx-arm64.whl 'power_grid_model/_core/power_grid_model_c/include/*'
mv tmp/power_grid_model/_core/power_grid_model_c/include ./
rm -rf tmp/

mkdir java
jextract \
  --output java \
  --target-package org.lfenergy.pgm \
  --header-class-name PowerGridModelC \
  include/power_grid_model_c.h \
  include/power_grid_model_c/dataset_definitions.h

popd
rm -rf src/generated/java/*
mkdir -p src/generated/java/resources

# Move the binaries to generated source set resource dir≈
mv pgm_build/lib/* src/generated/java/resources

# Move the generated Java files to the generated source set
mv pgm_build/java/* src/generated/java

# Maybe we want to keep it sometimes?
rm -rf pgm_build
