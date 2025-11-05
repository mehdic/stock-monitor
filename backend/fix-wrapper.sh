#!/bin/bash
# Fix: Generate full Maven Wrapper (not just scripts)
cd "$(dirname "$0")"

echo "Regenerating Maven Wrapper with JAR included..."
mvn wrapper:wrapper -Dmaven=3.9.11

echo ""
echo "Making scripts executable..."
chmod +x mvnw mvnw.cmd

echo ""
echo "Verifying wrapper files exist..."
ls -lh mvnw mvnw.cmd .mvn/wrapper/

echo ""
echo "Testing wrapper..."
./mvnw --version
