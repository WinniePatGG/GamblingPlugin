package de.winniepat.gamblingPlugin.util;

import de.winniepat.gamblingPlugin.GamblingPlugin;
import de.winniepat.gamblingPlugin.gui.BetSelectionGUI;
import de.winniepat.gamblingPlugin.gui.MenuGUI;
import de.winniepat.gamblingPlugin.gui.WheelGUI;
import de.winniepat.gamblingPlugin.gui.game.BlackjackGame;
import de.winniepat.gamblingPlugin.gui.game.CoinflipGame;
import de.winniepat.gamblingPlugin.gui.game.CrashGame;
import de.winniepat.gamblingPlugin.gui.game.MinesGame;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MusicManager {

    public enum Context {
        NONE,
        MENU,
        GAME
    }

    private MusicManager() {
    }

    private static final Set<String> MENU_TITLES = new HashSet<>();

    private static final Set<String> GAME_TITLES = new HashSet<>();

    private static final Set<String> GAME_TITLE_PREFIXES = new HashSet<>();

    static {
        MENU_TITLES.add(MenuGUI.TITLE);
        MENU_TITLES.add(BetSelectionGUI.TITLE);

        GAME_TITLES.add(BlackjackGame.TITLE);
        GAME_TITLES.add(MinesGame.TITLE);
        GAME_TITLES.add(CrashGame.TITLE);
        GAME_TITLES.add(CoinflipGame.TITLE);
        GAME_TITLES.add(WheelGUI.TITLE);
        GAME_TITLES.add("§6Slot Machine");
        GAME_TITLES.add("§aDouble or Nothing");
        GAME_TITLES.add("Choose a Color to Bet");
        GAME_TITLES.add("§bOpening Case...");
        GAME_TITLE_PREFIXES.add("Roulette: ");
    }

    private static final Map<String, Double> VANILLA_LENGTHS = new HashMap<>();

    static {
        VANILLA_LENGTHS.put("MUSIC_DISC_13", 178.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_CAT", 185.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_BLOCKS", 345.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_CHIRP", 185.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_FAR", 174.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_MALL", 197.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_MELLOHI", 96.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_STAL", 150.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_STRAD", 185.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_WARD", 251.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_11", 71.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_WAIT", 237.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_PIGSTEP", 148.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_OTHERSIDE", 195.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_5", 191.0);
        VANILLA_LENGTHS.put("MUSIC_DISC_RELICS", 218.0);
    }

    private static final class Session {
        Context context = Context.NONE;
        int generation;
    }

    private static final class Track {
        final Sound sound;
        final String key;
        final String tableKey;

        Track(Sound sound, String key, String tableKey) {
            this.sound = sound;
            this.key = key;
            this.tableKey = tableKey;
        }
    }

    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    private static final Set<String> BAD_TRACKS_LOGGED = new HashSet<>();

    public static void onOpenView(Player player, String title) {
        Context context = contextForTitle(title);
        if (context == Context.NONE && ActiveGames.active(player.getUniqueId()) != null) {
            context = Context.GAME;
        }
        setContext(player, context);
    }

    public static void reconcile(Player player) {
        if (GamblingPlugin.getInstance() == null) return;
        if (!player.isOnline()) {
            forget(player.getUniqueId());
            return;
        }
        if (ActiveGames.active(player.getUniqueId()) != null) {
            setContext(player, Context.GAME);
            return;
        }
        setContext(player, contextForTitle(ViewTitles.title(player.getOpenInventory())));
    }

    private static Context contextForTitle(String title) {
        if (MENU_TITLES.contains(title)) return Context.MENU;
        if (GAME_TITLES.contains(title)) return Context.GAME;
        for (String prefix : GAME_TITLE_PREFIXES) {
            if (title.startsWith(prefix)) return Context.GAME;
        }
        return Context.NONE;
    }

    private static void setContext(Player player, Context context) {
        if (!featureEnabled()) {
            context = Context.NONE;
        }
        Session session = SESSIONS.computeIfAbsent(player.getUniqueId(), key -> new Session());
        if (session.context == context) {
            return;
        }
        restart(player, session, context);
    }

    private static void restart(Player player, Session session, Context context) {
        session.generation++;
        session.context = context;
        stopMusic(player);
        if (context == Context.NONE || !featureEnabled() || isMuted(player)) {
            return;
        }
        startLoop(player, session, context);
    }

    public static boolean setMuted(Player player, boolean muted) {
        PlayerData data = playerData();
        if (data != null) {
            data.setMusicMuted(player.getUniqueId(), muted);
        }
        Session session = SESSIONS.get(player.getUniqueId());
        if (session != null) {
            restart(player, session, session.context);
        }
        return isMuted(player);
    }

    public static boolean isMuted(Player player) {
        PlayerData data = playerData();
        return data != null && data.isMusicMuted(player.getUniqueId());
    }

    private static PlayerData playerData() {
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        return plugin == null ? null : plugin.getPlayerData();
    }

    public static void forget(UUID uuid) {
        Session session = SESSIONS.remove(uuid);
        if (session != null) {
            session.generation++;
        }
    }

    public static void clearAll() {
        for (Session session : SESSIONS.values()) {
            session.generation++;
        }
        SESSIONS.clear();
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        if (plugin != null && plugin.isEnabled()) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                stopMusic(player);
            }
        }
    }

    public static void handleConfigReload() {
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        if (plugin == null) return;
        for (UUID uuid : new ArrayList<>(SESSIONS.keySet())) {
            Session session = SESSIONS.get(uuid);
            if (session == null || session.context == Context.NONE) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                forget(uuid);
                continue;
            }
            Context context = session.context;
            Scheduler.runForPlayer(plugin, player, 1L, () -> {
                Session current = SESSIONS.get(uuid);
                if (current != null && current.context == context) {
                    restart(player, current, context);
                } else {
                    reconcile(player);
                }
            });
        }
    }

    private static void startLoop(Player player, Session session, Context context) {
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        if (plugin == null) return;

        Track track = trackFor(context);
        if (track == null) return;

        long period = Math.max(60L, (long) Math.ceil((trackLength(context, track) + 1.5) * 20.0));
        final int generation = session.generation;

        Runnable loop = new Runnable() {
            @Override
            public void run() {
                GamblingPlugin active = GamblingPlugin.getInstance();
                if (active == null || !active.isEnabled()) return;
                if (session.generation != generation) return;
                if (session.context != context) return;
                if (!player.isOnline()) {
                    forget(player.getUniqueId());
                    return;
                }

                stopMusic(player);
                play(player, track);
                Scheduler.runForPlayer(active, player, period, this);
            }
        };
        Scheduler.runForPlayer(plugin, player, 3L, loop);
    }

    private static void play(Player player, Track track) {
        try {
            if (track.sound != null) {
                player.playSound(player, track.sound, SoundCategory.MUSIC, volume(), 1.0f);
            } else {
                player.playSound(player, track.key, SoundCategory.MUSIC, volume(), 1.0f);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void stopMusic(Player player) {
        try {
            player.stopSound(SoundCategory.MUSIC);
        } catch (Throwable ignored) {
        }
    }

    private static boolean featureEnabled() {
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        return plugin != null && plugin.getConfig().getBoolean("audio.music.enabled", true);
    }

    private static float volume() {
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        double value = plugin == null ? 1.0 : plugin.getConfig().getDouble("audio.music.volume", 1.0);
        return (float) Math.max(0.0, Math.min(1.0, value));
    }

    private static Track trackFor(Context context) {
        String path = context == Context.GAME ? "audio.music.game.sound" : "audio.music.menu.sound";
        String fallback = context == Context.GAME ? "MUSIC_DISC_PIGSTEP" : "MUSIC_DISC_FAR";
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        String raw = plugin == null ? fallback : plugin.getConfig().getString(path, fallback);
        return resolveTrack(path, raw);
    }

    private static Track resolveTrack(String path, String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        if (raw.isEmpty()) return null;

        String enumCandidate = raw;
        int namespace = raw.indexOf(':');
        if (namespace >= 0) {
            enumCandidate = raw.substring(namespace + 1);
        }
        String enumName = enumCandidate.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        try {
            return new Track(Sound.valueOf(enumName), null, enumName);
        } catch (IllegalArgumentException ignored) {
        }

        if (namespace < 0) {
            warnUnknownTrack(path, raw);
            return null;
        }
        return new Track(null, raw.toLowerCase(Locale.ROOT), null);
    }

    private static void warnUnknownTrack(String path, String raw) {
        if (!BAD_TRACKS_LOGGED.add(path + ":" + raw)) return;
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        if (plugin != null) {
            plugin.getLogger().warning(path + " is set to '" + raw
                    + "', which is not a known sound. Use a vanilla sound name like MUSIC_DISC_FAR,"
                    + " or a namespaced resource-pack key like 'mypack:my_song'. Music for this context is disabled.");
        }
    }

    private static double trackLength(Context context, Track track) {
        GamblingPlugin plugin = GamblingPlugin.getInstance();
        if (plugin == null) return 240.0;
        String path = context == Context.GAME
                ? "audio.music.game.length-seconds"
                : "audio.music.menu.length-seconds";
        double configured = plugin.getConfig().getDouble(path, 0.0);
        if (configured > 0) {
            return configured;
        }
        if (track.tableKey != null) {
            Double known = VANILLA_LENGTHS.get(track.tableKey);
            if (known != null) {
                return known;
            }
        }
        return 240.0;
    }
}
