package me.itsskeptical.displaytags.nametags;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import me.clip.placeholderapi.PlaceholderAPI;
import me.itsskeptical.displaytags.DisplayTags;
import me.itsskeptical.displaytags.config.NametagConfig;
import me.itsskeptical.displaytags.entities.ClientTextDisplay;
import me.itsskeptical.displaytags.utils.ComponentUtils;
import me.itsskeptical.displaytags.utils.handlers.NametagHandler;
import me.itsskeptical.displaytags.utils.helpers.DependencyHelper;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Custom nametag implementation using TEXT_DISPLAY entities.
 * Each configured line is rendered as a separate Text Display so backgrounds are separated per-line.
 */
public class Nametag {

    private final DisplayTags plugin;
    private final Player player;
    private final List<ClientTextDisplay> displays;
    private final Set<UUID> viewers;

    private final List<String> lines;
    private final boolean hideSelf;
    private final int visibilityDistance;

    // Vanilla-like spacing. This is applied in addition to the passenger mount position.
    private static final float BASE_Y = 0.25f;
    private static final float LINE_SPACING = 0.23f;

    public Nametag(Player player) {
        this.plugin = DisplayTags.getInstance();
        this.player = player;
        this.viewers = new HashSet<>();

        NametagConfig config = plugin.config().getNametagConfig();
        this.lines = config.getLines();
        this.hideSelf = config.shouldHideSelf();
        this.visibilityDistance = config.getVisibilityDistance();

        // Build one TextDisplay per configured line (at least one)
        int lineCount = Math.max(1, this.lines.size());
        this.displays = new ArrayList<>(lineCount);

        Location baseLoc = player.getLocation().setRotation(0, 0);
        Vector scale = config.getScale();
        float spacing = LINE_SPACING * (float) scale.getY();
        float startY = BASE_Y + ((lineCount - 1) * spacing / 2.0f);

        for (int i = 0; i < lineCount; i++) {
            ClientTextDisplay display = new ClientTextDisplay(baseLoc);

            display.setTranslation(new Vector3f(0, startY - (i * spacing), 0));
            display.setScale(scale);
            display.setTextShadow(config.hasTextShadow());
            display.setTextAlignment(config.getTextAlignment());
            display.setSeeThrough(config.isSeeThrough());
            display.setBillboard(config.getBillboard());
            display.setBackground(getBackground());

            this.displays.add(display);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public void showForAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.show(viewer);
        }
    }

    public void hideForAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            this.hide(viewer);
        }
    }

    public void updateVisibilityForAll() {
        Location baseLoc = player.getLocation().setRotation(0, 0);
        for (ClientTextDisplay display : displays) {
            display.setLocation(baseLoc);
        }

        viewers.removeIf((uuid) -> {
            Player viewer = Bukkit.getPlayer(uuid);
            return viewer == null || !viewer.isOnline();
        });

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean shouldSee = shouldSee(viewer);
            boolean isSeeing = this.viewers.contains(viewer.getUniqueId());

            if (shouldSee && !isSeeing) {
                this.show(viewer);
            } else if (!shouldSee && isSeeing) {
                this.hide(viewer);
            } else if (shouldSee) {
                this.update(viewer);
            }
        }
    }

    private boolean shouldSee(Player viewer) {
        if (viewer == null || !viewer.isOnline() || viewer.isDead()) return false;
        if (hideSelf && player.getUniqueId().equals(viewer.getUniqueId())) return false;
        if (player.isDead() || player.getGameMode().equals(GameMode.SPECTATOR)) return false;
        if (!viewer.getWorld().getName().equals(player.getWorld().getName())) return false;
        if (player.isInvisible() || !viewer.canSee(player)) return false;

        return viewer.getLocation().distanceSquared(player.getLocation()) < visibilityDistance * visibilityDistance;
    }

    public void show(Player viewer) {
        NametagHandler.hide(player, viewer);
        if (hideSelf && player.getUniqueId().equals(viewer.getUniqueId())) return;

        this.viewers.add(viewer.getUniqueId());
        for (ClientTextDisplay display : displays) {
            display.spawn(viewer);
        }
        this.update(viewer);
    }

    public void hide(Player viewer) {
        this.viewers.remove(viewer.getUniqueId());
        for (ClientTextDisplay display : displays) {
            display.despawn(viewer);
        }
    }

    public void update(Player viewer) {
        Location baseLoc = player.getLocation();
        for (ClientTextDisplay display : displays) {
            display.setLocation(baseLoc);
        }

        List<Component> textLines = getTextLines();
        int count = Math.min(textLines.size(), displays.size());
        for (int i = 0; i < count; i++) {
            displays.get(i).setText(textLines.get(i));
        }

        // Mount all displays in a single passengers packet so they don't overwrite each other.
        int[] passengerIds = displays.stream().mapToInt(ClientTextDisplay::getEntityId).toArray();
        WrapperPlayServerSetPassengers passengersPacket = new WrapperPlayServerSetPassengers(
                this.player.getEntityId(),
                passengerIds
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, passengersPacket);

        for (ClientTextDisplay display : displays) {
            display.update(viewer);
        }
    }

    private List<Component> getTextLines() {
        List<Component> components = new ArrayList<>(Math.max(1, lines.size()));
        DecimalFormat healthFormat = new DecimalFormat("#.##");

        for (String line : lines) {
            String modified = line
                    .replace("{player}", player.getName())
                    .replace("{health}", String.valueOf(healthFormat.format(player.getHealth())));

            if (DependencyHelper.isPlaceholderAPIEnabled()) {
                modified = PlaceholderAPI.setPlaceholders(player, modified);
            }

            components.add(ComponentUtils.format(modified));
        }

        if (components.isEmpty()) components.add(Component.empty());
        return components;
    }

    private int getBackground() {
        String background = plugin.config().getNametagConfig().getBackground();
        if (background == null || background.equalsIgnoreCase("default")) return -1;
        if (background.equalsIgnoreCase("transparent")) return 0;
        if (background.startsWith("#")) background = background.substring(1);

        Color color = Color.fromARGB((int) Long.parseLong(background, 16));
        if (background.length() == 6) color = color.setAlpha(255);
        return color.asARGB();
    }
}
