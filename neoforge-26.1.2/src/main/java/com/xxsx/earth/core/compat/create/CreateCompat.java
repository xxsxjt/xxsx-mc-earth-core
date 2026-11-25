package com.xxsx.earth.core.compat.create;

import com.xxsx.earth.core.compat.ICompatUtils;
import net.neoforged.bus.api.IEventBus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Optional Create/third-party wrench compatibility without linking against another mod. */
public final class CreateCompat implements ICompatUtils {
    private static final String CREATE = "create";

    @Override public String modId() { return CREATE; }

    @Override
    public void initial(IEventBus gameBus, IEventBus modBus) {
        // This integration only inspects registry IDs and optional BE methods reflectively.
    }
    private static final Set<String> KINETIC_BLOCKS = Set.of(
            "water_wheel", "large_water_wheel", "windmill_bearing", "steam_engine", "creative_motor");
    private static final Set<String> KINETIC_ITEMS = Set.of(
            "water_wheel", "large_water_wheel", "windmill_bearing", "hand_crank",
            "shaft", "cogwheel", "large_cogwheel");
    private static final Set<Identifier> WRENCH_IDS = Set.of(
            id("create", "wrench"),
            id("ae2", "certus_quartz_wrench"),
            id("ae2", "nether_quartz_wrench"),
            id("refinedstorage", "wrench"));
    private static final Identifier CREATE_CRANK_ID = id(CREATE, "hand_crank");
    private static final TagKey<Item> COMMON_WRENCH_TAG = TagKey.create(
            Registries.ITEM, id("c", "tools/wrench"));
    private static final Map<Class<?>, Optional<Method>> SPEED_METHODS = new ConcurrentHashMap<>();

    public CreateCompat() {
    }

    public static boolean isCreateCrankItem(ItemStack stack) {
        return !stack.isEmpty() && CREATE_CRANK_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isWrenchItem(ItemStack stack) {
        return !stack.isEmpty()
                && (WRENCH_IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))
                || stack.is(COMMON_WRENCH_TAG));
    }

    public static boolean hasAdjacentCreateKineticPower(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockEntity neighborEntity = level.getBlockEntity(neighborPos);
            if (neighborEntity != null && hasCreateSpeed(neighborEntity)) {
                return true;
            }

            Identifier blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(neighborPos).getBlock());
            if (blockId != null && CREATE.equals(blockId.getNamespace())
                    && KINETIC_BLOCKS.contains(blockId.getPath())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCreateKineticItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return itemId != null && CREATE.equals(itemId.getNamespace()) && KINETIC_ITEMS.contains(itemId.getPath());
    }

    private static boolean hasCreateSpeed(BlockEntity blockEntity) {
        Optional<Method> method = SPEED_METHODS.computeIfAbsent(blockEntity.getClass(), type -> {
            try {
                return Optional.of(type.getMethod("getSpeed"));
            } catch (NoSuchMethodException ignored) {
                return Optional.empty();
            }
        });
        if (method.isEmpty()) {
            return false;
        }
        try {
            Object speed = method.get().invoke(blockEntity);
            return speed instanceof Number number && Math.abs(number.floatValue()) >= 1.0f;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
