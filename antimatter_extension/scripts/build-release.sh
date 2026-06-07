#!/bin/bash
set -e

# Change to extension directory
cd "$(dirname "$0")/.."

echo "Bumping patch version..."
npm version patch

echo "Building and Packaging Extension..."
npm run build
vsce package --allow-missing-repository --allow-star-activation

echo "Moving VSIX to extension/ folder..."
mkdir -p extension
mv *.vsix extension/

echo "Done! The new version has been saved in antimatter_extension/extension/"
