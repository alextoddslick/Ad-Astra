package earth.terrarium.adastra.common.utils;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class KeybindManager {

    private static final Map<UUID, KeybindManager> PLAYER_KEYS = new HashMap<>();

    private boolean jumpDown;
    private boolean sprintDown;
    private boolean suitFlightEnabled;
    // Rising-edge flag — true on the server tick when jump transitions from
    // released → pressed. Lets the jet suit apply a one-shot impulse per tap
    // so press/release/press feels like distinct "pulses" instead of single
    // ticks of normal thrust (which is too small to feel between coast frames).
    private boolean jumpPressedThisTick;

    public KeybindManager() {
    }

    public KeybindManager(boolean jumpDown, boolean sprintDown, boolean suitFlightEnabled) {
        this.jumpDown = jumpDown;
        this.sprintDown = sprintDown;
        this.suitFlightEnabled = suitFlightEnabled;
    }

    public static boolean jumpDown(Player player) {
        return jumpDown(player.getUUID());
    }

    public static boolean jumpDown(UUID player) {
        return PLAYER_KEYS.getOrDefault(player, new KeybindManager()).jumpDown;
    }

    public static boolean jumpPressedThisTick(Player player) {
        return PLAYER_KEYS.getOrDefault(player.getUUID(), new KeybindManager()).jumpPressedThisTick;
    }

    public static boolean sprintDown(Player player) {
        return sprintDown(player.getUUID());
    }

    public static boolean sprintDown(UUID player) {
        return PLAYER_KEYS.getOrDefault(player, new KeybindManager()).sprintDown;
    }

    public static boolean suitFlightEnabled(Player player) {
        return suitFlightEnabled(player.getUUID());
    }

    public static boolean suitFlightEnabled(UUID player) {
        return PLAYER_KEYS.getOrDefault(player, new KeybindManager()).suitFlightEnabled;
    }

    public static void set(Player player, boolean jumpDown, boolean sprintDown, boolean suitFlightEnabled) {
        set(player.getUUID(), jumpDown, sprintDown, suitFlightEnabled);
    }

    public static void set(UUID player, boolean jumpDown, boolean sprintDown, boolean suitFlightEnabled) {
        KeybindManager prev = PLAYER_KEYS.get(player);
        boolean wasJumping = prev != null && prev.jumpDown;
        KeybindManager mgr = new KeybindManager(jumpDown, sprintDown, suitFlightEnabled);
        mgr.jumpPressedThisTick = jumpDown && !wasJumping;
        PLAYER_KEYS.put(player, mgr);
    }

    public static boolean hasAnyKeyDown(Player player) {
        KeybindManager keys = PLAYER_KEYS.get(player.getUUID());
        return keys != null && (keys.jumpDown || keys.sprintDown || keys.suitFlightEnabled);
    }
}