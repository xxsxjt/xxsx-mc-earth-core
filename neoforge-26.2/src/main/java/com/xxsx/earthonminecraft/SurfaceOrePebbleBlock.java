package com.xxsx.earthonminecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * Loose mineral float pebbles scattered lightly above real underground ore bodies.
 *
 * <p>Sits on top of natural ground without replacing grass or soil, breaks easily by hand or
 * pickaxe into 1-2 early-game mineral chunks/dusts, and hints at the underlying deposit type when
 * inspected.</p>
 */
public final class SurfaceOrePebbleBlock extends Block {
    public enum PebbleMineral implements StringRepresentable {
        MAGNETITE("magnetite", "magnetite_lens"),
        CHALCOPYRITE("chalcopyrite", "chalcopyrite_vein"),
        CASSITERITE("cassiterite", "cassiterite_placer_deposit"),
        COAL("coal", "bituminous_coal_seam"),
        GALENA("galena", "galena_lens"),
        AURIFEROUS_QUARTZ("auriferous_quartz", "auriferous_quartz_vein"),
        LIMESTONE("limestone", "limestone_calcite_bed");

        private final String serializedName;
        private final String primaryLodeId;

        PebbleMineral(String serializedName, String primaryLodeId) {
            this.serializedName = serializedName;
            this.primaryLodeId = primaryLodeId;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }

        public String primaryLodeId() {
            return primaryLodeId;
        }

        public String translationKey() {
            return "block.earth_on_minecraft.surface_ore_pebble." + serializedName;
        }

        public static Optional<PebbleMineral> forLode(String lodeId) {
            return SurfaceSignalRules.pebbleMineralKeyForLode(lodeId).map(key -> {
                for (PebbleMineral mineral : values()) {
                    if (mineral.serializedName.equals(key)) {
                        return mineral;
                    }
                }
                return MAGNETITE;
            });
        }
    }

    public static final EnumProperty<PebbleMineral> MINERAL = EnumProperty.create("mineral", PebbleMineral.class);
    public static final VoxelShape SHAPE = SurfaceSignalRules.PEBBLE_SHAPE;

    public SurfaceOrePebbleBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MINERAL, PebbleMineral.MAGNETITE));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return Block.isFaceFull(belowState.getCollisionShape(level, below), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess scheduledTickAccess,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, scheduledTickAccess, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof GeologyHammerItem) {
            return InteractionResult.PASS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            PebbleMineral mineral = state.getValue(MINERAL);
            player.sendOverlayMessage(clueComponent(mineral));
        }
        return InteractionResult.SUCCESS;
    }

    public static Component clueComponent(PebbleMineral mineral) {
        return Component.translatable("message.earth_on_minecraft.surface_ore_pebble.hint",
                Component.translatable(mineral.translationKey()),
                ProspectingSurvey.lodeName(mineral.primaryLodeId()))
                .withStyle(ChatFormatting.YELLOW);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MINERAL);
    }
}
