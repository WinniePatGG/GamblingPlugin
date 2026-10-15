package de.winniepat.gamblingPlugin.util;

import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Method;

public final class ViewTitles {

    private ViewTitles() {
    }

    public static String title(InventoryView view) {
        if (view == null) return "";
        try {
            Method titleMethod = view.getClass().getMethod("title");
            Object component = titleMethod.invoke(view);

            Class<?> componentClass = Class.forName("net.kyori.adventure.text.Component");
            Class<?> serializerClass = Class.forName("net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer");
            Object serializer = serializerClass.getMethod("legacySection").invoke(null);
            Method serialize = serializerClass.getMethod("serialize", componentClass);

            Object result = serialize.invoke(serializer, component);
            if (result != null) return result.toString();
        } catch (Throwable ignored) {
        }
        try {
            return view.getTitle();
        } catch (Throwable ignored) {
            return "";
        }
    }
}