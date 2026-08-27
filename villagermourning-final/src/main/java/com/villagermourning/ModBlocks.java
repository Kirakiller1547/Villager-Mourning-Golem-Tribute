package com.villagermourning;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

/**
 * Registriert den echten Grabstein-Block (statt Steinblock + Schild).
 */
public class ModBlocks {

    public static final Block GRAVESTONE = registerBlock(
            "gravestone",
            settings -> new GravestoneBlock(settings)
    );

    private static Block registerBlock(String name, java.util.function.Function<AbstractBlock.Settings, Block> factory) {
        Identifier id = Identifier.of(VillagerMourningMod.MOD_ID, name);
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, id);

        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
                .mapColor(MapColor.STONE_GRAY)
                .strength(1.5f, 6.0f)
                .sounds(BlockSoundGroup.STONE)
                .nonOpaque()
                .registryKey(blockKey);

        Block block = Registry.register(Registries.BLOCK, blockKey, factory.apply(settings));

        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, id);
        Registry.register(
                Registries.ITEM,
                itemKey,
                new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey())
        );

        return block;
    }

    /** Sorgt dafür, dass die statische Initialisierung (und damit die Registrierung) ausgeführt wird. */
    public static void register() {
        // Absichtlich leer - der Aufruf lädt die Klasse und triggert die Registrierung oben.
    }
}
