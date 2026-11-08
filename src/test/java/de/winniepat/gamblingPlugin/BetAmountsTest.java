package de.winniepat.gamblingPlugin;

import de.winniepat.gamblingPlugin.util.BetAmounts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetAmountsTest {

    @Test
    void acceptsPositiveFiniteNumbers() {
        List<String> messages = new ArrayList<>();

        assertEquals(List.of(1.0, 25.5, 1000.0),
                BetAmounts.loadValues(List.of(1, 25.5, 1000L), messages::add));
        assertTrue(messages.isEmpty());
    }

    @Test
    void fallsBackForInvalidValues() {
        List<String> messages = new ArrayList<>();

        assertEquals(BetAmounts.DEFAULT,
                BetAmounts.loadValues(List.of(10.0, 0.0, 50.0), messages::add));
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("positive finite"));
    }

    @Test
    void fallsBackForEmptyValues() {
        List<String> messages = new ArrayList<>();

        assertEquals(BetAmounts.DEFAULT, BetAmounts.loadValues(List.of(), messages::add));
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).contains("empty"));
    }
}
