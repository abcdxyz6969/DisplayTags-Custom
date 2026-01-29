// src/main/java/me/itsskeptical/displaytags/entities/ClientDisplay.java
package me.itsskeptical.displaytags.entities;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public abstract class ClientDisplay {

    protected final int entityId;
    protected final EntityType type;
    protected final Location location;

    protected Vector3f translation = new Vector3f(0f, 0f, 0f);
    protected Vector3f scale = new Vector3f(1f, 1f, 1f);

    protected ClientDisplay(EntityType type, Location location) {
        this.entityId = ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE);
        this.type = type;
        this.location = location.clone();
    }

    public List<EntityData<?>> getEntityData() {
        return new ArrayList<>();
    }

    public void setTranslation(Vector3f translation) {
        this.translation = translation;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    public void spawn(Player player) {
        WrapperPlayServerSpawnEntity spawnPacket =
                new WrapperPlayServerSpawnEntity(
                        entityId,
                        Optional.empty(),
                        type,
                        new Vector3d(location.getX(), location.getY(), location.getZ()),
                        location.getPitch(),
                        location.getYaw(),
                        0f,
                        0,
                        Optional.empty()
                );

        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacket(player, spawnPacket);

        sendMetadata(player);
    }

    protected void sendMetadata(Player player) {
        List<EntityData<?>> data = new ArrayList<>(getEntityData());

        data.add(new EntityData<>(
                11,
                EntityDataTypes.VECTOR3F,
                translation
        ));

        data.add(new EntityData<>(
                12,
                EntityDataTypes.VECTOR3F,
                scale
        ));

        WrapperPlayServerEntityMetadata metadataPacket =
                new WrapperPlayServerEntityMetadata(entityId, data);

        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacket(player, metadataPacket);
    }

    public void despawn(Player player) {
        WrapperPlayServerDestroyEntities destroyPacket =
                new WrapperPlayServerDestroyEntities(entityId);

        PacketEvents.getAPI()
                .getPlayerManager()
                .sendPacket(player, destroyPacket);
    }

    public int getEntityId() {
        return entityId;
    }
}
