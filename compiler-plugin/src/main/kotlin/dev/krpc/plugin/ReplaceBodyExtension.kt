package dev.krpc.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName

class ReplaceBodyExtension(val messages: MessageCollector) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        moduleFragment.transform(object: IrElementTransformerVoid() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                super.visitSimpleFunction(declaration)

                val isMarkedForReplacement =
                    declaration.hasAnnotation(REPLACE_WITH_PLUGIN_ANNOTATION)

                if(isMarkedForReplacement) {
                    messages.report(CompilerMessageSeverity.INFO, "Annotations: ${declaration.getAnnotation(REPLACE_WITH_PLUGIN_ANNOTATION)?.getName() ?: "anonymus"}")
                }
                val isSupportedFunction =
                    declaration.parameters.isEmpty() &&
                        declaration.returnType == pluginContext.irBuiltIns.stringType

                if (isMarkedForReplacement && isSupportedFunction) {
                    declaration.body = DeclarationIrBuilder(
                        pluginContext, declaration.symbol
                    ).irBlockBody {
                        +irReturn(irString("produced by the IR plugin"))
                    }
                }

                return declaration
            }
        }, null)
    }

    private companion object {
        val REPLACE_WITH_PLUGIN_ANNOTATION = FqName("demo.ReplaceWithPlugin")
    }
}



@OptIn(UnsafeDuringIrConstructionAPI::class)
fun IrAnnotation.getName() = this.symbol.owner.parentAsClass.name.asString()
