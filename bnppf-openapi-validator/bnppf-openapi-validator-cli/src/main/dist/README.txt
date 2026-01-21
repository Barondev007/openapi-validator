================================================================================
                     BNPPF OpenAPI Validator CLI
                         Version ${project.version}
================================================================================

DESCRIPTION
-----------
The BNPPF OpenAPI Validator CLI is a command-line tool for validating HTTP
requests and responses against OpenAPI 2.0 (Swagger) and OpenAPI 3.x
specifications.

REQUIREMENTS
------------
- Java 11 or higher (Java 17 recommended)

INSTALLATION
------------
1. Extract this archive to your desired location
2. Add the extraction directory to your PATH (optional)

USAGE
-----
Windows:
  bnppf-validator.bat <command> [options]

Linux/Unix:
  ./bnppf-validator.sh <command> [options]

COMMANDS
--------
validate-spec      Validate an OpenAPI specification file
validate-request   Validate an HTTP request against a specification
validate-response  Validate an HTTP response against a specification

OPTIONS
-------
Common options for all commands:
  -h, --help           Show help message
  -V, --version        Show version information

validate-spec options:
  -s, --spec <path>    Path or URL to the OpenAPI specification (required)
  -l, --level <level>  Validation level: light, lenient, strict (default: lenient)
  -o, --output <fmt>   Output format: text, json, junit (default: text)

validate-request options:
  -s, --spec <path>    Path or URL to the OpenAPI specification (required)
  -m, --method <http>  HTTP method: GET, POST, PUT, DELETE, etc. (required)
  -p, --path <path>    Request path, e.g., /pets/123 (required)
  -b, --body <body>    Request body as string
  --body-file <file>   Path to file containing request body
  --stdin              Read request body from stdin
  -H, --header <h>     HTTP header (repeatable), format: 'Header-Name: value'
  -q, --query <q>      Query parameter (repeatable), format: 'name=value'
  --content-type <ct>  Content-Type header (shorthand)
  -l, --level <level>  Validation level: light, lenient, strict (default: lenient)
  -o, --output <fmt>   Output format: text, json, junit (default: text)
  --fail-on-warn       Exit with error code if warnings are found

validate-response options:
  -s, --spec <path>    Path or URL to the OpenAPI specification (required)
  -m, --method <http>  HTTP method used in the request (required)
  -p, --path <path>    Request path (required)
  --status <code>      HTTP response status code (required)
  -b, --body <body>    Response body as string
  --body-file <file>   Path to file containing response body
  --stdin              Read response body from stdin
  -H, --header <h>     Response header (repeatable), format: 'Header-Name: value'
  --content-type <ct>  Content-Type header (shorthand)
  -l, --level <level>  Validation level: light, lenient, strict (default: lenient)
  -o, --output <fmt>   Output format: text, json, junit (default: text)
  --fail-on-warn       Exit with error code if warnings are found

VALIDATION LEVELS
-----------------
light    - Most permissive: ignores additional properties, relaxes type checking
lenient  - Balanced: validates structure but allows extra fields (default)
strict   - Most strict: enforces all schema constraints

EXAMPLES
--------
1. Validate a specification file:
   bnppf-validator.bat validate-spec --spec petstore.yaml

2. Validate a GET request:
   bnppf-validator.bat validate-request --spec petstore.yaml --method GET --path /pets

3. Validate a POST request with body:
   bnppf-validator.bat validate-request --spec petstore.yaml --method POST --path /pets \
     --body '{"name": "doggie", "status": "available"}' \
     --header "Content-Type: application/json"

4. Validate a response:
   bnppf-validator.bat validate-response --spec petstore.yaml --method GET --path /pets \
     --status 200 --body '[{"id": 1, "name": "doggie"}]' \
     --header "Content-Type: application/json"

5. Use JSON output format:
   bnppf-validator.bat validate-spec --spec petstore.yaml --output json

6. Use strict validation:
   bnppf-validator.bat validate-request --spec petstore.yaml --method GET --path /pets \
     --level strict

EXIT CODES
----------
0 - Validation passed (no errors)
1 - Validation failed (errors found)
2 - Invalid arguments or configuration error

ENVIRONMENT VARIABLES
---------------------
JAVA_HOME   - Path to Java installation (optional if java is in PATH)
JAVA_OPTS   - JVM options (default: -Xmx256m)

LICENSE
-------
Copyright (c) BNPPF. All rights reserved.

SUPPORT
-------
For issues and support, please contact your system administrator.

================================================================================
