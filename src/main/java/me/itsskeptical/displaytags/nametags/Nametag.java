// src/main/java/me/itsskeptical/displaytags/nametags/Nametag.java
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

    private static final double LINE_SPACING = 0.23;

    public Nametag(DisplayTags plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    public void show(Player viewer) {
        // no-op (NametagManager controls spawn/update)
    }

    public void hide(Player viewer) {
        // no-op (NametagManager controls spawn/update)
    }

    public void showForAll() {
        // no-op (NametagManager calls spawn(lines))
    }

    public void hideForAll() {
        despawn();
    }

    public void updateVisibilityForAll() {
        // no-op (NametagManager calls update(lines))
    }

    public void spawn(List<Component> lines) {
        despawn();

        Location base = player.getLocation();

        double baseYOffset = plugin.getConfig().getDouble("nametag.base-y-offset", 0.35);
        double baseY = player.getEyeHeight() + baseYOffset;

        float scale = (float) plugin.getConfig().getDouble("nametag.scale", 0.7);

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

    public void update(List<Component> lines) {
        if (displays.isEmpty()) {
            spawn(lines);
            return;
        }

        int n = Math.min(displays.size(), lines.size());
        for (int i = 0; i < n; i++) {
            displays.get(i).setText(lines.get(i));
        }
    }

    private void mountAll() {
        if (displays.isEmpty()) return;

        int[] passengers = displays.stream()
                .mapToInt(ClientTextDisplay::getEntityId)
                .toArray();

        WrapperPlayServerSetPassengers packet =
                new WrapperPlayServerSetPassengers(player.getEntityId(), passengers);

        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacket(player, packet);
    }

    public void despawn() {
        for (ClientTextDisplay display : displays) {
            display.despawn(player);
        }
        displays.clear();
    }

    private double getCustomLineOffset(int line) {
        return plugin.getConfig().getDouble("nametag.line-y-offsets." + line, 0.0);
    }
}
