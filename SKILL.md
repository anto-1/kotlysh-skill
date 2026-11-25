---
name: kotlysh
description: Write and run Kotlysh tasks - one typed Kotlin file that drives shell, Python, Node and Ruby interpreters under coroutines. Use when a task needs several commands or languages with shared state, parallel steps, deadlines, typed data passed between languages, or a plan a person can review and rerun, instead of a chain of one-off shell commands.
---

# Kotlysh

Kotlysh is a Kotlin DSL with one construct: inside a runtime block, a string is
code, and invoking it runs it.

```kotlin
sh { "git status --short"() }
py { "print(sum(range(10)))"() }
```

Each block is a live interpreter. Kotlin decides what runs, in what order, with
which data, and what happens on failure. Use it when a job has state, more than
one language, parallel steps or a deadline. For a single one-off command, a
plain shell is quicker.

## Run a task

Write the task as one Kotlin file with no `package` line and a top-level
`main`, then run it with the launcher that ships with this skill:

```bash
scripts/kotlysh-run task.kt [arguments...]
```

- The task runs in the directory you call the launcher from. Its arguments,
  stdin, stdout and exit code are its own. An uncaught exception prints a
  stack trace and exits with status 1; call `kotlin.system.exitProcess(n)` to
  choose a status.
- The first run clones kotlysh-lib into `~/.cache/kotlysh-skill` (or
  `$KOTLYSH_HOME`) and builds it, which takes a few minutes. Later runs take about a second plus the task.
- It needs `git`, JDK 21 (found through `KOTLYSH_JAVA_HOME`, `JAVA_HOME` or
  the usual install locations) and the interpreters the task uses: `bash`,
  `python3`, `node`, `ruby`.
- A compile error is printed as a line starting `e:` that ends in
  `Task.kt:LINE:COLUMN message`, and the launcher exits with status 2.
- `scripts/kotlysh-run --update` refreshes the cached library. The full
  reference is its README: `~/.cache/kotlysh-skill/kotlysh-lib/README.md`.

## Task skeleton

```kotlin
import com.kotlysh.core.*
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Serializable
data class Stats(val words: Int, val longest: String)

fun main(args: Array<String>): Unit = runBlocking {
    withTimeout(2.minutes) {                                  // one deadline over everything inside
        val branch = async { sh { "git branch --show-current"().orThrow().stdoutText } }
        val log = async { sh { "git log --oneline -n 50"().orThrow().stdout } }

        val stats = py {
            eval<Stats>(
                "{'words': sum(len(l.split()) for l in lines), 'longest': max(lines, key=len)}",
                "lines" to value(log.await()),               // data goes in as a binding, not as text
            )
        }
        println("${branch.await()}: $stats")
    }
}
```

Declare `main` as `: Unit = runBlocking { ... }` so the block's last
expression does not become its return type.

## What you can do

| Need | Write |
| --- | --- |
| Run a command | `sh { "ls -la"() }` |
| Read the result | `.stdout`, `.stderr` (lines), `.stdoutText`, `.stderrText`, `.exitCode`, `.ok` |
| Keep state | several scriptlets in one block share directory, variables, imports |
| Fail on error | `"cmd"().orThrow()` throws `RuntimeCallException` with the stderr in its message |
| Branch on error | `if (!"git diff --quiet"().ok) { ... }` |
| Use Python or Node | `py { }`, `node { }`; `ruby { }` for scriptlets only |
| Get a typed value | `py { eval<Int>("sum(items)", "items" to value(listOf(2, 3, 5))) }` |
| Value plus its output | `evaluate<T>(...)` gives `.value` and `.execution` (a result) |
| Keep a foreign object | `hold("expr") { ref -> eval<T>("f(x)", "x" to ref) }` |
| Move data across | `py { store(data) { ref -> node { copy(ref) { copied -> ... } } } }` |
| Run in parallel | `async { sh { ... } }` once per session, then `await()` |
| Bound a call | `"cmd"(timeout = 10.minutes, idleTimeout = 2.minutes)` |
| Bound everything | `withTimeout(5.minutes) { ... }` stops every interpreter inside |
| Run without a shell | `exec(listOf("git", "status"), cwd = path)` |
| Watch output live | `"cmd"(output = { line -> println(line.text) })` |
| Trace a run | `withRuntimeTrace(RuntimeTraceBuffer()) { ... }` |

## Rules that prevent most mistakes

**Failure**
- A scriptlet never throws because its command failed. Check `.ok` or call
  `.orThrow()`, or the failure passes silently.
- To fail a scriptlet on purpose, end it with a failing command such as
  `false`, not `exit`, which ends the whole session.
- A shell pipeline reports only its last command. Run `"set -o pipefail"()`
  once and the session keeps it.
- `eval`, `evaluate`, `hold`, `store`, `read` and `copy` throw instead:
  `RuntimeCallException` when the foreign code raises (`errorType`,
  `errorMessage`), `RuntimeDecodeException` when the value does not fit the
  Kotlin type.

**Sessions**
- An ordinary failure leaves the session usable. A timeout, a cancelled
  caller, too much output or `exit` closes it, and later calls throw. Open a
  new block to retry.
- One session runs one call at a time. For parallelism use one block per
  `async`.
- The default `timeout` is 30 seconds: raise it for builds, tests and
  downloads. A scriptlet that overruns returns a result with `timedOut` and
  does not throw. `withTimeout` throws `TimeoutCancellationException`.
- These are ordinary coroutines: an `async` that throws cancels its siblings
  and the scope around them.
- Shell commands get an empty stdin, and a scriptlet must `wait` for its own
  background jobs.
- A dispatch keeps at most 10,000 lines of output. Filter in the scriptlet, or
  write to a file and pass the path.

**Strings**

| To send | In `"..."` | In `"""..."""` |
| --- | --- | --- |
| `$HOME` | `\$HOME` | `${'$'}HOME` |
| `${VAR}` | `\${VAR}` | `${'$'}{VAR}` |
| `$?`, `$1`, `$(cmd)` | as written | as written |
| a double quote | `\"` | as written |
| `\n`, `\d` | `\\n`, `\\d` | as written |

- Prefer `"""..."""` for anything with quotes, backslashes or several lines.
  Common indentation is removed, so Python blocks keep their shape.
- A bare `$name` or `${...}` is Kotlin, and its value becomes source code.
- Never splice data into source. In `py` and `node` pass a binding. In shell
  use `${shLiteral(value)}`, outside any quotes. `pythonLiteral` and
  `jsLiteral` do the same for those languages.
- Use `'''` for Python docstrings inside a triple-quoted scriptlet.

**Typed values**
- An expression is one expression, sent as written. Put statements in a
  scriptlet first, then call what they defined. A multiline Python expression
  must not start with indentation: keep it on one line or add `.trimIndent()`.
- Only plain JSON crosses: strings, numbers, booleans, null, lists, and
  string-keyed maps. Convert the rest in its own language first: `int(x)`,
  `float(x)`, `x.tolist()`, `list(a_set)`, `dict(counter)`, `d.toISOString()`.
  NumPy and pandas values and JavaScript `undefined` do not cross.
- Your own result types need `@Serializable`.
- A reference works only inside its block and its own session.

**Node**
- A scriptlet has no top-level `await`: wrap asynchronous work as
  `(async () => { ... })()`, which is awaited. Expressions may use `await`.
- Top-level `const` and `let` persist, so declaring the same name twice fails.
  Assign, or use `var`.
