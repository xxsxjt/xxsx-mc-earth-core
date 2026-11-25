package com.xxsx.earth.core;

import com.xxsx.earth.core.living.SettlementSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettlementBoardMenu extends AbstractContainerMenu {
    public static final int BUTTON_TRAVEL_BASE = 1000;
    private final BlockPos pos;
    private final SettlementSavedData.SettlementSnapshot snapshot;
    private final List<TravelDestination> destinations;

    public SettlementBoardMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, readPayload(buf));
    }

    private SettlementBoardMenu(int containerId, Inventory inventory, Payload payload) {
        this(containerId, inventory, payload.pos(), payload.snapshot(), payload.destinations());
    }

    public SettlementBoardMenu(int containerId, Inventory inventory, BlockPos pos,
                               SettlementSavedData.SettlementSnapshot snapshot) {
        this(containerId, inventory, pos, snapshot, List.of());
    }

    private SettlementBoardMenu(int containerId, Inventory inventory, BlockPos pos,
                                SettlementSavedData.SettlementSnapshot snapshot, List<TravelDestination> destinations) {
        super(EarthOnMinecraft.SETTLEMENT_BOARD_MENU.get(), containerId);
        this.pos = pos;
        this.snapshot = snapshot;
        this.destinations = List.copyOf(destinations);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(pos).is(EarthOnMinecraft.SETTLEMENT_BOARD.get())
                && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < BUTTON_TRAVEL_BASE || !(player instanceof ServerPlayer serverPlayer)
                || !(player.level() instanceof ServerLevel currentLevel)) {
            return super.clickMenuButton(player, id);
        }
        int index = id - BUTTON_TRAVEL_BASE;
        if (index < 0 || index >= destinations.size()) {
            return false;
        }
        TravelDestination target = destinations.get(index);
        if (ModList.get().isLoaded("waystones") && tryWaystonesTeleport(serverPlayer, target)) {
            serverPlayer.closeContainer();
            return true;
        }
        ServerLevel destinationLevel = currentLevel.getServer().getLevel(
                net.minecraft.resources.ResourceKey.create(Registries.DIMENSION, Identifier.parse(target.dimension())));
        if (destinationLevel == null) {
            return false;
        }
        var center = target.center();
        var safe = net.minecraft.core.BlockPos.containing(center.getX() + 0.5D, center.getY() + 1.0D, center.getZ() + 0.5D);
        while (safe.getY() < destinationLevel.getMaxY() - 1
                && (!destinationLevel.getBlockState(safe).getCollisionShape(destinationLevel, safe).isEmpty()
                || !destinationLevel.getBlockState(safe.above()).getCollisionShape(destinationLevel, safe.above()).isEmpty())) {
            safe = safe.above();
        }
        if (!destinationLevel.getBlockState(safe.below()).isSolid()) {
            return false;
        }
        serverPlayer.closeContainer();
        serverPlayer.teleportTo(destinationLevel, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                java.util.Set.of(), serverPlayer.getYRot(), serverPlayer.getXRot(), true);
        return true;
    }

    /** Uses Waystones' public API when a destination has an activated waystone at its settlement anchor. */
    private static boolean tryWaystonesTeleport(ServerPlayer player, TravelDestination target) {
        try {
            var server = player.level().getServer();
            if (server == null) return false;
            var destinationLevel = server.getLevel(net.minecraft.resources.ResourceKey.create(
                    Registries.DIMENSION, Identifier.parse(target.dimension())));
            if (destinationLevel == null) return false;
            var waystone = net.blay09.mods.waystones.api.WaystonesAPI.getAllWaystones(server).stream()
                    .filter(candidate -> candidate.isValidInLevel(destinationLevel))
                    .filter(candidate -> candidate.getDimension().equals(destinationLevel.dimension()))
                    .filter(candidate -> candidate.getPos().distSqr(target.center()) <= 32.0D)
                    .findFirst();
            if (waystone.isEmpty()) return false;
            var context = net.blay09.mods.waystones.api.WaystonesAPI
                    .createDefaultTeleportContext(player, waystone.get(), ignored -> {});
            if (context.left().isPresent()) {
                var result = net.blay09.mods.waystones.api.WaystonesAPI.tryTeleport(context.left().get());
                if (result.left().isPresent()) return true;
            }
        } catch (LinkageError | RuntimeException ignored) {
            // The optional API is deliberately fail-open to the built-in settlement teleport.
        }
        return false;
    }

    public BlockPos pos() {
        return pos;
    }

    public SettlementSavedData.SettlementSnapshot snapshot() {
        return snapshot;
    }

    public int tier() {
        return snapshot.tier();
    }

    public String tierProgressKey() {
        return snapshot.tierProgressKey();
    }

    public List<TravelDestination> destinations() {
        return destinations;
    }

    public static void writePayload(RegistryFriendlyByteBuf buf, BlockPos pos,
                                    SettlementSavedData.SettlementSnapshot snapshot,
                                    List<SettlementSavedData.SettlementSnapshot> allSettlements) {
        buf.writeBlockPos(pos);
        buf.writeUtf(snapshot.id());
        buf.writeUtf(snapshot.dimension());
        buf.writeBlockPos(snapshot.center());
        buf.writeUtf(snapshot.profileId());
        buf.writeUtf(snapshot.nameKey());
        buf.writeVarInt(snapshot.tier());
        buf.writeUtf(snapshot.tierProgressKey());
        buf.writeUtf(snapshot.profileNameKey());
        buf.writeUtf(snapshot.scaleKey());
        buf.writeUtf(snapshot.technologyKey());
        writeStrings(buf, snapshot.industryKeys());
        writeStrings(buf, snapshot.demandKeys());
        writeStrings(buf, snapshot.supplyKeys());
        writeResidents(buf, snapshot.residents());
        writeCounts(buf, snapshot.roleCounts());
        writeCounts(buf, snapshot.facilityCounts());
        buf.writeVarInt(snapshot.security());
        buf.writeInt(snapshot.reputation());
        buf.writeLong(snapshot.createdAt());
        buf.writeLong(snapshot.updatedAt());
        List<SettlementSavedData.SettlementSnapshot> destinations = allSettlements.stream()
                .filter(candidate -> !candidate.id().equals(snapshot.id())).toList();
        buf.writeVarInt(destinations.size());
        for (SettlementSavedData.SettlementSnapshot destination : destinations) {
            buf.writeUtf(destination.id());
            buf.writeUtf(destination.dimension());
            buf.writeBlockPos(destination.center());
            buf.writeUtf(destination.nameKey());
        }
    }

    public static SettlementSavedData.SettlementSnapshot emptySnapshot(BlockPos pos) {
        return new SettlementSavedData.SettlementSnapshot("", "minecraft:overworld", pos,
                EarthOnMinecraft.id("agricultural_village").toString(),
                "settlement.earth_core.name.unknown",
                1, "settlement.earth_core.tier.status.tier_1_need_mining",
                "settlement.earth_core.profile.agricultural_village",
                "settlement.earth_core.scale.village",
                "settlement.earth_core.technology.agricultural",
                List.of(), List.of(), List.of(), List.of(), Map.of(), Map.of(), 50, 0, 0L, 0L);
    }

    private static Payload readPayload(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        SettlementSavedData.SettlementSnapshot snapshot = new SettlementSavedData.SettlementSnapshot(
                buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readUtf(), buf.readUtf(),
                Math.max(1, Math.min(4, buf.readVarInt())), buf.readUtf(), buf.readUtf(),
                buf.readUtf(), buf.readUtf(), readStrings(buf), readStrings(buf), readStrings(buf),
                readResidents(buf), readCounts(buf), readCounts(buf), buf.readVarInt(), buf.readInt(),
                buf.readLong(), buf.readLong());
        int size = Math.min(128, Math.max(0, buf.readVarInt()));
        List<TravelDestination> destinations = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            destinations.add(new TravelDestination(buf.readUtf(), buf.readUtf(), buf.readBlockPos(), buf.readUtf()));
        }
        return new Payload(pos, snapshot, destinations);
    }

    private static void writeStrings(RegistryFriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        values.forEach(buf::writeUtf);
    }

    private static List<String> readStrings(RegistryFriendlyByteBuf buf) {
        int size = Math.min(256, Math.max(0, buf.readVarInt()));
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readUtf());
        }
        return List.copyOf(values);
    }

    private static void writeCounts(RegistryFriendlyByteBuf buf, Map<String, Integer> values) {
        buf.writeVarInt(values.size());
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(Math.max(0, entry.getValue()));
        });
    }

    private static Map<String, Integer> readCounts(RegistryFriendlyByteBuf buf) {
        int size = Math.min(256, Math.max(0, buf.readVarInt()));
        Map<String, Integer> values = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            values.put(buf.readUtf(), Math.max(0, buf.readVarInt()));
        }
        return Map.copyOf(values);
    }

    private static void writeResidents(RegistryFriendlyByteBuf buf,
                                       List<SettlementSavedData.ResidentSummary> residents) {
        buf.writeVarInt(residents.size());
        for (SettlementSavedData.ResidentSummary resident : residents) {
            buf.writeUtf(resident.nameKey());
            buf.writeUtf(resident.roleId());
            buf.writeVarInt(Math.max(1, resident.skill()));
        }
    }

    private static List<SettlementSavedData.ResidentSummary> readResidents(RegistryFriendlyByteBuf buf) {
        int size = Math.min(256, Math.max(0, buf.readVarInt()));
        List<SettlementSavedData.ResidentSummary> residents = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            residents.add(new SettlementSavedData.ResidentSummary(
                    buf.readUtf(), buf.readUtf(), Math.max(1, buf.readVarInt())));
        }
        return List.copyOf(residents);
    }

    public record TravelDestination(String id, String dimension, BlockPos center, String nameKey) {
    }

    private record Payload(BlockPos pos, SettlementSavedData.SettlementSnapshot snapshot,
                           List<TravelDestination> destinations) {
    }
}
