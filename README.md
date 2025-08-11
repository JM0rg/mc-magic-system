# Magic System Mod

A comprehensive magic system for Minecraft 1.21.8 on Fabric that provides spells, mana management, and cooldowns. This mod is designed to work with the YellSpells mod for voice-activated spellcasting.

## Features

- **Mana System**: Players have a mana pool that regenerates over time
- **Spell System**: Configurable spells with mana costs and cooldowns
- **Command Integration**: Cast spells using `/cast <spellname>` commands
- **Visual HUD**: Mana bar displayed on the client
- **Extensible Design**: Easy to add new spells and modify existing ones

## Requirements

- Minecraft 1.21.8
- Fabric Loader 0.17.2+
- Fabric API 0.131.0+
- Java 21+

## Installation

1. Install Fabric Loader for Minecraft 1.21.8
2. Download and install Fabric API
3. Download this mod and place it in your mods folder
4. Start Minecraft and configure the mod

## Configuration

The mod creates a configuration file at `config/magicsystem.json` with the following settings:

### Mana Settings
- `baseMana`: Base mana pool (default: 100)
- `manaPerLevel`: Additional mana per experience level (default: 10)
- `manaRegenerationRate`: Mana regeneration per second (default: 1)
- `manaRegenerationInterval`: Ticks between regeneration (default: 20)

### Spell Settings
- `enableSpellCooldowns`: Enable spell cooldowns (default: true)
- `enableManaCosts`: Enable mana costs for spells (default: true)
- `globalSpellDamageMultiplier`: Global damage multiplier for spells (default: 1.0)

## Usage

### Commands
- `/cast fireball` - Cast a fireball spell

### Spells

#### Fireball
- **Mana Cost**: 25
- **Cooldown**: 3 seconds
- **Damage**: 8.0 hearts
- **Range**: 64 blocks
- **Description**: Launches a fireball projectile in the direction you're looking

## Integration with YellSpells

This mod is designed to work with the YellSpells mod. When YellSpells detects a voice command, it can execute the corresponding `/cast` command to trigger spells from this mod.

Example integration:
- Player says "fireball" → YellSpells executes `/cast fireball` → Magic System casts the fireball spell

## Development

### Building from Source

```bash
./gradlew build
```

### Project Structure

```
src/main/java/com/magicsystem/
├── MagicSystemMod.java              # Main mod class
├── config/
│   └── MagicSystemConfig.java       # Configuration system
├── mana/
│   ├── ManaManager.java             # Mana management
│   └── ManaData.java                # Player mana data
├── spells/
│   ├── SpellManager.java            # Spell management
│   ├── Spell.java                   # Base spell class
│   └── FireballSpell.java           # Fireball implementation
├── commands/
│   └── CastCommand.java             # Cast command
└── client/
    └── hud/
        └── ManaHUD.java             # Mana display HUD
```

### Adding New Spells

1. Create a new spell class extending `Spell`:
```java
public class LightningSpell extends Spell {
    public LightningSpell() {
        super("lightning", "Lightning", 50, 5000, 12.0f, 32);
    }
    
    @Override
    public boolean cast(ServerPlayerEntity player) {
        // Implementation here
        return true;
    }
}
```

2. Register the spell in `SpellManager.registerSpells()`:
```java
spells.put("lightning", new LightningSpell());
```

3. Add the command to your voice system or use `/cast lightning`

### Spell Properties

Each spell has the following configurable properties:
- **id**: Unique identifier for the spell
- **name**: Display name
- **manaCost**: Mana required to cast
- **cooldown**: Cooldown in milliseconds
- **damage**: Base damage amount
- **range**: Maximum range

## Performance

- Minimal impact on game performance
- Efficient mana regeneration system
- Optimized spell casting with proper validation

## Troubleshooting

### Common Issues

1. **Spells not casting**: Check mana requirements and cooldowns
2. **Mana not regenerating**: Verify configuration settings
3. **HUD not showing**: Ensure you're on the client side

### Logs

Check the Minecraft logs for detailed error information. Look for entries from "MagicSystem".

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Acknowledgments

- [Fabric](https://fabricmc.net/) for the modding framework
- [YellSpells](https://github.com/your-repo/yell-spells) for voice integration inspiration
