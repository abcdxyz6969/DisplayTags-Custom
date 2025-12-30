package me.itsskeptical.displaytags.entities;

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import me.yourpackage.displaytags.entity.ClientTextDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class Nametag {

    private final Player player;
    private final List<ClientTextDisplay> displays = new ArrayList<>();

    // === Vanilla-like tuning ===
    private static final float SCALE = 0.7f;
    private static final double BASE_Y_OFFSET = 0.35;
    private static final double LINE_SPACING = 0.23;

    public Nametag(Player player) {
        this.player = player;
    }

    /* =========================
       Spawn
       ========================= */
    public void spawn(List<Component> lines) {
        despawn();

        Location base = player.getLocation().clone();
        double baseY = player.getEyeHeight() + BASE_Y_OFFSET;

        for (int i = 0; i < lines.size(); i++) {
            ClientTextDisplay display = new ClientTextDisplay(base);

            display.setText(lines.get(i));
            display.setBillboard(Display.Billboard.CENTER);
            display.setShadow(true);
            display.setSeeThrough(true);
            display.setScale(new Vector3f(SCALE, SCALE, SCALE));

            double yOffset = baseY - (i * LINE_SPACING * SCALE);
            display.setTranslation(new Vector3f(0f, (float) yOffset, 0f));

            display.spawn(player);
            displays.add(display);
        }

        mountAll();
    }

    /* =========================
       Update text only
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
       Mount all displays
       ========================= */
    private void mountAll() {
        if (displays.isEmpty()) return;

        int[] passengers = displays.stream()
                .mapToInt(ClientTextDisplay::getEntityId)
                .toArray();

        WrapperPlayServerSetPassengers packet =
                new WrapperPlayServerSetPassengers(player.getEntityId(), passengers);

        packet.sendPacket(player);
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
}
