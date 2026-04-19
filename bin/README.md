# /bin — the `acg` CLI dispatcher

<div align="center"><img src="../guild/Enterprise/L2/hmi/web/assets/svg/cmd-panel.svg" alt="ACG cmd panel · the browser-side twin of this CLI — every button here has an acg subcommand equivalent" width="1016"/></div>

> ⌨ **The command line.** `bin/acg` is a Python-stdlib-only dispatcher over the [Tool UDT registry](../guild/Enterprise/L3/tools/instances/). Every `.json` Tool instance becomes a subcommand automatically — no command list to maintain. The browser [cmd-panel](../guild/Enterprise/L2/hmi/web/assets/svg/cmd-panel.svg) above calls the same tools via GitHub Issues.
>
> Breadcrumb: [`/`](../) · `bin/`

## Files

| file | role |
|---|---|
| [`acg`](acg) | POSIX shell wrapper — Python dispatcher. 60+ Tool UDTs auto-registered. |
| [`acg.cmd`](acg.cmd) | Windows CMD wrapper for the same dispatcher. |
| [`udt-catalog.py`](udt-catalog.py) | Walks `guild/Enterprise/L3/udts/` and emits one JSON blob per discovered instance — shell-friendly + seeds the mobile apps' local UDT registry. |
| [`path.json`](path.json) | Dir manifest for the tag/dir rollup. |

## Usage

```bash
acg                         # list every registered tool
acg <tool_id>               # run a tool with no inputs
acg <tool_id> k=v k2=v2     # inputs as key=value pairs
acg --test <tool_id>        # run a tool's declared test cases
acg --mcp                   # emit MCP tool manifest for external agents
```

## Frequent tools

| tool | what it does |
|---|---|
| `pipeline:run id=build` | 14-step tag-driven full rebuild |
| `build:svg-all` | regenerate every [`SvgOrganism`](../guild/Enterprise/L4/svg/instances/organisms/) |
| `build:svg:spot-patrol` | one SPOT sweep — writes SVG, JSON log, and `state.db` faults |
| `build:svg:fork-compare` | mine-vs-origin side-by-side card |
| `build:tag-dbs` | rebuild `tag.db` from the UDT + tag file tree |
| `build:api` · `build:state` · `build:runtime-tags` | refresh L4 endpoints |
| `gh-tag:write path=<p> value=<v> type=Counter` | write a tag (requires `gh` CLI or `GITHUB_TOKEN`) |
| `state:fire tag=<t> to_state=CHANGED` | fire a state transition through `state_machine.py` |
| `udt:list` · `udt:read id=<tool_id>` | inspect the registry |

Every invocation writes a JSONL audit row to `guild/Enterprise/L2/logs/` via the logger wrapper, so the full CLI history is introspectable from the state DB + `state.json`.

## See also

- [`guild/Enterprise/L3/tools/instances/`](../guild/Enterprise/L3/tools/instances/) — every Tool UDT that becomes a subcommand
- [`guild/Enterprise/L3/udts/`](../guild/Enterprise/L3/udts/) — UDT catalog root
- [`guild/Enterprise/L2/logs/`](../guild/Enterprise/L2/logs/) — JSONL audit trail
