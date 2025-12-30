package me.itsskeptical.displaytags.nametags;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import me.itsskeptical.displaytags.DisplayTags;
import me.itsskeptical.displaytags.entities.ClientTextDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class Nametag {

    private final DisplayTags plugin;
    private final Player player;

    private final List<ClientTextDisplay> displays = new ArrayList<>();

    // Vanilla-like spacing
    private static final double LINE_SPACING = 0.23;

    public Nametag(DisplayTags plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    /* =========================
       Getters
       ========================= */

    public Player getPlayer() {
        return player;
    }

    /* =========================
       Visibility – per player
       ========================= */

    public void show(Player viewer) {
        spawnFor(viewer, plugin.getNametagManager().getLines(player));
    }

    public void hide(Player viewer) {
        despawnFor(viewer);
    }

    /* =========================
       Visibility – all
       ========================= */

    public void showForAll() {
        spawn(plugin.getNametagManager().getLines(player));
    }

    public void hideForAll() {
        despawn();
    }

    public void updateVisibilityForAll() {
        update(plugin.getNametagManager().getLines(player));
    }

    /* =========================
       Spawn (all viewers)
       ========================= */

    public void spawn(List<Component> lines) {
        despawn();

        Location base = player.getLocation();

        double baseYOffset = plugin.getConfig()
                .getDouble("nametag.base-y-offset", 0.35);

        double baseY = player.getEyeHeight() + baseYOffset;

        float scale = (float) plugin.getConfig()
                .getDouble("nametag.scale", 0.7);

        for (int i = 0; i < lines.size(); i++) {
            ClientTextDisplay display = new ClientTextDisplay(base);

            display.setText(lines.get(i));
            display.setTextShadow(true);
            display.setSeeThrough(true);

            display.setScale(new Vector3f(scale, scale, scale));

            double yOffset =
                    baseY
                    - (i * LINE_SPACING * scale)
                    + getCustomLineOffset(i + 1);

            display.setTranslation(new Vector3f(0f, (float) yOffset, 0f));

            display.spawn(player);
            displays.add(display);
        }

        mountAll();
    }

    /* =========================
       Spawn (single viewer)
       ========================= */

    private void spawnFor(Player viewer, List<Component> lines) {
        // DisplayTags gốc không cache per-viewer display,
        // nên vẫn dùng chung entity logic
        spawn(lines);
    }

    /* =========================
       Update text
       ========================= */

    public void update(List<Component> lines) {
        if (displays.isEmpty()) {
            spawn(lines);
            return;
        }

        for (int i = 0; i < displays.size(); i++) {
            if (i >= lines.size()) break;
            displays.get(i).setText(lines.get(i));
        }
    }

    /* =========================
       Mount passengers
       ========================= */

    private void mountAll() {
        if (displays.isEmpty()) return;

        int[] passengers = displays.stream()
                .mapToInt(ClientTextDisplay::getEntityId)
                .toArray();

        WrapperPlayServerSetPassengers packet =
                new WrapperPlayServerSetPassengers(
                        player.getEntityId(),
                        passengers
                );

        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacket(player, packet);
    }

    /* =========================
       Despawn
       ========================= */

    public void despawn() {
        for (ClientTextDisplay display : displays) {
            display.despawn(player);
        }
        displays.clear();
    }

    private void despawnFor(Player viewer) {
        despawn();
    }

    /* =========================
       Config helpers
       ========================= */

    private double getCustomLineOffset(int line) {
        return plugin.getConfig().getDouble(
                "nametag.line-y-offsets." + line,
                0.0
        );
    }
}
