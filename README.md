# Magic System (Fabric 1.21.8)

A configurable, data‑driven magic framework: mana, cooldowns, HUD, and a growing library of spells. Designed to work great on its own and integrates cleanly with the YellSpells voice mod.

## Requirements

- Minecraft 1.21.8
- Fabric Loader ≥ 0.17.2
- Fabric API ≥ 0.131.0
- Java 21

## Highlights

- Data‑driven spells loaded from `config/magicsystem_spells.json`
- Server‑side mana and cooldown enforcement
- HUD with compact “pip” mana display above the health bar
- Built‑in spells: Fireball, Safe Descent (slow falling)
- Compatible with YellSpells for voice commands

## How it works (overview)

- Server loads global settings from `config/magicsystem.json` and spells from `config/magicsystem_spells.json`.
- All spell casting and mana checks happen on the server (`/cast <id>`).
- Client HUD shows mana pips and selected effect timers.

## Mana system

- Max mana = `baseMana + (playerLevel * manaPerLevel)`
- Regeneration: every `manaRegenerationInterval` ticks, restore `manaRegenerationRate` mana
- Values are server‑configurable (see “Configuration”)

## Mana HUD

- 10 mana per pip; number of pips scales with max mana (rounded up)
- Pips are rendered directly above the vanilla health row
- Color: blue fill with subtle border; compact and unobtrusive
- Effect timers: selected effects (e.g., Safe Descent) show time remaining (seconds) in the top‑right

## Spells (built‑ins)

- Fireball (`fireball`)
  - Type: projectile (large fireball visuals, block‑safe)
  - Defaults: `manaCost: 20`, `cooldown: 2000ms`, `damage: 16.0`, `explosionPower: 0`, `velocity: 1.0`, `range: 64`
  - Cast sound: `minecraft:entity.firework_rocket.launch`

- Safe Descent (`safedescent`)
  - Type: status effect (applies Slow Falling)
  - Defaults: `manaCost: 50`, `cooldown: 40000ms`, `effects: [{ id: minecraft:slow_falling, duration: 600, amplifier: 0 }]`
  - Cast sound: `minecraft:entity.evoker.cast_spell`

## Commands

- `/cast <spellId>` — casts a spell by id (server‑side validation and consumption)

## Configuration

### Global settings — `config/magicsystem.json`

- `baseMana` (default 100)
- `manaPerLevel` (default 10)
- `manaRegenerationRate` (default 2)
- `manaRegenerationInterval` ticks (default 20)
- `enableSpellCooldowns` (default true)
- `enableManaCosts` (default true)
- `globalSpellDamageMultiplier` (default 1.0)

This file is created automatically on first run and can be edited then reloaded by restarting the server/game.

### Spells — `config/magicsystem_spells.json`

Created automatically on first run with safe defaults. Example:

```json
{
  "fireball": {
    "type": "projectile",
    "name": "Fireball",
    "manaCost": 20,
    "cooldown": 2000,
    "projectile": { "variant": "fireball", "velocity": 1.0, "explosionPower": 0, "startOffset": 1.2 },
    "damage": 16.0,
    "directHitRadius": 0.5,
    "areaDamageRadius": 2.5,
    "knockbackStrength": 2.0,
    "castSound": "minecraft:entity.firework_rocket.launch"
  },
  "safedescent": {
    "type": "status_effect",
    "name": "Safe Descent",
    "manaCost": 50,
    "cooldown": 40000,
    "castSound": "minecraft:entity.evoker.cast_spell",
    "castParticleCount": 24,
    "effects": [
      { "id": "minecraft:slow_falling", "duration": 600, "amplifier": 0 }
    ]
  }
}
```

Supported spell families today:

- `status_effect`: applies one or more vanilla status effects to the caster
  - keys: `effects[]` (`id`, `duration`, `amplifier`), optional `castSound`, `castParticleCount`
- `projectile`: fires a vanilla projectile with tuned parameters
  - keys: `projectile.variant` (`fireball`|`small_fireball`), `projectile.velocity`, `projectile.explosionPower`, `projectile.startOffset`, optional `castSound`
  - damage AOE: `damage`, `directHitRadius`, `areaDamageRadius`, `knockbackStrength`

Add new spells by adding entries to `magicsystem_spells.json`. Restart to apply.

## YellSpells integration (optional)

- YellSpells can map voice keywords to `/cast <spellId>`
- Its config (`yellspells.json`) supports multiple keywords, per‑spell cooldowns, and a reload command `/yellspells reload`

## Building from source

```bash
./gradlew build
```

### Project structure (key files)

```
src/main/java/com/magicsystem/
├── MagicSystemMod.java                  # Main mod class, events
├── config/
│   └── MagicSystemConfig.java           # Global server settings
├── mana/
│   ├── ManaManager.java                 # Mana/cooldown server logic
│   └── ManaData.java
├── spells/
│   ├── Spell.java                       # Base spell abstraction
│   ├── SpellManager.java                # Registry + casting entrypoint
│   ├── core/
│   │   ├── StatusEffectSpell.java       # Generic status-effect spell
│   │   └── ProjectileSpell.java         # Generic projectile spell
│   └── registry/
│       └── SpellRegistry.java           # Loads config/magicsystem_spells.json
├── commands/
│   └── CastCommand.java                 # `/cast <id>`
└── client/
    └── hud/
        └── ManaHUD.java                 # Mana pips + effect timers
```

## Troubleshooting

- Fireball explodes blocks: ensure `explosionPower` is `0` in `magicsystem_spells.json`
- Mana doesn’t regenerate: check `manaRegenerationRate` and `manaRegenerationInterval`
- HUD not visible: ensure HUD is enabled (F1 hides it) and you’re on the client

## License

MIT License (see `LICENSE`).

## Contributing

Issues and PRs are welcome. Please include Minecraft/Fabric versions and reproduction steps.
