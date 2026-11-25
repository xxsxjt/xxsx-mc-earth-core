package com.xxsx.earth.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Placeable manual drive mounted against a processing machine. */
public final class HandCrankBlock extends Block {
    public static final EnumProperty<Direction> MOUNT_FACE = EnumProperty.create("mount_face", Direction.class);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(5, 5, 5, 11, 11, 11), Block.box(7, 0, 7, 9, 8, 9), Block.box(1, 7, 7, 15, 9, 9));

    public HandCrankBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(MOUNT_FACE, Direction.DOWN));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos clicked = context.getClickedPos();
        Level level = context.getLevel();
        for (Direction face : context.getNearestLookingDirections()) {
            if (face.getAxis().isHorizontal() && isCrankableTarget(level.getBlockState(clicked.relative(face)))) {
                return defaultBlockState().setValue(MOUNT_FACE, face);
            }
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos machinePos = pos.relative(state.getValue(MOUNT_FACE));
        if (state.getValue(MOUNT_FACE).getAxis() == Direction.Axis.Y) {
            return InteractionResult.PASS;
        }
        if (level.getBlockEntity(machinePos) instanceof BatteryBoxBlockEntity battery) {
            if (!level.isClientSide() && battery.getEnergyStored() < battery.getEnergyCapacity()) {
                int accepted = battery.receiveEnergy(HandCrankItem.CRANK_ENERGY_EOU, false);
                if (accepted > 0) {
                    MachineFeedback.emitBatteryCrank(level, machinePos);
                    player.sendOverlayMessage(Component.translatable(
                            "message.earth_core.hand_crank.battery",
                            accepted, battery.getEnergyStored(), battery.getEnergyCapacity()));
                    player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(machinePos) instanceof ProcessingMachineBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        if (!machine.canCrankTurn()) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.earth_core.hand_crank.no_recipe"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide()) {
            machine.applyCrankTurn(HandCrankItem.CRANK_WORK_TICKS);
            MachineFeedback.emitManualCrank(level, machinePos, level.getBlockState(machinePos), machine.kind());
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
            player.sendOverlayMessage(Component.translatable(
                    "message.earth_core.hand_crank.turned", machine.getDisplayName()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public boolean canSurvive(BlockState state, Level level, BlockPos pos) {
        Direction face = state.getValue(MOUNT_FACE);
        return face.getAxis().isHorizontal() && isCrankableTarget(level.getBlockState(pos.relative(face)));
    }

    private static boolean isCrankableTarget(BlockState state) {
        return state.getBlock() instanceof ProcessingMachineBlock || state.getBlock() instanceof BatteryBoxBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MOUNT_FACE);
    }
}
