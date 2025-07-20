# PotionStackerPlus
This is a configurable [Purpur 1.21.7] plugin that lets you control potion stack behavior.


✅ Features:
Custom Stack Size: Stack any potion up to your configured amount (stack-size in config).

Potion Type Filters: Choose which types (POTION, SPLASH_POTION, LINGERING_POTION) are affected.

Effect Filtering (optional): Only allow stacking if potion effects match your allowed list.

Live Config Reload: Run /potionstacker reload to reload without restarting.

Admin Commands:

/potionstacker setstack <size>

/potionstacker addeffect <effect>

/potionstacker removeeffect <effect>

Tab Completion for all subcommands.

Permissions:

```
potionstacker.use
```

🔧 Config Example: --(generated automaticlly)--

```
yaml
# Default maximum stack size for enabled potions
stack-size: 16

# Enable potion stacking only for these vanilla potion types
enabled-potions:
- POTION
- SPLASH_POTION
- LINGERING_POTION

# Enable checking custom potion effects (disabled by default)
use-custom-effects: false

# If custom effects are enabled, only allow stacking of potions with these effects
allowed-effects:
- SPEED
- REGENERATION
- STRENGTH
```
