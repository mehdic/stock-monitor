# Maven Wrapper Setup Instructions

## Why Maven Wrapper?

Maven Wrapper ensures everyone uses the same Maven version and allows Claude Code to build/test your project without Maven DNS issues.

## Prerequisites

- Git installed
- Maven installed locally (to generate the wrapper)
- Access to your GitHub repository

## Steps to Execute Locally

### 1. Pull the Latest Changes

```bash
git clone https://github.com/mehdic/stock-monitor.git
cd stock-monitor
git checkout claude/fix-remaining-test-failures-011CUpXsSLGez6dF9oRyEit8
git pull
```

Or if you already have it cloned:

```bash
cd stock-monitor
git checkout claude/fix-remaining-test-failures-011CUpXsSLGez6dF9oRyEit8
git pull
```

### 2. Run the Setup Script

```bash
chmod +x setup-maven-wrapper.sh
./setup-maven-wrapper.sh
```

**What this script does:**
- Generates Maven Wrapper files using `mvn wrapper:wrapper`
- Makes wrapper scripts executable
- Adds all wrapper files to git staging
- Shows you what will be committed

### 3. Commit and Push

```bash
git commit -m "Add Maven Wrapper for consistent builds across environments"
git push
```

## What Gets Committed

The script will add these files:

```
backend/
├── mvnw              # Unix/Linux/Mac wrapper script
├── mvnw.cmd          # Windows wrapper script
└── .mvn/
    └── wrapper/
        ├── maven-wrapper.properties   # Wrapper configuration
        └── maven-wrapper.jar          # Wrapper binary
```

## File Sizes

- `mvnw`: ~10 KB (text script)
- `mvnw.cmd`: ~7 KB (text script)
- `maven-wrapper.jar`: ~50 KB (binary)
- `maven-wrapper.properties`: ~1 KB (text)

**Total: ~68 KB** - Small enough for Git, won't bloat your repository.

## After Pushing

Once you've pushed these changes, Claude Code will be able to:

```bash
# Instead of:
mvn test

# Use:
./mvnw test
```

The wrapper will:
1. Download the correct Maven version (if needed)
2. Download dependencies
3. Run tests

**No more DNS issues!** ✅

## Verification

After pushing, you can verify in Claude Code:

```bash
cd /home/user/stock-monitor/backend
./mvnw --version
./mvnw clean test
```

## Troubleshooting

### If the script fails:

**Maven not found:**
```bash
# Install Maven first (Mac)
brew install maven

# Or (Linux/Ubuntu)
sudo apt-get install maven
```

**Permission denied:**
```bash
chmod +x setup-maven-wrapper.sh
```

### If you need to regenerate:

```bash
cd backend
rm -rf .mvn mvnw mvnw.cmd
mvn wrapper:wrapper -Dmaven=3.9.11
```

## Why This Works

Maven Wrapper:
- ✅ Bundles Maven distribution with your project
- ✅ Downloads dependencies directly (bypasses DNS issues)
- ✅ Ensures consistent Maven version across all environments
- ✅ Standard practice in Java projects
- ✅ Small files (~68 KB total)

## Questions?

After you push, I (Claude Code) will be able to:
1. Use `./mvnw test` to run all 183 tests
2. Verify the 31 test fixes work correctly
3. Continue fixing the remaining 23 tests if needed

---

**Ready to proceed?** Run the setup script and push! 🚀
