package de.winniepat.gamblingPlugin.util;

import org.bukkit.Particle;

public final class Particles {

    private Particles() {
    }

    public static Particle byName(String name, String... fallbacks) {
        Particle particle = valueOf(name);
        if (particle != null) {
            return particle;
        }
        for (String fallback : fallbacks) {
            particle = valueOf(fallback);
            if (particle != null) {
                return particle;
            }
        }
        return null;
    }

    private static Particle valueOf(String name) {
        try {
            return Particle.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
