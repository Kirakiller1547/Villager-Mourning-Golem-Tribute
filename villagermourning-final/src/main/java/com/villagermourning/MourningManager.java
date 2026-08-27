package com.villagermourning;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Hält alle laufenden "Trauer-Events" (ein toter Iron Golem + Grabstein + die Villager,
 * die dorthin laufen sollen) und aktualisiert sie einmal pro Server-Tick.
 */
public class MourningManager {

    // --- Einstellbare Werte ---
    private static final double SEARCH_RADIUS = 32.0;        // Suchradius für Villager um den toten Golem
    private static final double ARRIVAL_DISTANCE = 2.5;       // Ab wann gilt ein Villager als "angekommen"
    private static final double WALK_SPEED = 0.55;             // Laufgeschwindigkeit (Vanilla-Standard ~0.5)
    private static final int MAX_WAIT_TICKS = 15 * 20;         // Nach spätestens 15s wird der Sound trotzdem gespielt
    private static final int REPATH_INTERVAL = 20;             // Alle 20 Ticks (1s) den Laufbefehl erneuern

    // Das Lied dauert genau 1 Minute - so lange stehen die Villager auch am Grab und sind still.
    private static final int SONG_DURATION_TICKS = 60 * 20;
    private static final int EVENT_LIFETIME_AFTER_SONG = SONG_DURATION_TICKS;

    // Radius, in dem Spielern während des Liedes die Minecraft-Musik stummgeschaltet wird
    // (in etwa die Hörweite des Liedes, damit weit entfernte Spieler nicht betroffen sind).
    private static final double MUSIC_MUTE_RADIUS = 64.0;
    // Alle paar Ticks erneut senden, da sonst zwischendurch ein neuer Musik-Track anspringen könnte.
    private static final int MUSIC_MUTE_INTERVAL = 10;

    // WICHTIG: Bei world.playSound(...) bestimmt der volume-Parameter nicht nur die Lautstärke,
    // sondern auch, bis zu welcher Entfernung der Sound überhaupt an Spieler gesendet wird
    // (Formel intern: volume > 1.0 -> Hörradius = 16 * volume Blöcke, sonst fix 16 Blöcke).
    // Bei volume=1.0f wäre nach nur 16 Blöcken abrupt Schluss - das wirkte wie "wird nicht leiser".
    // Mit 4.0f ergibt sich ein Hörradius von 64 Blöcken, innerhalb dessen die Lautstärke ganz normal
    // (3D-Audio) mit der Entfernung abnimmt, bevor der Sound am Rand ausklingt.
    private static final float SONG_VOLUME = 4.0f;

    private final List<MourningEvent> activeEvents = new ArrayList<>();

    /** Wird aufgerufen, sobald ein Iron Golem stirbt. */
    public void onIronGolemDeath(ServerWorld world, BlockPos deathPos) {
        List<VillagerEntity> nearbyVillagers = world.getEntitiesByClass(
                VillagerEntity.class,
                Box.of(deathPos.toCenterPos(), SEARCH_RADIUS * 2, SEARCH_RADIUS * 2, SEARCH_RADIUS * 2),
                VillagerEntity::isAlive
        );

        // Kein Villager in der Nähe -> kein Dorf -> nichts tun (auch kein Grabstein)
        if (nearbyVillagers.isEmpty()) {
            return;
        }

        BlockPos gravePos = placeGravestone(world, deathPos);

        List<UUID> villagerIds = new ArrayList<>();
        for (VillagerEntity villager : nearbyVillagers) {
            villagerIds.add(villager.getUuid());
            sendVillagerToPosition(villager, gravePos);
        }

        activeEvents.add(new MourningEvent(world, gravePos, villagerIds));
    }

