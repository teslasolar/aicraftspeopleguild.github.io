# /docs — engineering + API documentation

<div align="center"><img src="../guild/Enterprise/L2/hmi/web/assets/svg/architecture.svg" alt="ACG ISA-95 architecture pyramid — the spine that everything under /docs documents" width="1040"/></div>

> 📚 **The reference material.** Prose documentation of the control plane drawn in the pyramid above. This dir is for humans reading specs; the live HMIs and the API endpoints are under [`guild/Enterprise/`](../guild/Enterprise/).
>
> Breadcrumb: [`/`](../) · `docs/`

## Layout

```
docs/
├── engineering/           technical specs, standards, and architecture
│   ├── architecture/      ISA-95 layer descriptions + context
│   ├── standards/         naming, tag conventions, glyph vocabulary
│   ├── component-catalog/ every UDT / Tool / SvgOrganism in prose
│   ├── tech-spec/         detailed sub-system specs
│   └── urs/               User Requirements Specs
├── api/                   API reference (complements /guild/Enterprise/L4/api/)
├── templates/             doc stubs — drop in, fill out, commit
└── path.json              dir manifest for the rollup
```

## Engineering docs cover

- **ISA-95 layering** — why L0/L1/L2/L3/L4, and what code lives at each layer
- **Tag conventions** — the `scada.*`, `enterprise.*`, `demo.*` namespaces and their rules
- **UDT vocabulary** — `Tool`, `Script`, `SvgOrganism`, `ApiEndpoint`, `MirrorPair`, `WhitepaperApp`
- **State machine semantics** — `state_machine.py` transitions, `state_db.py` write path
- **SPOT patrol rules** — which beats, what they check, fault-routing conventions (see also [`guild/Enterprise/L2/scada/spot/README.md`](../guild/Enterprise/L2/scada/spot/README.md))

## See also

- [`guild/Enterprise/`](../guild/Enterprise/) — the implementation that these docs describe
- [`guild/Enterprise/L4/api/`](../guild/Enterprise/L4/api/) — live JSON endpoints (machine-readable side of what `docs/api/` describes in prose)
- [`bin/acg`](../bin/acg) — the CLI those specs get exercised through
