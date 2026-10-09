#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
TOOLS_DIR="$ROOT_DIR/tools"
LIB_DIR="$ROOT_DIR/lib"

mkdir -p "$TOOLS_DIR" "$LIB_DIR"

BYTECODER_VERSION="2024-05-10"
CLI_URL="https://repo1.maven.org/maven2/de/mirkosertic/bytecoder/bytecoder-cli/${BYTECODER_VERSION}/bytecoder-cli-${BYTECODER_VERSION}.jar"

echo "=== Downloading Bytecoder CLI ${BYTECODER_VERSION} ==="
if [ ! -f "$TOOLS_DIR/bytecoder-cli.jar" ]; then
    curl -sSL -o "$TOOLS_DIR/bytecoder-cli.jar" "$CLI_URL"
    echo "Downloaded to $TOOLS_DIR/bytecoder-cli.jar"
else
    echo "bytecoder-cli.jar already present."
fi

echo "Extracting API libraries into $LIB_DIR..."
python3 -c "
import zipfile, os
z = zipfile.ZipFile('$TOOLS_DIR/bytecoder-cli.jar')
for name in ['BOOT-INF/lib/bytecoder.api-${BYTECODER_VERSION}.jar', 'BOOT-INF/lib/bytecoder.web-${BYTECODER_VERSION}.jar', 'BOOT-INF/lib/bytecoder-core-${BYTECODER_VERSION}.jar']:
    basename = os.path.basename(name)
    with open('$LIB_DIR/' + basename, 'wb') as f:
        f.write(z.read(name))
    print('Extracted:', basename)
"
echo "Bytecoder toolchain setup complete!"
