#!/bin/bash
# Cache all Maven dependencies in the project for offline builds

set -e

echo "=========================================="
echo "Maven Dependency Caching Script"
echo "=========================================="
echo ""

cd backend

echo "Step 1: Creating local repository directory..."
mkdir -p .mvn/repository

echo ""
echo "Step 2: Configuring Maven to use local repository..."
cat > .mvn/maven.config << 'EOF'
-Dmaven.repo.local=.mvn/repository
EOF

echo ""
echo "Step 3: Downloading ALL dependencies..."
echo "This may take 5-10 minutes depending on your connection..."
mvn dependency:go-offline -Dmaven.repo.local=.mvn/repository

echo ""
echo "Step 4: Copying any missing dependencies..."
mvn dependency:resolve -Dmaven.repo.local=.mvn/repository
mvn dependency:resolve-plugins -Dmaven.repo.local=.mvn/repository

echo ""
echo "Step 5: Checking repository size..."
du -sh .mvn/repository

echo ""
echo "Step 6: Adding to git (this may take a moment)..."
git add .mvn/repository .mvn/maven.config

echo ""
echo "=========================================="
echo "✅ Dependencies Cached Successfully!"
echo "=========================================="
echo ""
echo "Repository size: $(du -sh .mvn/repository | cut -f1)"
echo ""
echo "Next steps:"
echo "  1. git commit -m 'Cache Maven dependencies for offline builds'"
echo "  2. git push"
echo ""
echo "After pushing, Claude Code can build with:"
echo "  cd backend && mvn clean test"
echo ""
