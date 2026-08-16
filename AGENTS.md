# AGENTS.md

## Project overview

`krpc` is a Kotlin/JVM Gradle multi-module project. It currently contains an
experimental RPC/route DSL and a small Ktor server used to exercise it.

## Modules

- `app`: application and DSL experiments. The primary source file is
  `app/src/main/kotlin/KRpcDSL.kt`.
- `server`: Ktor/Netty server. Its entry point is
  `de.pr.loaf.software.server.ApplicationKt` and it listens on port 8080.
- `utils`: shared Kotlin utilities and serialization/coroutines dependencies.
- `buildSrc`: shared Gradle convention plugin used by the Kotlin modules.

## Build and test

Always use the Gradle wrapper from the repository root:

```sh
./gradlew build
./gradlew check
./gradlew :server:run
./gradlew :<module>:test
```

The project uses Kotlin 2.4 and JDK 25 through the shared convention plugin.
Run the narrowest relevant Gradle task after a change; use `check` or `build`
before handing off broader changes.

## Code conventions

- Write production code in Kotlin under each module's `src/main/kotlin` tree.
- Keep package declarations aligned with the module's existing package layout.
- Prefer small, focused APIs and tests close to the behavior being changed.
- Use `kotlinx.serialization` and the version catalog for dependency additions;
  do not hard-code dependency versions in individual module build files.
- Reuse the `buildsrc.convention.kotlin-jvm` plugin for JVM module defaults.
- Preserve the current experimental DSL intent unless the task explicitly calls
  for an API redesign.

## Gradle conventions

- Add project-wide dependency versions and aliases in
  `gradle/libs.versions.toml`.
- Add a new module to `settings.gradle.kts` and apply the shared convention
  plugin unless there is a documented reason not to.
- Keep build configuration Kotlin DSL (`*.gradle.kts`).

## Repository hygiene

- Do not commit generated build outputs, local IDE state, or credentials.
- Do not overwrite unrelated working-tree changes; this repository may be in
  active development.
- Update `README.md` when a change alters how users build, run, or consume the
  project.
