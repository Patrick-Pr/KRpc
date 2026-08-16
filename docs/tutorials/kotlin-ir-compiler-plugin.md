# A Minimal Kotlin IR Compiler Plugin

This tutorial shows how to set up a small Kotlin IR compiler plugin in this
repository. The `app` module is the code the plugin transforms.

## 1. What an IR plugin does

The Kotlin compiler roughly follows this pipeline:

```text
Kotlin source
  -> parsing and type/name resolution (FIR)
  -> typed IR
  -> IR lowerings
  -> JVM bytecode
```

An IR plugin runs after source has been resolved into typed IR and before JVM
bytecode generation. It can inspect or rewrite declarations and expressions.
For example, it can replace a function body or add instrumentation around
calls.

IR is too late to add declarations that Kotlin source or the IDE can see. That
requires a FIR component as well. The backend extension point is
`IrGenerationExtension`.

See the [Kotlin custom compiler plugin documentation](https://kotlinlang.org/docs/custom-compiler-plugins.html)
for the compiler pipeline and API caveats.

## 2. Use a separate compiler-plugin module

The plugin cannot live in `app` and transform the same `app` compilation.
Kotlin must load the plugin JAR before compiling `app`.

```text
krpc
├── app                 <- sample code transformed by the plugin
├── compiler-plugin     <- the compiler loads this JAR
├── utils
└── buildSrc
```

For a distributable plugin, add these modules later:

```text
plugin-annotations      <- annotations used by consumers
gradle-plugin            <- provides plugins { id("…") }
```

They are unnecessary for this first experiment.

## 3. Register the module and compiler dependency

Add this to `settings.gradle.kts`:

```kotlin
include(":compiler-plugin")
```

Add this alias to `gradle/libs.versions.toml`:

```toml
kotlinCompilerEmbeddable = {
    module = "org.jetbrains.kotlin:kotlin-compiler-embeddable",
    version.ref = "kotlin"
}
```

Use the repository's existing Kotlin version. Compiler plugin APIs are unstable
and closely tied to the compiler version, so update the plugin with Kotlin.

## 4. Configure the plugin project

Create `compiler-plugin/build.gradle.kts`:

```kotlin
plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    compileOnly(libs.kotlinCompilerEmbeddable)
}
```

Use `compileOnly`: the compiler provides these classes when it loads a
plugin. Do not bundle a compiler implementation into the plugin JAR.

## 5. Register the IR extension

Create `compiler-plugin/src/main/kotlin/dev/krpc/plugin/DemoRegistrar.kt`:

```kotlin
package dev.krpc.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class DemoRegistrar : CompilerPluginRegistrar() {
    override val pluginId = "dev.krpc.ir-demo"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        IrGenerationExtension.registerExtension(ReplaceBodyExtension())
    }
}
```

`DemoRegistrar` is Kotlin's entry point into the plugin. Its unique
`pluginId` is used for plugin configuration and ordering.
`registerExtensions` attaches the code that receives the module's IR.

## 6. Register the plugin JAR with ServiceLoader

Create this resource file:

```text
compiler-plugin/src/main/resources/META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
```

Its complete contents are:

```text
dev.krpc.plugin.DemoRegistrar
```

This is Java `ServiceLoader` registration. Without it, Kotlin can load the
JAR but cannot discover the registrar.

## 7. Write a minimal IR transformation

Create `compiler-plugin/src/main/kotlin/dev/krpc/plugin/ReplaceBodyExtension.kt`:

```kotlin
package dev.krpc.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.statements.IrStatement
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class ReplaceBodyExtension : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        moduleFragment.transform(object : IrElementTransformerVoid() {
            override fun visitSimpleFunction(
                declaration: IrSimpleFunction
            ): IrStatement {
                super.visitSimpleFunction(declaration)

                val isDemoFunction =
                    declaration.name.asString() == "irDemo" &&
                        declaration.parameters.isEmpty() &&
                        declaration.returnType == pluginContext.irBuiltIns.stringType

                if (isDemoFunction) {
                    declaration.body = DeclarationIrBuilder(
                        pluginContext,
                        declaration.symbol
                    ).irBlockBody {
                        +irReturn(irString("produced by the IR plugin"))
                    }
                }

                return declaration
            }
        }, null)
    }
}
```

The transformation walks the module's IR and finds a no-argument `String`
function named `irDemo`. It replaces the body with the IR equivalent of:

```kotlin
return "produced by the IR plugin"
```

The Kotlin source is never changed; the compiled class file behaves
differently.

## 8. Attach the plugin to app

In `app/build.gradle.kts`, add the local plugin JAR to the existing
dependencies block:

```kotlin
dependencies {
    implementation(project(":utils"))

    add("kotlinCompilerPluginClasspath", project(":compiler-plugin"))
}
```

This direct classpath configuration is a simple local-development setup. It
applies the plugin to `app`'s Kotlin compilations without publishing an
artifact or creating a Gradle plugin.

## 9. Add a small app experiment

Create a new Kotlin file in `app/src/main/kotlin`, separate from the existing
DSL experiments:

```kotlin
package demo

fun irDemo(): String = "written in source"
```

Create `app/src/test/kotlin/demo/IrDemoTest.kt`:

```kotlin
package demo

import kotlin.test.Test
import kotlin.test.assertEquals

class IrDemoTest {
    @Test
    fun `IR plugin replaces the function body`() {
        assertEquals("produced by the IR plugin", irDemo())
    }
}
```

Add this normal test dependency to `app/build.gradle.kts`:

```kotlin
testImplementation(kotlin("test"))
```

Run the narrow test task from the repository root:

```sh
./gradlew :app:test --rerun-tasks
```

The test should pass even though the source body returns
`"written in source"`.

Use `./gradlew :app:compileKotlin` when only compilation needs checking. The
current app application configuration names a main class that does not exist,
so this tutorial does not rely on `:app:run`.

## 10. Verify IR while learning

Temporarily add this to `app/build.gradle.kts`:

```kotlin
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xverify-ir")
    }
}
```

Then run the test command again. IR is not type-checked after a plugin changes
it, and `-Xverify-ir` catches many malformed trees early. See
[Kotlin's verification guidance](https://kotlinlang.org/docs/custom-compiler-plugins.html).

## 11. Improve the selection rule with an annotation

Matching a function by name is deliberately minimal. Keep the annotation in
the code compiled by the plugin (the `app` module for this example), and give
it a package so the plugin can match a stable fully qualified name:

```kotlin
package demo

@Target(AnnotationTarget.FUNCTION)
annotation class ReplaceWithPlugin

@ReplaceWithPlugin
fun markedIrDemo(): String = "written in source"

fun irDemo(): String = "not marked"
```

Replace the name-based predicate in `ReplaceBodyExtension` with this check:

```kotlin
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

private companion object {
    val REPLACE_WITH_PLUGIN_ANNOTATION = FqName("demo.ReplaceWithPlugin")
}

val isMarkedForReplacement =
    declaration.hasAnnotation(REPLACE_WITH_PLUGIN_ANNOTATION)
val isSupportedFunction =
    declaration.parameters.isEmpty() &&
        declaration.returnType == pluginContext.irBuiltIns.stringType

if (isMarkedForReplacement && isSupportedFunction) {
    declaration.body = DeclarationIrBuilder(
        pluginContext,
        declaration.symbol
    ).irBlockBody {
        +irReturn(irString("produced by the IR plugin"))
    }
}
```

`hasAnnotation` matches the resolved annotation class by its fully qualified
name, so this does not depend on a function name or on the annotation's
spelling at the use site. The supported signature check is retained because
the generated body returns a `String` and does not supply arguments.

Update the test to exercise both paths:

```kotlin
package demo

import kotlin.test.Test
import kotlin.test.assertEquals

class IrDemoTest {
    @Test
    fun `IR plugin replaces only functions marked with the annotation`() {
        assertEquals("produced by the IR plugin", markedIrDemo())
        assertEquals("not marked", irDemo())
    }
}
```

For a real library, place the annotation in a small `plugin-annotations`
module shared by consumers and the plugin. Use that module's package-qualified
name in `REPLACE_WITH_PLUGIN_ANNOTATION`.

## 12. Add a Gradle plugin only when distributing

The direct `kotlinCompilerPluginClasspath` approach is sufficient here. When
other projects should use:

```kotlin
plugins {
    id("dev.krpc.ir-demo")
}
```

add a `gradle-plugin` module that implements
`KotlinCompilerPluginSupportPlugin`. It supplies the compiler-plugin artifact
and options to the Kotlin Gradle Plugin. See the
[Kotlin Gradle API reference](https://kotlinlang.org/api/kotlin-gradle-plugin/kotlin-gradle-plugin-api/org.jetbrains.kotlin.gradle.plugin/-kotlin-compiler-plugin-support-plugin/).

## Key limitations

- Keep the compiler plugin version aligned with Kotlin.
- Treat IR as a typed compiler-internal tree, not a source-code AST.
- Use focused predicates and the correct function scope and return target.
- Test generated behaviour; compiler console output is poor verification.
- Use FIR as well when source compilation or the IDE must know about generated
  declarations.
