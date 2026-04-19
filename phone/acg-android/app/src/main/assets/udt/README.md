# Android UDT assets

JSON files loaded at app start by `UdtRegistry(ctx)`. Every `*.template.json`
declares a type; every other `*.json` that carries `{udtType, instance, parameters}`
is an instance of one of those types.

## Type map (current)

| udtType                  | what it describes                                         |
|--------------------------|-----------------------------------------------------------|
| `AndroidModule`          | A Kotlin class living for the app lifetime (TagDb etc.)   |
| `AndroidApi`             | An HTTP endpoint the app reads                            |
| `AndroidActuator`        | A thing the phone can DO (torch, vibrate, notify)         |
| `AndroidScreen`          | One tab in the bottom nav                                 |
| `AndroidMqttTopic`       | A declared Mqtt topic pattern + who pub/sub on it         |
| `AndroidTagEventScript`  | Binds an Mqtt topic (+ optional `when` expr) to an action |
| `AndroidAtom.*`          | UI primitive (StatRow, ActionButton, StatusLine, Chip…)   |
| `AndroidMolecule.*`      | Composed UI (StatCard, ActionRow, SensorCard…)            |
| `AndroidOrganism.*`      | Whole-screen composites (StatScreen, PlantScreen…)        |

## Conventions

- `id` is kebab-case, namespaced with `<kind>:<slug>` — `module:tagdb`, `api:health`, `screen:home`.
- Cross-references use those `id`s (`uses`, `published_by`, `consumed_by`, `action.actuator`).
- `enabled: false` on TagEventScripts keeps them catalogued but inert — the future runtime will honour that flag.

## Adding a new instance

Drop a `<kind>-<slug>.json` (or `instance-*.json`) in this folder. Next build rolls it into the registry. Settings → UDT Catalog lists it.
