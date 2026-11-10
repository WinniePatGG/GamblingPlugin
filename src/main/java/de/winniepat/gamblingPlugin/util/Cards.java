package de.winniepat.gamblingPlugin.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public final class Cards {

    private static final int CARD_MODEL_BASE = 1200;
    private static final String[] RANKS = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
    private static final String[] SUITS = {"\u2660", "\u2665", "\u2666", "\u2663"};

    private Cards() {
    }

    public static String draw(Random random) {
        return RANKS[random.nextInt(RANKS.length)] + SUITS[random.nextInt(SUITS.length)];
    }

    public static int value(List<String> hand) {
        int total = 0;
        int aces = 0;
        for (String card : hand) {
            String rank = card.substring(0, card.length() - 1);
            if (rank.equals("A")) {
                total += 11;
                aces++;
            } else if (rank.equals("K") || rank.equals("Q") || rank.equals("J") || rank.equals("10")) {
                total += 10;
            } else {
                total += Integer.parseInt(rank);
            }
        }
        while (aces > 0 && total > 21) {
            total -= 10;
            aces--;
        }
        return total;
    }

    public static boolean isSoft(List<String> hand) {
        int best = value(hand);
        int withAcesDown = 0;
        for (String card : hand) {
            String rank = card.substring(0, card.length() - 1);
            if (rank.equals("A")) {
                withAcesDown++;
            } else if (rank.equals("K") || rank.equals("Q") || rank.equals("J") || rank.equals("10")) {
                withAcesDown += 10;
            } else {
                withAcesDown += Integer.parseInt(rank);
            }
        }
        return best != withAcesDown;
    }

    public static boolean isBlackjack(List<String> hand) {
        return hand.size() == 2 && value(hand) == 21;
    }

    public static ItemStack item(String card) {
        char suit = card.charAt(card.length() - 1);
        String color = (suit == '\u2665' || suit == '\u2666') ? "§c" : "§f";
        return CasinoItems.model(Material.PAPER, modelData(card), color + card);
    }

    public static ItemStack faceDown() {
        return CasinoItems.model(Material.PAPER, CARD_MODEL_BASE + 53, "§7Face-down card");
    }

    private static int modelData(String card) {
        String rank = card.substring(0, card.length() - 1);
        char suit = card.charAt(card.length() - 1);
        int rankIndex = switch (rank) {
            case "A" -> 0;
            case "J" -> 10;
            case "Q" -> 11;
            case "K" -> 12;
            default -> Integer.parseInt(rank) - 1;
        };
        int suitIndex = switch (suit) {
            case '\u2665' -> 1;
            case '\u2666' -> 2;
            case '\u2663' -> 3;
            default -> 0;
        };
        return CARD_MODEL_BASE + (suitIndex * 13) + rankIndex;
    }
}
