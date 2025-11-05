#!/bin/bash
# Script to add H2 in-memory database dependency for tests
# Run this script locally on your Mac

set -e

echo "Adding H2 database dependency to backend..."

cd "$(dirname "$0")/backend"

# Check if H2 is already in pom.xml
if grep -q "com.h2database" pom.xml; then
    echo "H2 dependency already exists in pom.xml"
else
    echo "H2 not found in pom.xml - will be added manually"
fi

# Download H2 dependency (latest stable 2.x version compatible with Spring Boot 3.2)
echo "Downloading H2 database JAR..."
mvn dependency:get \
    -Dartifact=com.h2database:h2:2.2.224:jar \
    -Dmaven.repo.local=.mvn/repository

echo "H2 dependency downloaded successfully!"

# Remove metadata files that can cause issues
echo "Cleaning up metadata files..."
find .mvn/repository -name "_remote.repositories" -type f -delete

echo ""
echo "============================================"
echo "Next steps:"
echo "1. Add the following to backend/pom.xml in the <dependencies> section:"
echo ""
echo "    <!-- H2 In-Memory Database for Testing -->"
echo "    <dependency>"
echo "      <groupId>com.h2database</groupId>"
echo "      <artifactId>h2</artifactId>"
echo "      <version>2.2.224</version>"
echo "      <scope>test</scope>"
echo "    </dependency>"
echo ""
echo "2. Commit and push the changes:"
echo "   git add backend/pom.xml backend/.mvn/repository/"
echo "   git commit -m \"Add H2 in-memory database for tests\""
echo "   git push"
echo ""
echo "3. Let Claude know when done so it can continue"
echo "============================================"
