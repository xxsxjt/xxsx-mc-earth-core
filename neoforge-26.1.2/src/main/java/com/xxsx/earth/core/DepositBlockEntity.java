package com.xxsx.earth.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores finite extractable mineral reserve for a natural deposit block. */
public final class DepositBlockEntity extends BlockEntity {
    private String depositId = "magnetite_ore";
    private int initialReserve = 24000;
    private int reserve = 24000;

    public DepositBlockEntity(BlockPos pos, BlockState state) {
        super(EarthCoreContent.DEPOSIT_BE.get(), pos, state);
        if (state.getBlock() instanceof DepositBlock db) {
            this.depositId = db.spec().id();
            this.initialReserve = db.spec().defaultReserve();
            this.reserve = db.spec().defaultReserve();
        }
    }

    public String depositId() { return depositId; }
    public int initialReserve() { return initialReserve; }
    public int reserve() { return reserve; }
    public boolean isDepleted() { return reserve <= 0; }

    public int extract(int amount) {
        if (amount <= 0 || reserve <= 0) return 0;
        int taken = Math.min(amount, reserve);
        reserve -= taken;
        setChanged();
        return taken;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("deposit_id", depositId);
        output.putInt("initial_reserve", initialReserve);
        output.putInt("reserve", reserve);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        depositId = input.getStringOr("deposit_id", depositId);
        initialReserve = input.getIntOr("initial_reserve", initialReserve);
        reserve = input.getIntOr("reserve", reserve);
    }
}
