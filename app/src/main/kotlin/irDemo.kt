package demo

@Target(AnnotationTarget.FUNCTION)
annotation class ReplaceWithPlugin

@ReplaceWithPlugin
fun markedIrDemo(): String = "written in source"

fun irDemo(): String = "not marked"
