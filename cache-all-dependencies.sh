#!/bin/bash
# Cache BOTH Maven and npm dependencies for complete offline builds

set -e

echo "=============================================="
echo "Complete Dependency Caching Script"
echo "=============================================="
echo ""

echo "PART 1: Caching Maven Dependencies (Backend)"
echo "----------------------------------------------"
./cache-dependencies.sh

echo ""
echo ""
echo "PART 2: Caching npm Dependencies (Frontend)"
echo "----------------------------------------------"
./cache-npm-dependencies.sh

echo ""
echo "=============================================="
echo "✅ ALL Dependencies Cached Successfully!"
echo "=============================================="
echo ""
echo "Summary:"
echo "  Backend (.mvn/repository): $(du -sh backend/.mvn/repository 2>/dev/null | cut -f1)"
echo "  Frontend (node_modules):   $(du -sh frontend/node_modules 2>/dev/null | cut -f1)"
echo ""
echo "Total cached dependencies ready for offline builds!"
echo ""
echo "Next step:"
echo "  git commit -m 'Cache all dependencies (Maven + npm) for offline builds'"
echo "  git push"
echo ""
