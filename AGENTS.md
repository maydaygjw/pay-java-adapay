# Repository Guidelines

## Project Structure & Module Organization
This repository is a multi-module Maven build rooted at `pay-java-parent`. Core abstractions live in `pay-java-common`, web callback helpers in `pay-java-web-support`, and each provider integration in `pay-java-*` modules such as `pay-java-ali`, `pay-java-wx`, and `pay-java-paypal`. The runnable example app is `pay-java-demo`, which packages a WAR and includes `src/main/java`, `src/main/resources`, and `src/main/webapp`. Most library modules follow the standard Maven layout: `src/main/java` for production code and `src/test/java` for samples or tests where present.

## Build, Test, and Development Commands
Use Maven from the repository root.

- `mvn clean install`: build all modules and install artifacts to the local Maven cache.
- `mvn -pl pay-java-wx -am package`: rebuild one provider module plus its required dependencies.
- `mvn -pl pay-java-demo package`: build the demo WAR.
- `mvn -pl pay-java-demo tomcat7:run`: run the demo locally on port `8080`.

The parent build targets Java 17. `pay-java-demo` still compiles for Java 8, so avoid introducing newer language features there.

## Coding Style & Naming Conventions
Match the existing Java style: 4-space indentation, UTF-8 sources, `com.egzosn.pay...` packages, and PascalCase class names such as `WxPayService` or `BaiduPayConfigStorage`. Keep provider-specific code inside its module; shared utilities belong in `pay-java-common`. Prefer descriptive method names over abbreviations, and keep public API changes backward-compatible across modules.

## Testing Guidelines
Current coverage is light. Existing files under `src/test/java` are often executable integration samples like `PayTest` with a `main()` method, not full JUnit suites. When adding tests, place them in the owning module under `src/test/java`, keep class names ending in `Test`, and prefer isolated tests around signing, request building, and callback verification. For manual verification, run the relevant module build and exercise the demo app when web flows change.

## Commit & Pull Request Guidelines
Recent history uses short, direct subjects such as `add adapay` and version-prefixed messages like `2.14.9 微信公钥证书支持...`. Keep commits focused and use a concise imperative summary. This project follows git-flow; open pull requests against `develop`, not `master`. Follow `PULL_REQUEST_TEMPLATE.zh-CN.md`: link the issue, describe the change, note test coverage, and attach screenshots when UI or callback behavior changes.

## Security & Configuration Tips
Do not commit live merchant keys, certificates, or account secrets. Demo resources already contain certificate-like files; treat them as examples only and keep real credentials in local, untracked configuration.
