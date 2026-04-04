# WeaponsPlugin — Custom Weapons for Paper 1.20.6

A Paper 1.20.6 plugin adding **7 custom weapons** with unique active abilities,
cooldowns, particle effects, and sound feedback.

---

## Build Requirements

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.8+ |
| Paper API | 1.20.6-R0.1-SNAPSHOT |

```bash
cd weapons-plugin
mvn clean package
# Output: target/WeaponsPlugin-1.0.0.jar
```

Drop the JAR into your server's `plugins/` folder and restart.

---

## Commands

| Command | Description |
|---------|-------------|
| `/weapon list` | List all weapon IDs |
| `/weapon give <id>` | Give yourself a weapon |
| `/weapon give <id> <player>` | Give a weapon to another player |

**Permission:** `weapons.give` (OP by default)

---

## Weapon Reference

### ⚔ Greatsword — Mid-Game
- **Damage:** 20  |  **Material:** Netherite Sword
- **Ability: Reflective Guard** — Right-click to activate a 5-second reflect shield.
  Any hit received while the shield is active reflects **80% of the damage** back to
  the attacker. Player still takes the full hit.
- **Cooldown:** 30 seconds
- **ID:** `greatsword`

---

### ⚒ Dominican Axe — Late-Game
- **Damage:** 35  |  **Material:** Netherite Axe
- **Ability: Ground Smash** — Right-click to unleash a shockwave.
  All enemies within **4.5 blocks** are **knocked back** and dealt **50 damage**.
- **Cooldown:** 10 seconds
- **ID:** `dominican_axe`

---

### ✦ Arcanist Staff — Late-Game
- **Damage:** 15 melee / 15 projectile  |  **Material:** Blaze Rod
- **Right-click:** Fire a magic bolt (15 damage).
- **Ability: Charged Shot** — Sneak + Right-click to prime the next bolt for **50 damage**.
- **Cooldown:** 60 seconds (charged shot only; regular bolts have no cooldown)
- **ID:** `arcanist_staff`

---

### ✦ Archmage's Wand — Late-Game
- **Damage:** 50  |  **Material:** End Rod
- **Ability: Arcane Burst** — Right-click to release arcane energy:
  - Knocks back **all enemies** in a 6-block radius.
  - Heals you for **30% of your maximum health**.
  - Has **3 charges**; after all 3 are used, enters a **90-second cooldown**
    before recharging to 3 again.
- **ID:** `archmages_wand`

---

### ☠ Shadowblade — Late-Game
- **Damage:** 15  |  **Material:** Iron Sword
- **Ability: Shadowstep** — Right-click to dash **5 blocks forward** (stops at walls).
  Any enemy caught in the dash path takes **10 damage** and receives
  **Bleeding** (1 damage/second for 10 seconds).
- **Cooldown:** 10 seconds
- **ID:** `shadowblade`

---

### ☠ Assassin's Blade — Late-Game
- **Damage:** 25  |  **Material:** Golden Sword
- **Ability: The Shadow** — Right-click to vanish for **5 seconds**:
  - Grants **Invisibility** and **Speed II**.
  - Mobs and players **cannot attack you** during this phase.
  - Natural regen (food, potions of Regeneration) is **disabled**.
  - **Healing potions still work.**
- **Cooldown:** 40 seconds
- **ID:** `assassins_blade`

---

### ❤ Harmony Wand — Support
- **Damage:** 0 melee  |  **Material:** Stick
- **Ability: Harmony Bolt** — Right-click to fire a harmony bolt (no cooldown).
  - **Enemy hit:** deals **5 damage × 3 pulses** (one pulse per second).
  - **Ally (player) hit:** heals **5 HP × 3 pulses** (one pulse per second).
- **ID:** `harmony_wand`

---

## Technical Notes

- All items use **PDC (PersistentDataContainer)** for identity — never display names.
- Custom damage is applied via `EntityDamageByEntityEvent` override — items show correct
  damage in their lore without relying on vanilla attribute display.
- All scheduled tasks are cancelled cleanly in `onDisable()` and on player quit.
- Arrow projectiles (Arcanist Staff) are marked `DISALLOWED` for pickup.
- Snowball projectiles (Harmony Wand) despawn automatically after hitting.
