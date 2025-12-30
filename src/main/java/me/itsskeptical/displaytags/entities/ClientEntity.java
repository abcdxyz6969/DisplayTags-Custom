package me.itsskeptical.displaytags.entities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateEntityMetadata;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class ClientEntity {

    protected final int entityId;
    protected final Location location;
    protected final EntityType type;
    protected final List<EntityData> metadata = new ArrayList<>();

    protected ClientEntity(EntityType type, Location location) {
        this.entityId = ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE);
        this.type = type;
        this.location = location.clone();
    }

    /* =========================
       Spawn
       ========================= */
    public void spawn(Player player) {
        WrapperPlayServerSpawnEntity spawn = new WrapperPlayServerSpawnEntity(
                entityId,
                type,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );

        spawn.sendPacket(player);
        sendMetadata(player);
    }

    /* =========================
       Metadata
       ========================= */
    protected void sendMetadata(Player player) {
        WrapperPlayServerUpdateEntityMetadata packet =
                new WrapperPlayServerUpdateEntityMetadata(entityId, metadata);

        packet.sendPacket(player);
    }

    protected void setMetadata(int index, EntityDataTypes type, Object value) {
        metadata.removeIf(data -> data.getIndex() == index);
        metadata.add(new EntityData(index, type, value));
    }

    /* =========================
       Despawn
       ========================= */
    public void despawn(Player player) {
        WrapperPlayServerDestroyEntities packet =
                new WrapperPlayServerDestroyEntities(entityId);

        packet.sendPacket(player);
    }

    /* =========================
       Getter (QUAN TRỌNG)
       ========================= */
    public int getEntityId() {
        return entityId;
    }
}
