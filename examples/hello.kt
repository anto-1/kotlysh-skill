import com.kotlysh.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

// Two shells run at once; Python adds up what they found.
fun main(args: Array<String>): Unit = runBlocking {
    val directory = args.firstOrNull() ?: "."
    val total = withTimeout(20.seconds) {
        val files = async { sh { "ls -1 ${shLiteral(directory)} | wc -l"().orThrow().stdoutText.trim().toInt() } }
        val hidden = async { sh { "ls -1A ${shLiteral(directory)} | grep -c '^\\.' || true"().stdoutText.trim().toInt() } }
        py { eval<Int>("sum(counts)", "counts" to value(listOf(files.await(), hidden.await()))) }
    }
    println("$total entries in $directory")
}
