# Repository Guidelines

## Project Structure
This repository is a single-module Maven project: `pay-java-adapay`.
It provides an Adapay (斗拱支付) integration built on top of the upstream `com.egzosn:pay-java-common` library.
The project follows the standard Maven layout: `src/main/java` for production code and `src/test/java` for tests.

## Build, Test, and Development Commands
Use Maven from the repository root.

- `mvn clean install -DskipTests`: build and install the artifact to the local Maven cache.
- `mvn clean compile`: compile production sources.
- `mvn dependency:tree`: inspect the dependency graph.

The build targets Java 8.

## Coding Style & Naming Conventions
Match the existing Java style: 4-space indentation, UTF-8 sources, `com.holuntech.pay.adapay.*` packages, and PascalCase class names such as `AdapayPayService` or `AdapayPayConfigStorage`. Keep the public API backward-compatible when possible.

## Testing Guidelines
Existing tests are integration samples like `PayTest` and require real Adapay credentials to run. For CI verification, use `mvn clean compile` or `mvn clean install -DskipTests`.

## Commit & Pull Request Guidelines
Keep commits focused and use concise imperative summaries. Open pull requests against `develop`, not `master`. Follow `PULL_REQUEST_TEMPLATE.zh-CN.md`.

## Security & Configuration Tips
Do not commit live merchant keys, certificates, or account secrets. Keep real credentials in local, untracked configuration.
