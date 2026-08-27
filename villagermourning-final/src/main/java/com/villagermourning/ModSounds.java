package com.villagermourning;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {

    public static final SoundEvent MOURNING_SONG = SoundEvent.of(
            Identifier.of(VillagerMourningMod.MOD_ID, "mourning_song")
    );

    public static void register() {
        Registry.register(
                Registries.SOUND_EVENT,
                Identifier.of(VillagerMourningMod.MOD_ID, "mourning_song"),
                MOURNING_SONG
        );
    }
}
