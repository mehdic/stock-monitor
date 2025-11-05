#!/bin/bash
# Script to set up Maven Wrapper for Claude Code environment
# Run this locally after pulling the branch

set -e  # Exit on error

echo "=================================="
echo "Maven Wrapper Setup Script"
echo "=================================="
echo ""

# Navigate to backend directory
cd backend

echo "Step 1: Generating Maven Wrapper..."
mvn wrapper:wrapper -Dmaven=3.9.11

echo ""
echo "Step 2: Making wrapper scripts executable..."
chmod +x mvnw
chmod +x .mvn/wrapper/maven-wrapper.jar

echo ""
echo "Step 3: Staging Maven Wrapper files for commit..."
git add mvnw mvnw.cmd .mvn/

echo ""
echo "Step 4: Verifying what will be committed..."
git status

echo ""
echo "=================================="
echo "✅ Maven Wrapper Setup Complete!"
echo "=================================="
echo ""
echo "Files added to staging:"
echo "  - mvnw (Unix wrapper script)"
echo "  - mvnw.cmd (Windows wrapper script)"
echo "  - .mvn/wrapper/ (wrapper configuration and JAR)"
echo ""
echo "Next steps:"
echo "  1. Review the changes with: git status"
echo "  2. Commit with: git commit -m 'Add Maven Wrapper for consistent builds'"
echo "  3. Push with: git push"
echo ""
echo "After pushing, Claude Code can use: ./mvnw test"
echo ""
