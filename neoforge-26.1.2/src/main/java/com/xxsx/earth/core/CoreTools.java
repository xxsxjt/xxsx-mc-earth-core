package com.xxsx.earth.core;

import com.xxsx.earth.core.api.v1.EarthCoreApi;
import com.xxsx.earth.core.api.v1.GeologyQueryApi;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class CoreTools {
    private CoreTools() {}

    public static final class GeologyHammerItem extends Item {
        public GeologyHammerItem(Properties props) { super(props); }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            Player player = ctx.getPlayer();
            if (player == null) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                BlockPos pos = ctx.getClickedPos();
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof DepositBlock db) {
                    DepositSpec spec = db.spec();
                    int reserve = spec.defaultReserve();
                    int init = spec.defaultReserve();
                    if (level.getBlockEntity(pos) instanceof DepositBlockEntity dbe) {
                        reserve = dbe.reserve();
                        init = dbe.initialReserve();
                    }
                    player.sendOverlayMessage(Component.translatable(
                            "msg.earth_core.hammer_assay",
                            db.getName().getString(), spec.mineralFamily(), reserve, init, spec.gradeFactor()
                    ));
                } else {
                    player.sendOverlayMessage(Component.translatable(
                            "msg.earth_core.hammer_non_deposit",
                            state.getBlock().getName().getString()
                    ));
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final class OreScannerItem extends Item {
        public OreScannerItem(Properties props) { super(props); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide()) {
                BlockPos origin = player.blockPosition();
                Optional<GeologyQueryApi.DepositInfo> found = EarthCoreApi.geology().findNearestDeposit(level, origin, 48);
                if (found.isPresent()) {
                    GeologyQueryApi.DepositInfo info = found.get();
                    int dist = (int) Math.round(Math.sqrt(info.center().distSqr(origin)));
                    player.sendOverlayMessage(Component.translatable(
                            "msg.earth_core.scanner_hit",
                            info.id(), dist, info.center().getX(), info.center().getY(), info.center().getZ(), info.reserve()
                    ));
                } else {
                    player.sendOverlayMessage(Component.translatable("msg.earth_core.scanner_none"));
                }
                player.getCooldowns().addCooldown(player.getItemInHand(hand), 30);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final class FieldCompendiumItem extends Item {
        public FieldCompendiumItem(Properties props) { super(props); }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide()) {
                double k = EarthCoreApi.thermal().ambientKelvinAt(player.blockPosition().getY(), 15.0D);
                double c = EarthCoreApi.thermal().toCelsius(k);
                player.sendOverlayMessage(Component.translatable(
                        "msg.earth_core.notebook_summary",
                        com.xxsx.earth.core.api.v1.ThermalUnitsApi.STP_PRESSURE_KPA,
                        k, c, EarthCoreApi.geology().catalog().size()
                ));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
