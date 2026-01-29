// src/main/java/me/itsskeptical/displaytags/nametags/NametagManager.java
package me.itsskeptical.displaytags.nametags;

import me.itsskeptical.displaytags.DisplayTags;
import me.itsskeptical.displaytags.utils.handlers.NametagHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NametagManager {

    private final DisplayTags plugin;
    private final Map<UUID, Nametag> nametags;

    public NametagManager(DisplayTags plugin) {
        this.plugin = plugin;
        this.nametags = new ConcurrentHashMap<>();
    }

    public Nametag get(Player player) {
        return this.nametags.get(player.getUniqueId());
    }

    public Collection<Nametag> getAll() {
        return this.nametags.values();
    }

    public void create(Player player) {
        Nametag nametag = new Nametag(plugin, player);

        // If you already have your own line-building logic elsewhere, call it here.
        // Replace this with your existing logic if needed.
        List<Component> lines = buildLines(player);

        nametag.spawn(lines);
        this.nametags.put(player.getUniqueId(), nametag);
    }

    public void remove(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            NametagHandler.show(player, viewer);
        }

        Nametag nametag = nametags.get(player.getUniqueId());
        if (nametag != null) {
            nametag.hideForAll();
            nametags.remove(player.getUniqueId());
        }
    }

    public void createAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.create(player);
        }
    }

    public void removeAll() {
        for (Nametag nametag : nametags.values()) {
            this.remove(nametag.getPlayer());
        }
    }

    // TODO: Replace with your existing formatting/placeholder logic.
    private List<Component> buildLines(Player player) {
        return List.of(Component.text(player.getName()));
    }
}
