package custom.scriptDefinition

import org.jetbrains.kotlin.script.*
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class TestDependenciesResolver : ScriptDependenciesResolver {
    override fun resolve(
            script: ScriptContents,
            environment: Map<String, Any?>?,
            report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit, previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?> {
        return CompletableFuture.completedFuture(
                object : KotlinScriptExternalDependencies {
                    override val javaHome: String? = null
                    override val classpath: Iterable<File> = listOf(environment?.get("kotlin-runtime") as File)
                    override val imports: Iterable<String> = listOf()
                })

    }
}

@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template