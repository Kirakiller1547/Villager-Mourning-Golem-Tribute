package com.villagermourning;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerMourningMod implements ModInitializer {

    public static final String MOD_ID = "villagermourning";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private final MourningManager mourningManager = new MourningManager();

    @Override
    public void onInitialize() {
        LOGGER.info("[VillagerMourning] Mod wird initialisiert");

        ModSounds.register();
        ModBlocks.register();

        // Feuert NACH dem Tod jeder LivingEntity - egal ob durch Spieler, Mob, Fallschaden usw. getötet
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof IronGolemEntity golem) {
                World world = golem.getWorld();
                if (world instanceof ServerWorld serverWorld) {
                    LOGGER.info("[VillagerMourning] Iron Golem gestorben bei {} (Ursache: {})",
                            golem.getBlockPos(), damageSource.getName());
                    mourningManager.onIronGolemDeath(serverWorld, golem.getBlockPos());
                }
            }
        });

        // Einmal pro Server-Tick die laufenden Trauer-Events aktualisieren
        ServerTickEvents.END_SERVER_TICK.register(server -> mourningManager.tick());
    }
}
