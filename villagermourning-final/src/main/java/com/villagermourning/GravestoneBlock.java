package com.villagermourning;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

/**
 * Ein echter Grabstein-Block (kein voller Würfel), zusammengesetzt aus mehreren
 * Quadern, damit er wie ein Fuß + Stele mit abgerundeter Kuppe aussieht -
 * statt einfach nur ein Block mit einem Schild darauf.
 */
public class GravestoneBlock extends Block {

    // Form in 1/16-Block-Einheiten: schmaler Sockel unten, Stele darüber,
    // nach oben hin schmaler werdend ("Kuppe").
    private static final VoxelShape SHAPE = VoxelShapes.union(
            createCuboidShape(3, 0, 6, 13, 3, 10),     // Sockel / Fuß
            createCuboidShape(4, 3, 6.5, 12, 12, 9.5), // Hauptkörper der Stele
            createCuboidShape(5, 12, 7, 11, 14, 9),    // Schulter
            createCuboidShape(6, 14, 7.5, 10, 15, 8.5) // abgerundete Kuppe
    );

    public GravestoneBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }
}
