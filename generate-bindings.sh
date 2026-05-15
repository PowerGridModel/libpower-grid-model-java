#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  OUT_DIR=<output_directory> PGM_DIR=<power_grid_model_root> ./generate-bindings.sh

Required environment variables:
  OUT_DIR  Directory where generated bindings will be written
  PGM_DIR  Root directory of the power-grid-model repository
EOF
}

if [[ -z "${OUT_DIR:-}" || -z "${PGM_DIR:-}" ]]; then
  usage
  exit 1
fi

rm -rf "$OUT_DIR/org"
mkdir -p "$OUT_DIR"

jextract \
  --output "$OUT_DIR" \
  --target-package org.lfenergy.pgm \
  --header-class-name PowerGridModelC \
  --include-dir /home/gerrit/Projects/Alliander/power-grid-model/power_grid_model_c/power_grid_model_c/include \
  --library :libpower_grid_model_c.so.1.13 \
  "$PGM_DIR/power_grid_model_c/power_grid_model_c/include/power_grid_model_c.h" \
  "$PGM_DIR/power_grid_model_c/power_grid_model_c/include/power_grid_model_c/dataset_definitions.h"
