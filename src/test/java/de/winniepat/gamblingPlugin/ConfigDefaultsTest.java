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
    void configContainsSlotMultipliers() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
            String config = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(config.contains("DIAMOND:"));
            assertTrue(config.contains("REDSTONE:"));
        }
    }
}