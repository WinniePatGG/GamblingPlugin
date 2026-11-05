package de.winniepat.gamblingPlugin.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

public final class ActiveGames {

    private static final Map<UUID, String> ACTIVE = new HashMap<>();

    private ActiveGames() {
    }

    public static String active(UUID uuid) {
        return ACTIVE.get(uuid);
    }

    public static boolean busyWithOther(UUID uuid, String gameKey) {
        String current = ACTIVE.get(uuid);
        return current != null && !current.equals(gameKey);
    }

    public static void join(UUID uuid, String gameKey) {
        ACTIVE.put(uuid, gameKey);
    }

    public static void leave(UUID uuid) {
        ACTIVE.remove(uuid);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static Set<UUID> players() {
        return new HashSet<>(ACTIVE.keySet());
    }
}
