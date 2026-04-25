# BetterRTP

---

## GUI

![GUI Overview](https://i.imgur.com/fJX53dB.png)

---

## Features

- Teleport in **Overworld, Nether und End** per GUI
-  Konfigurierbarer **Cooldown** pro Spieler
- **Location-Pool** im Hintergrund — kein Lag beim Teleportieren
- Konfigurierbarer Sound & Partikel-Effekt
- Biom- und Block-Blacklist
- Benutzerdefiniertes GUI via **Resource Pack** (`better_rtp:rtp_gui`)
---

## Commands

| Befehl        | Beschreibung              | Permission    |
|---------------|---------------------------|---------------|
| `/rtp`        | Öffnet das Dimensions-GUI | `rtp.use`     |
| `/rtp reload` | Konfiguration neu laden   | `rtp.admin`   |

---

## Permissions

| Node                    | Standard | Beschreibung                  |
|-------------------------|----------|-------------------------------|
| `rtp.use`               | `true`   | Erlaubt `/rtp`                |
| `rtp.bypass.cooldown`   | `false`  | Cooldown überspringen         |
| `rtp.admin`             | `op`     | Erlaubt `/rtp reload`         |

---

## Pool Size Rechnung

Die optimale Pool-Größe für den Location-Cache berechnet sich wie folgt:

```
Pool Size = (Spieler / 100) × 5
```

---

## Architektur

```
│
├── command/
│   ├── RtpCommand          — /rtp Command-Handler
│   └── RtpTabCompleter     — Tab-Completion für "reload"
│
├── config/
│   └── RtpConfig           — YAML-Parsing, typsichere Getter
│
├── cooldown/
│   └── CooldownManager     — UUID-basiertes Cooldown-Tracking
│
├── gui/
│   ├── DimensionGui        — Baut das 27-Slot Inventory
│   └── GuiClickListener    — Verarbeitet Klicks und routet zur Teleporter-Logik
│
├── teleport/
│   ├── LocationFinder      — Async sichere Standortsuche (Overworld/Nether/End)
│   ├── LocationCache       — Hintergrund-Pool vorberechneter Positionen
│   └── Teleporter          — Führt den Teleport aus inkl. Effekten und Cooldown-Start
│
└── util/
    └── Messages            — MiniMessage-Wrapper (parse, send, actionbar, sound)
```

---

## Maybe Features

- [ ] `/rtp queue` — Battle-Match: Zwei Spieler in der Queue werden im Umkreis von 100 Blöcken teleportiert
- [ ] Last TP Log — Zeigt die letzten 5 Teleport-Koordinaten an