package de.winniepat.gamblingPlugin;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigDefaultsTest {

    @Test
    void configContainsRequiredSections() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            List<String> required = Arrays.asList(
                    "slots:",
                    "roulette:",
                    "cases:",
                    "double:",
                    "blackjack:",
                    "mines:",
                    "crash:",
                    "coinflip:",
                    "wheel:",
                    "stations:",
                    "daily-reward:",
                    "updates:",
                    "audio:",
                    "particles:"
            );

            for (String key : required) {
                assertTrue(config.contains(key), "config.yml is missing section: " + key);
            }
        }
    }

    @Test
    void configContainsNewGameDefaults() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // blackjack payout tiers
            assertTrue(config.contains("blackjack:"));
            assertTrue(config.contains("2.5"));
            // mines and crash house edges
            assertTrue(config.contains("house-edge"));
            // wheel segments with weights
            assertTrue(config.contains("multiplier: 5.0"));
            assertTrue(config.contains("weight:"));
            // legacy roulette station must stay mapped to the structure block
            assertTrue(config.contains("STRUCTURE_BLOCK"));
        }
    }

    @Test
    void configContainsSlotMultipliers() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(config.contains("DIAMOND:"));
            assertTrue(config.contains("REDSTONE:"));
        }
    }
}