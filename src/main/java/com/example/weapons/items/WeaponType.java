package com.example.weapons.items;

public enum WeaponType {

    GREATSWORD("greatsword",         7.0,  10.0, false),
    DOMINICAN_AXE("dominican_axe",  10.0,  13.0, false),
    ARCANIST_STAFF("arcanist_staff",  0.0,   0.0, true),
    ARCHMAGES_WAND("archmages_wand",  0.0,   0.0, true),
    SHADOWBLADE("shadowblade",        5.6,   7.0, false),
    ASSASSINS_BLADE("assassins_blade",7.2,   9.0, false),
    HARMONY_WAND("harmony_wand",      0.0,   0.0, true);

    private final String id;
    private final double normalDamage;
    private final double critDamage;
    private final boolean wand;

    WeaponType(String id, double normalDamage, double critDamage, boolean wand) {
        this.id           = id;
        this.normalDamage = normalDamage;
        this.critDamage   = critDamage;
        this.wand         = wand;
    }

    public String  getId()           { return id; }
    public double  getNormalDamage() { return normalDamage; }
    public double  getCritDamage()   { return critDamage; }
    public boolean isWand()          { return wand; }

    public static WeaponType fromId(String id) {
        if (id == null) return null;
        for (WeaponType t : values()) if (t.id.equals(id)) return t;
        return null;
    }
}