    /** Muss jeden Server-Tick aufgerufen werden. */
    public void tick() {
        Iterator<MourningEvent> iterator = activeEvents.iterator();
        while (iterator.hasNext()) {
            MourningEvent event = iterator.next();
            event.ticksElapsed++;

            boolean allArrived = true;
            int aliveVillagers = 0;

            for (UUID id : event.villagerIds) {
                VillagerEntity villager = getVillagerByUuid(event.world, id);
                if (villager == null || !villager.isAlive()) {
                    continue;
                }
                aliveVillagers++;

                if (event.songStarted) {
                    // Während des Liedes bleiben die Villager stehen und schauen weiter zum Grabstein.
                    villager.getNavigation().stop();
                    villager.getLookControl().lookAt(
                            event.gravePos.getX() + 0.5,
                            event.gravePos.getY() + 1.0,
                            event.gravePos.getZ() + 0.5
                    );
                    continue;
                }

                double distanceSq = villager.getBlockPos().getSquaredDistance(event.gravePos);
                if (distanceSq > ARRIVAL_DISTANCE * ARRIVAL_DISTANCE) {
                    allArrived = false;
                    // Brain-KI der Villager überschreibt die Navigation ständig -> regelmäßig erneut befehligen
                    if (event.ticksElapsed % REPATH_INTERVAL == 0) {
                        sendVillagerToPosition(villager, event.gravePos);
                    }
                }
            }

            boolean timedOut = event.ticksElapsed >= MAX_WAIT_TICKS;

            if (!event.songStarted && aliveVillagers > 0 && (allArrived || timedOut)) {
                startMourning(event);
                event.songStarted = true;
                event.songStartTick = event.ticksElapsed;
            }

            if (event.songStarted) {
                // Minecraft-Musik für Spieler in der Nähe regelmäßig stummschalten, solange das Lied läuft.
                if (event.ticksElapsed % MUSIC_MUTE_INTERVAL == 0) {
                    muteGameMusicNearby(event);
                }
            }

            boolean songFinished = event.songStarted && (event.ticksElapsed - event.songStartTick) >= EVENT_LIFETIME_AFTER_SONG;

            // Event beenden: kein Villager mehr übrig, oder Song ist zu Ende (1 Minute vorbei)
            boolean noVillagersLeft = aliveVillagers == 0;

            if (noVillagersLeft || songFinished) {
                endMourning(event);
                iterator.remove();
            }
        }
    }

    /**
     * Platziert einen echten Grabstein-Block (kein Steinblock mit Schild) an der Stelle,
     * an der der Golem gestorben ist, und gibt dessen Position zurück (dorthin laufen die Villager).
     */
    private BlockPos placeGravestone(ServerWorld world, BlockPos deathPos) {
        BlockPos gravePos = deathPos;
        world.setBlockState(gravePos, ModBlocks.GRAVESTONE.getDefaultState());
        return gravePos;
    }

    private void sendVillagerToPosition(VillagerEntity villager, BlockPos target) {
        villager.getNavigation().startMovingTo(
                target.getX() + 0.5,
                target.getY(),
                target.getZ() + 0.5,
                WALK_SPEED
        );
        // Blick zum Grabstein richten für einen glaubwürdigeren Effekt
        villager.getLookControl().lookAt(target.getX() + 0.5, target.getY() + 1.0, target.getZ() + 0.5);
    }

    /** Startet das Lied und schaltet Villager-Sounds sowie die Hintergrundmusik stumm. */
    private void startMourning(MourningEvent event) {
        event.world.playSound(
                null,
                event.gravePos,
                ModSounds.MOURNING_SONG,
                // RECORDS statt NEUTRAL: passt inhaltlich besser zu einem "Lied".
                SoundCategory.RECORDS,
                SONG_VOLUME,
                1.0f
        );

        for (UUID id : event.villagerIds) {
            VillagerEntity villager = getVillagerByUuid(event.world, id);
            if (villager != null && villager.isAlive()) {
                // Villager-eigene Geräusche (Gemurmel, Handel usw.) stummschalten, solange das Lied läuft.
                villager.setSilent(true);
                villager.getNavigation().stop();
                villager.getLookControl().lookAt(
                        event.gravePos.getX() + 0.5,
                        event.gravePos.getY() + 1.0,
                        event.gravePos.getZ() + 0.5
                );
            }
        }
    }

    /** Sobald das Lied vorbei ist: Villager-Sounds wieder freigeben, Villager dürfen sich wieder bewegen. */
    private void endMourning(MourningEvent event) {
        for (UUID id : event.villagerIds) {
            VillagerEntity villager = getVillagerByUuid(event.world, id);
            if (villager != null && villager.isAlive()) {
                villager.setSilent(false);
            }
        }
    }

    /** Schickt allen Spielern in der Nähe des Grabsteins ein "Stop-Sound"-Paket für die Musik-Kategorie. */
    private void muteGameMusicNearby(MourningEvent event) {
        Vec3d center = event.gravePos.toCenterPos();
        for (ServerPlayerEntity player : PlayerLookup.around(event.world, center, MUSIC_MUTE_RADIUS)) {
            player.networkHandler.sendPacket(new StopSoundS2CPacket(null, SoundCategory.MUSIC));
        }
    }

    private VillagerEntity getVillagerByUuid(ServerWorld world, UUID id) {
        var entity = world.getEntity(id);
        if (entity instanceof VillagerEntity villager) {
            return villager;
        }
        return null;
    }

    /** Interner Datencontainer für ein einzelnes Trauer-Event. */
    private static class MourningEvent {
        final ServerWorld world;
        final BlockPos gravePos;
        final List<UUID> villagerIds;
        int ticksElapsed = 0;
        boolean songStarted = false;
        int songStartTick = 0;

        MourningEvent(ServerWorld world, BlockPos gravePos, List<UUID> villagerIds) {
            this.world = world;
            this.gravePos = gravePos;
            this.villagerIds = villagerIds;
        }
    }
}
