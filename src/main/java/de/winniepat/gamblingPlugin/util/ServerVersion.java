package de.winniepat.gamblingPlugin.util;

import org.bukkit.Bukkit;

public final class ServerVersion {

    public static final String MIN_SUPPORTED = "1.21.4";
    public static final String MAX_SUPPORTED = "26.1.2";

    private ServerVersion() {
    }

    public static String current() {
        return Bukkit.getMinecraftVersion();
    }

    public static boolean isSupported() {
        return isSupported(current());
    }

    public static boolean isSupported(String version) {
        int[] v = parse(version);
        return compare(v, parse(MIN_SUPPORTED)) >= 0 && compare(v, parse(MAX_SUPPORTED)) <= 0;
    }

    private static int compare(int[] a, int[] b) {
        for (int i = 0; i < 3; i++) {
            int diff = Integer.compare(a[i], b[i]);
            if (diff != 0) return diff;
        }
        return 0;
    }

    private static int[] parse(String version) {
        int[] parts = new int[3];
        String[] split = version.split("\\.");
        for (int i = 0; i < split.length && i < 3; i++) {
            try {
                parts[i] = Integer.parseInt(split[i]);
            } catch (NumberFormatException ignored) {
            }
        }
        return parts;
    }
}
