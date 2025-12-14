#!/usr/bin/env bash
# Convert Whisper Tiny and Phi-3-mini to SNPE .dlc format using Qualcomm QNN/SNPE tools.
# Requires SNPE SDK installed at ../snpe-sdk/ and Python env with model export deps.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SNPE_ROOT="${SNPE_ROOT:-$ROOT/snpe-sdk}"
MODELS="$ROOT/models"

echo "==> Vox model conversion pipeline"
echo "    SNPE SDK: $SNPE_ROOT"
echo "    Output:   $MODELS"

if [[ ! -d "$SNPE_ROOT/include/SNPE" ]]; then
  echo "ERROR: SNPE SDK not found. Extract the Qualcomm SNPE SDK into snpe-sdk/."
  exit 1
fi

mkdir -p "$MODELS/whisper" "$MODELS/phi3"

echo ""
echo "1. Whisper Tiny (OpenAI whisper-tiny)"
echo "   Export ONNX → quantize → snpe-onnx-to-dlc"
echo "   Place output at: models/whisper/whisper_tiny.dlc"
echo ""
echo "2. Phi-3-mini-4k-instruct (Microsoft)"
echo "   Export ONNX → quantize → snpe-onnx-to-dlc"
echo "   Place output at: models/phi3/phi3_mini_4k_instruct.dlc"
echo ""
echo "See Qualcomm QIDK documentation for platform-specific conversion flags (HTP/DSP)."
