# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StockMonitor is a Java project managed as an IntelliJ IDEA project. The project is in its initial stages with minimal implementation.

## Project Structure

- `src/` - Java source files
  - `Main.java` - Entry point of the application
- `.idea/` - IntelliJ IDEA project configuration
- `StockMonitor.iml` - IntelliJ IDEA module file

## Build and Run

This project uses IntelliJ IDEA's built-in build system (no Maven or Gradle configuration present).

**Compile and run:**
```bash
javac src/Main.java -d out/
java -cp out Main
```

**Run from IntelliJ IDEA:**
- Open the project in IntelliJ IDEA
- Right-click on `Main.java` and select "Run 'Main.main()'"
- Or use the Run configuration in the IDE toolbar

## Development Notes

- The project currently uses the default JDK configured in IntelliJ IDEA
- Build output directory: `out/` (gitignored)
- No external dependencies or build tool configuration currently present
