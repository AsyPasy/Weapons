package com.example.weapons.items;

/**
 * Enum registry for every custom weapon.
 * id  — stored in PDC to identify the item (never use display name for identity).
 * meleeDamage — damage applied by WeaponCombatListener on left-click attacks.
 */
public enum WeaponType {

    // ── Mid-game ──────────────────────────────────────────────────────────────
    GREATSWORD("greatsword", 20.0),

    // ── Late-game ─────────────────────────────────────────────────────────────
    DOMINICAN_AXE("dominican_axe", 35.0),
    ARCANIST_STAFF("arcanist_staff", 15.0),
    ARCHMAGES_WAND("archmages_wand", 50.0),
    SHADOWBLADE("shadowblade", 15.0),
    ASSASSINS_BLADE("assassins_blade", 25.0),

    // ── Support ───────────────────────────────────────────────────────────────
    HARMONY_WAND("harmony_wand", 0.0);      // purely projectile — no melee damage

    private final String id;
    private final double meleeDamage;

    WeaponType(String id, double meleeDamage) {
        this.id = id;
        this.meleeDamage = meleeDamage;
    }

    public String getId() {
        return id;
    }

    public double getMeleeDamage() {
        return meleeDamage;
    }

    /** Returns null if the id doesn't match any weapon. */
    public static WeaponType fromId(String id) {
        if (id == null) return null;
        for (WeaponType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }
}
