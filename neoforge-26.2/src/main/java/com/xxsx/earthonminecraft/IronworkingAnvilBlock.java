package com.xxsx.earthonminecraft;

import com.xxsx.earthonminecraft.component.EarthDataComponents;
import com.xxsx.earthonminecraft.component.ForgingProcess;
import com.xxsx.earthonminecraft.component.MetalBatch;
import com.xxsx.earthonminecraft.component.QualityComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

public class IronworkingAnvilBlock extends Block {
    public IronworkingAnvilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        InteractionHand workpieceHand = player.getItemInHand(InteractionHand.MAIN_HAND)
                .has(EarthDataComponents.FORGING_PROCESS.get()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack workpiece = player.getItemInHand(workpieceHand);
        ForgingProcess process = workpiece.get(EarthDataComponents.FORGING_PROCESS.get());
        if (process == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            ForgingProcess switched = process.withTarget(process.targetForm().next());
            if (!level.isClientSide()) {
                workpiece.set(EarthDataComponents.FORGING_PROCESS.get(), switched);
                player.sendOverlayMessage(forgingDashboard(workpiece, switched,
                        (int) switched.currentTemperatureK(level.getGameTime(), sampleNearbyTemperatureK(level, pos))));
            }
            return InteractionResult.SUCCESS;
        }
        int sourceK = sampleNearbyTemperatureK(level, pos);
        if (sourceK >= ForgingProcess.MIN_HOT_WORK_K) {
            ForgingProcess heated = process.reheated(level.getGameTime(), sourceK);
            if (!level.isClientSide()) {
                workpiece.set(EarthDataComponents.FORGING_PROCESS.get(), heated);
                player.sendOverlayMessage(forgingDashboard(workpiece, heated, sourceK));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack hammer, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hitResult) {
        InteractionHand hammerHand = hand;
        ItemStack held = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);
        ForgingProcess otherProcess = other.get(EarthDataComponents.FORGING_PROCESS.get());
        if (otherProcess != null && otherProcess.targetForm() != ForgingProcess.TargetForm.INGOT_REFINING
                && (held.is(Items.WATER_BUCKET) || held.is(Items.POTION))) {
            if (!level.isClientSide()) {
                ForgingProcess quenched = quench(otherProcess, (int) otherProcess.currentTemperatureK(level.getGameTime(), sampleNearbyTemperatureK(level, pos)), held.is(Items.WATER_BUCKET));
                other.set(EarthDataComponents.FORGING_PROCESS.get(), quenched);
                if (held.is(Items.WATER_BUCKET)) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                player.sendOverlayMessage(forgingDashboard(other, quenched, (int) quenched.currentTemperatureK(level.getGameTime(), 293)));
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && held.isEmpty() && otherProcess != null) {
            ForgingProcess switched = otherProcess.withTarget(otherProcess.targetForm().next());
            if (!level.isClientSide()) {
                other.set(EarthDataComponents.FORGING_PROCESS.get(), switched);
                player.sendOverlayMessage(forgingDashboard(other, switched, (int) switched.currentTemperatureK(level.getGameTime(), sampleNearbyTemperatureK(level, pos))));
            }
            return InteractionResult.SUCCESS;
        }
        if (held.isEmpty() && otherProcess != null && sampleNearbyTemperatureK(level, pos) >= ForgingProcess.MIN_HOT_WORK_K) {
            if (!level.isClientSide()) {
                ForgingProcess heated = otherProcess.reheated(level.getGameTime(), sampleNearbyTemperatureK(level, pos));
                other.set(EarthDataComponents.FORGING_PROCESS.get(), heated);
                player.sendOverlayMessage(forgingDashboard(other, heated, (int) heated.initialTempK()));
            }
            return InteractionResult.SUCCESS;
        }
        ForgingProcess process = held.get(EarthDataComponents.FORGING_PROCESS.get());
        if (process != null && !EarthMedicalAndWeaponSupport.isAnvilHammer(held)) {
            if (player.isShiftKeyDown() && held.isEmpty()) {
                process = process.withTarget(process.targetForm().next());
                player.setItemInHand(hand, held);
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(forgingDashboard(held, process, 293));
                }
                return InteractionResult.SUCCESS;
            }
            if (process.targetForm() != ForgingProcess.TargetForm.INGOT_REFINING && (held.is(Items.WATER_BUCKET) || held.is(Items.POTION))) {
                if (!level.isClientSide()) {
                    ForgingProcess quenched = quench(process, 1200, held.is(Items.WATER_BUCKET));
                    held.set(EarthDataComponents.FORGING_PROCESS.get(), quenched);
                    if (held.is(Items.WATER_BUCKET)) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    player.sendOverlayMessage(forgingDashboard(held, quenched, 1200));
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (!EarthMedicalAndWeaponSupport.isAnvilHammer(player.getItemInHand(hammerHand))) {
            hammerHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        }
        ItemStack activeHammer = player.getItemInHand(hammerHand);
        if (!EarthMedicalAndWeaponSupport.isAnvilHammer(activeHammer)) {
            return InteractionResult.PASS;
        }
        InteractionHand workpieceHand = hammerHand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack workpiece = player.getItemInHand(workpieceHand);
        int hearthTempK = sampleNearbyTemperatureK(level, pos);
        ForgingProcess current = workpiece.getOrDefault(EarthDataComponents.FORGING_PROCESS.get(), ForgingProcess.heated(0L, hearthTempK));
        double currentTemp = current.currentTemperatureK(level.getGameTime(), hearthTempK);
        if (current.quenchState() != ForgingProcess.QuenchState.UNQUENCHED
                && hearthTempK >= 850 && hearthTempK <= 1200) {
            ForgingProcess tempered = new ForgingProcess(1, level.getGameTime(), hearthTempK, 4.0,
                    current.foldCount(), current.shapingProgress(), current.targetForm(), current.edgeSharpness(),
                    current.quenchState(), ForgingProcess.TemperState.TEMPERED,
                    Math.max(0.0, current.microcracks() - 0.25), current.decarburization(),
                    Math.min(1.0, current.substratePotential() + 0.10));
            if (!level.isClientSide()) {
                workpiece.set(EarthDataComponents.FORGING_PROCESS.get(), tempered);
                player.sendOverlayMessage(forgingDashboard(workpiece, tempered, (int) currentTemp));
                playStrike(level, pos, player, hammerHand);
            }
            return InteractionResult.SUCCESS;
        }
        if (currentTemp < ForgingProcess.MIN_HOT_WORK_K) {
            ForgingProcess coldWorked = shape(current, currentTemp, player);
            if (!level.isClientSide()) {
                workpiece.set(EarthDataComponents.FORGING_PROCESS.get(), coldWorked);
                player.sendOverlayMessage(forgingDashboard(workpiece, coldWorked, (int) currentTemp));
                playStrike(level, pos, player, hammerHand);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && currentTemp >= ForgingProcess.FOLD_MIN_K && currentTemp <= ForgingProcess.FOLD_MAX_K) {
            ForgingProcess folded = fold(current, currentTemp);
            if (!level.isClientSide()) {
                workpiece.set(EarthDataComponents.FORGING_PROCESS.get(), folded);
                player.sendOverlayMessage(forgingDashboard(workpiece, folded, (int) currentTemp));
                playStrike(level, pos, player, hammerHand);
            }
            return InteractionResult.SUCCESS;
        }
        if (currentTemp >= ForgingProcess.MIN_HOT_WORK_K && currentTemp <= 1450 && !workpiece.isEmpty()) {
            ForgingProcess worked = shape(current, currentTemp, player);
            if (!worked.shaped() || !worked.edged() || worked.targetForm() == ForgingProcess.TargetForm.INGOT_REFINING) {
                if (!level.isClientSide()) {
                    workpiece.set(EarthDataComponents.FORGING_PROCESS.get(), worked);
                    player.sendOverlayMessage(forgingDashboard(workpiece, worked, (int) currentTemp));
                    playStrike(level, pos, player, hammerHand);
                }
                return InteractionResult.SUCCESS;
            }
        }
        Optional<ItemStack> result = forgeResult(workpiece, activeHammer, (int) currentTemp);
        if (current.shaped() && current.targetForm() != ForgingProcess.TargetForm.INGOT_REFINING
                && current.quenchState() != ForgingProcess.QuenchState.OVERQUENCHED_CRACKED
                && (current.quenchState() == ForgingProcess.QuenchState.UNQUENCHED || current.temperState() == ForgingProcess.TemperState.TEMPERED)) {
            Optional<ItemStack> tool = MetallurgyAssay.finalizeForgedTool(workpiece, current, (int) currentTemp);
            if (tool.isPresent()) result = tool;
        }
        if (result.isEmpty()) {
            if (!level.isClientSide()) {
                player.swing(hammerHand, true);
                player.sendSystemMessage(Component.translatable("message.earth_on_minecraft.anvil.invalid_workpiece"));
            }
            return InteractionResult.SUCCESS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack output = result.get();
        if ((workpiece.is(EarthOnMinecraft.SPONGE_IRON.get()) || workpiece.is(EarthOnMinecraft.WROUGHT_IRON.get()))
                && (hearthTempK >= 950 || activeHammer.getItem() != EarthOnMinecraft.GEOLOGY_HAMMER.get())) {
            output.grow(1);
        }
        workpiece.shrink(1);
        if (workpiece.isEmpty()) {
            player.setItemInHand(workpieceHand, output);
        } else if (!player.addItem(output)) {
            popResource(level, pos, output);
        }
        activeHammer.hurtAndBreak(1, player, hammerHand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND
                : EquipmentSlot.OFFHAND);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS,
                0.85F, 0.95F + level.getRandom().nextFloat() * 0.10F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    pos.getX() + 0.5D, pos.getY() + 1.02D, pos.getZ() + 0.5D,
                    10, 0.18D, 0.08D, 0.18D, 0.12D);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    pos.getX() + 0.5D, pos.getY() + 1.02D, pos.getZ() + 0.5D,
                    4, 0.12D, 0.05D, 0.12D, 0.02D);
        }
        player.swing(hammerHand, true);
        player.sendSystemMessage(Component.translatable("message.earth_on_minecraft.anvil.forged",
                output.getHoverName()));
        return InteractionResult.SUCCESS;
    }

    public static Optional<ItemStack> forgeResult(ItemStack workpiece) {
        return forgeResult(workpiece, ItemStack.EMPTY, 293);
    }

    public static Optional<ItemStack> forgeResult(ItemStack workpiece, ItemStack hammer, int temperatureK) {
        return MetallurgyAssay.forgeAnvilWorkpiece(workpiece, hammer, temperatureK);
    }

    private static ForgingProcess fold(ForgingProcess p, double tempK) {
        return new ForgingProcess(1, 0L, tempK, 8.0, Math.min(12, p.foldCount() + 1),
                p.shapingProgress(), p.targetForm(), p.edgeSharpness(), p.quenchState(), p.temperState(),
                Math.max(0.0, p.microcracks() - 0.02), p.decarburization(), Math.min(1.0, p.substratePotential() + 0.035));
    }

    private static ForgingProcess shape(ForgingProcess p, double tempK, Player player) {
        double strength = 0.30 + 0.05 * p.foldCount();
        double bonus = player.getPersistentData().getDoubleOr("earth_human.forging_power_bonus", 0.0);
        double rate = Math.max(0.12, Math.min(0.55, strength + bonus * 0.10));
        double shaped = Math.min(1.0, p.shapingProgress() + rate * (tempK >= 950 && tempK <= 1350 ? 1.0 : 0.55));
        double edge = shaped >= 0.80 ? Math.min(1.0, p.edgeSharpness() + rate * 0.32) : p.edgeSharpness();
        double cracks = tempK < ForgingProcess.COLD_FORGING_K ? Math.min(1.0, p.microcracks() + 0.08) : p.microcracks();
        double decarb = tempK > ForgingProcess.OVERHEAT_K ? Math.min(1.0, p.decarburization() + 0.06) : p.decarburization();
        return new ForgingProcess(1, 0L, tempK, 8.0, p.foldCount(), shaped, p.targetForm(), edge,
                p.quenchState(), p.temperState(), cracks, decarb, Math.min(1.0, p.substratePotential() + rate * 0.03));
    }

    private static ForgingProcess quench(ForgingProcess p, double tempK, boolean water) {
        ForgingProcess.QuenchState state = tempK >= ForgingProcess.QUENCH_MIN_K && tempK <= ForgingProcess.QUENCH_MAX_K
                ? (water ? ForgingProcess.QuenchState.WATER_QUENCHED : ForgingProcess.QuenchState.OIL_OR_BRINE_QUENCHED)
                : ForgingProcess.QuenchState.OVERQUENCHED_CRACKED;
        return new ForgingProcess(1, 0L, ForgingProcess.AMBIENT_K, 8.0, p.foldCount(), p.shapingProgress(), p.targetForm(),
                p.edgeSharpness(), state, p.temperState(), state == ForgingProcess.QuenchState.OVERQUENCHED_CRACKED ? 1.0 : p.microcracks(),
                p.decarburization(), p.substratePotential());
    }

    private static Component forgingDashboard(ItemStack stack, ForgingProcess p, int tempK) {
        MetalBatch metal = QualityComponents.resolveMetalBatch(stack).orElse(MetalBatch.DEFAULT);
        return Component.literal("[锻造] 温度: " + tempK + "K | 工序: 折叠x" + p.foldCount()
                + " 塑形:" + Math.round(p.shapingProgress() * 100) + "%(" + p.targetForm().getSerializedName()
                + ") 锐度:" + Math.round(p.edgeSharpness() * 100) + "% | 成分: C "
                + QualityComponents.formatPercent(metal.carbonContent()) + "% 渣 "
                + QualityComponents.formatPercent(metal.slagContent()) + "%");
    }

    private static void playStrike(Level level, BlockPos pos, Player player, InteractionHand hand) {
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.85F, 0.95F);
        player.swing(hand, true);
    }

    private static int sampleNearbyTemperatureK(Level level, BlockPos pos) {
        int bestK = 293;
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockEntity(neighbor) instanceof ProcessingMachineBlockEntity machine) {
                bestK = Math.max(bestK, machine.temperatureK());
            } else {
                BlockState state = level.getBlockState(neighbor);
                if (state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) {
                    bestK = Math.max(bestK, 1300);
                } else if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CAMPFIRE)) {
                    bestK = Math.max(bestK, 1050);
                }
            }
        }
        return bestK;
    }
}
