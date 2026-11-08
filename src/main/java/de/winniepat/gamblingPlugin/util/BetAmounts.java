package de.winniepat.gamblingPlugin.util;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class BetAmounts {

    public static final List<Double> DEFAULT = List.of(10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0);
    public static final int MAX_AMOUNT_COUNT = 35;

    private BetAmounts() {
    }

    public static List<Double> load(FileConfiguration config, Consumer<String> logger) {
        return loadValues(config.getList("betting.amounts"), logger);
    }

    public static List<Double> loadValues(List<?> configured, Consumer<String> logger) {
        if (configured == null || configured.isEmpty()) {
            logger.accept("Invalid or empty betting.amounts configuration; using default wager amounts " + DEFAULT + ".");
            return DEFAULT;
        }
        if (configured.size() > MAX_AMOUNT_COUNT) {
            logger.accept("betting.amounts contains " + configured.size() + " values, but the betting GUI supports at most "
                    + MAX_AMOUNT_COUNT + "; using default wager amounts " + DEFAULT + ".");
            return DEFAULT;
        }

        List<Double> amounts = new ArrayList<>(configured.size());
        for (int i = 0; i < configured.size(); i++) {
            Object value = configured.get(i);
            if (!(value instanceof Number)) {
                logger.accept("Invalid betting.amounts[" + i + "] value '" + value
                        + "'; every wager must be a positive finite number. Using default wager amounts " + DEFAULT + ".");
                return DEFAULT;
            }
            double amount = ((Number) value).doubleValue();
            if (!Double.isFinite(amount) || amount <= 0.0) {
                logger.accept("Invalid betting.amounts[" + i + "] value '" + value
                        + "'; every wager must be a positive finite number. Using default wager amounts " + DEFAULT + ".");
                return DEFAULT;
            }
            amounts.add(amount);
        }
        return List.copyOf(amounts);
    }
}
