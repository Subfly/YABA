# Deferred (not implemented in this pass)

Per the Darwin SwiftData parity plan:

- **Managers / queue / state machines** — will call into this layer in a later step.
- **View models / UI** — continue using legacy `YabaSchemaV1` types until the app is rewired to `YabaComposeParityModelContainer` and the new models.
- **Compile fixes** across `Darwin/YABA` after switching the injected `ModelContext` are explicitly deferred.
- **Repository “shims”** — optional; can be introduced when integrating managers if helpful.
