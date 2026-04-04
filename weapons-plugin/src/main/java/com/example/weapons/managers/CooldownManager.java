package com.example.weapons.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks per-player, per-ability cooldowns using wall-clock timestamps.
 * Thread-safe reads/writes are not required here since all ability code
 * runs on the main server thread.
 */
public final class CooldownManager {

    // Map<PlayerUUID, Map<abilityKey, expiresAt (ms)>>
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    /**
     * Returns true if the player is currently on cooldown for the given ability.
     */
    public boolean isOnCooldown(UUID playerId, String abilityKey) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return false;
        Long expires = playerMap.get(abilityKey);
        return expires != null && System.currentTimeMillis() < expires;
    }

    /**
     * Returns the remaining cooldown in milliseconds, or 0 if not on cooldown.
     */
    public long getRemainingMs(UUID playerId, String abilityKey) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap == null) return 0L;
        Long expires = playerMap.get(abilityKey);
        if (expires == null) return 0L;
        return Math.max(0L, expires - System.currentTimeMillis());
    }

    /** Convenience: returns remaining seconds as a whole number (ceiling). */
    public long getRemainingSeconds(UUID playerId, String abilityKey) {
        return (long) Math.ceil(getRemainingMs(playerId, abilityKey) / 1000.0);
    }

    /**
     * Sets a cooldown for the given player and ability key.
     * @param durationMs duration in milliseconds
     */
    public void setCooldown(UUID playerId, String abilityKey, long durationMs) {
        cooldowns
            .computeIfAbsent(playerId, k -> new HashMap<>())
            .put(abilityKey, System.currentTimeMillis() + durationMs);
    }

    /** Immediately clears a specific cooldown (e.g. admin override). */
    public void clearCooldown(UUID playerId, String abilityKey) {
        Map<String, Long> playerMap = cooldowns.get(playerId);
        if (playerMap != null) playerMap.remove(abilityKey);
    }

    /** Clears all cooldowns for a player (e.g. on quit to free memory). */
    public void clearAll(UUID playerId) {
        cooldowns.remove(playerId);
    }
}
