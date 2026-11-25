# Kotlysh skill

An agent skill for [Kotlysh](https://github.com/anto-1/kotlysh-lib), the
LLM-centric polyglot DSL: one typed Kotlin file drives shell, Python, Node and
Ruby interpreters under coroutines.

The skill teaches an agent to write Kotlysh tasks and gives it a launcher that
runs one from a single file, with no project to set up.

## Install

Clone it where your agent looks for skills. For Claude Code:

```bash
git clone https://github.com/anto-1/kotlysh-skill ~/.claude/skills/kotlysh
```

## Use

Ask for work that spans commands or languages, or run a task yourself:

```bash
~/.claude/skills/kotlysh/scripts/kotlysh-run examples/hello.kt /usr
```

The first run clones and builds kotlysh-lib into `~/.cache/kotlysh-skill`,
which takes a few minutes. It needs `git`, JDK 21 and the interpreters the
task uses.

## Contents

| Path | Purpose |
| --- | --- |
| `SKILL.md` | what the agent reads: how to write a task, and the rules that prevent most mistakes |
| `scripts/kotlysh-run` | builds one task file against kotlysh-lib and runs it |
| `runner/` | the small Gradle project the launcher builds with |
| `examples/hello.kt` | a first task |

## License

Apache License 2.0. See [LICENSE](LICENSE).
