#!/bin/bash
#
# BNPPF OpenAPI Validator CLI - Linux/Unix Launcher
#
# This script launches the BNPPF OpenAPI Validator command-line tool.
#
# Usage: bnppf-validator.sh <command> [options]
#
# Commands:
#   validate-spec      Validate an OpenAPI specification
#   validate-request   Validate an HTTP request
#   validate-response  Validate an HTTP response
#
# Examples:
#   ./bnppf-validator.sh validate-spec --spec openapi.yaml
#   ./bnppf-validator.sh validate-request --spec openapi.yaml --method GET --path /pets
#   ./bnppf-validator.sh --help
#

# Determine the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set the lib directory
LIB_DIR="${SCRIPT_DIR}/lib"

# Check if lib directory exists
if [ ! -d "${LIB_DIR}" ]; then
    echo "Error: Library directory not found: ${LIB_DIR}"
    echo "Please ensure the distribution was extracted correctly."
    exit 1
fi

# Build the classpath from all JARs in the lib directory
CLASSPATH=""
for jar in "${LIB_DIR}"/*.jar; do
    if [ -f "${jar}" ]; then
        if [ -z "${CLASSPATH}" ]; then
            CLASSPATH="${jar}"
        else
            CLASSPATH="${CLASSPATH}:${jar}"
        fi
    fi
done

# Check if any JARs were found
if [ -z "${CLASSPATH}" ]; then
    echo "Error: No JAR files found in ${LIB_DIR}"
    exit 1
fi

# Set Java options (can be overridden by JAVA_OPTS environment variable)
if [ -z "${JAVA_OPTS}" ]; then
    JAVA_OPTS="-Xmx256m"
fi

# Find Java executable
if [ -n "${JAVA_HOME}" ]; then
    JAVA_CMD="${JAVA_HOME}/bin/java"
else
    JAVA_CMD="java"
fi

# Check if Java is available
if ! command -v "${JAVA_CMD}" &> /dev/null; then
    echo "Error: Java not found. Please install Java 11 or higher."
    echo "You can set JAVA_HOME to point to your Java installation."
    exit 1
fi

# Run the CLI
exec "${JAVA_CMD}" ${JAVA_OPTS} -cp "${CLASSPATH}" be.bnppf.openvalidator.cli.OpenAPIValidatorCLI "$@"
