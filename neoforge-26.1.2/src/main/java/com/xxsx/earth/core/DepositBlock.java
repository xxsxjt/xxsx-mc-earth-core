package com.xxsx.earth.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class DepositBlock extends Block implements EntityBlock {
    private final DepositSpec spec;

    public DepositBlock(DepositSpec spec, BlockBehaviour.Properties properties) {
        super(properties);
        this.spec = spec;
    }

    public DepositSpec spec() {
        return spec;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DepositBlockEntity(pos, state);
    }
}
