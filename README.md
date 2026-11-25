# Kotlysh skill

An agent skill for [Kotlysh](https://github.com/anto-1/kotlysh-lib), the
LLM-centric polyglot DSL: one typed Kotlin file drives shell, Python, Node and
Ruby interpreters under coroutines.

The skill teaches an agent to write Kotlysh tasks and gives it a launcher that
runs one from a single file, with no project to set up.

## Install

For an agent that loads skills, clone it where the agent looks for them. For
Claude Code:

```bash
git clone https://github.com/anto-1/kotlysh-skill ~/.claude/skills/kotlysh
```

For an agent that reads `AGENTS.md`, such as Codex, clone it anywhere and add
a pointer to it:

```bash
git clone https://github.com/anto-1/kotlysh-skill ~/kotlysh-skill
~/kotlysh-skill/scripts/install-agents-md                    # global: ~/.codex/AGENTS.md
~/kotlysh-skill/scripts/install-agents-md path/to/AGENTS.md  # or one project only
```

The pointer is a short marked block that tells the agent when to use Kotlysh
and where `SKILL.md` and the launcher are. Running the script again refreshes
it, and `--remove` takes it out.

## Use

Ask for work that spans commands or languages, or run a task yourself:

```bash
scripts/kotlysh-run examples/hello.kt /usr
```

The first run clones and builds kotlysh-lib into `~/.cache/kotlysh-skill`,
which takes a few minutes. It needs `git`, JDK 21 and the interpreters the
task uses.

## Contents

| Path | Purpose |
| --- | --- |
| `SKILL.md` | what the agent reads: how to write a task, and the rules that prevent most mistakes |
| `scripts/kotlysh-run` | builds one task file against kotlysh-lib and runs it |
| `scripts/install-agents-md` | adds the pointer to an `AGENTS.md` file |
| `AGENTS.md` | sends an agent working in this checkout to `SKILL.md` |
| `runner/` | the small Gradle project the launcher builds with |
| `examples/hello.kt` | a first task |

## License

Apache License 2.0. See [LICENSE](LICENSE).
