#!/bin/bash
# Cache all npm dependencies in the project for offline builds

set -e

echo "=========================================="
echo "npm Dependency Caching Script"
echo "=========================================="
echo ""

cd frontend

echo "Step 1: Installing dependencies with npm ci (clean install)..."
npm ci

echo ""
echo "Step 2: Checking node_modules size..."
du -sh node_modules

echo ""
echo "Step 3: Verifying frontend builds..."
npm run build

echo ""
echo "Step 4: Adding node_modules to git..."
echo "⚠️  WARNING: node_modules is typically gitignored, but we're committing it for offline builds"
git add -f node_modules/

echo ""
echo "=========================================="
echo "✅ Frontend Dependencies Cached!"
echo "=========================================="
echo ""
echo "node_modules size: $(du -sh node_modules | cut -f1)"
echo ""
echo "Next steps:"
echo "  1. git commit -m 'Cache npm dependencies for offline builds'"
echo "  2. git push"
echo ""
echo "After pushing, Claude Code can build with:"
echo "  cd frontend && npm run build"
echo ""
