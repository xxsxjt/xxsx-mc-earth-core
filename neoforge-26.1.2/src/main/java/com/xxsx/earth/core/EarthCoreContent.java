package com.xxsx.earth.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Pure substrate registrations for Earth: Core (zero misplaced placeholder tools;
 * geology tools live strictly in earth_geography).
 */
public final class EarthCoreContent {
    private EarthCoreContent() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EarthCore.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EarthCore.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EarthCore.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EarthCore.MODID);

    public static final Map<String, DeferredBlock<DepositBlock>> DEPOSIT_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> DEPOSIT_ITEMS = new LinkedHashMap<>();

    static {
        for (DepositSpec spec : DepositSpec.ALL) {
            float hardness = spec.alterationHalo() ? 2.2F
                    : (spec.hostRock().equals("deepslate") || spec.hostRock().equals("kimberlite") ? 4.2F : 3.0F);
            DeferredBlock<DepositBlock> blk = BLOCKS.registerBlock(
                    spec.id(),
                    props -> new DepositBlock(spec, props),
                    () -> BlockBehaviour.Properties.of()
                            .strength(hardness, 6.0F)
                            .sound(spec.hostRock().equals("gravel") ? SoundType.GRAVEL : SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );
            DEPOSIT_BLOCKS.put(spec.id(), blk);
            DEPOSIT_ITEMS.put(spec.id(), ITEMS.registerItem(
                    spec.id(),
                    props -> new DepositBlockItem(blk.get(), spec, props.useBlockDescriptionPrefix()),
                    Item.Properties::new
            ));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DepositBlockEntity>> DEPOSIT_BE =
            BLOCK_ENTITIES.register("deposit", () -> new BlockEntityType<>(
                    DepositBlockEntity::new,
                    DEPOSIT_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
            ));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.earth_core.main"))
                    .icon(() -> new ItemStack(DEPOSIT_ITEMS.values().iterator().next().get()))
                    .displayItems((params, output) -> {
                        for (DeferredItem<BlockItem> item : DEPOSIT_ITEMS.values()) {
                            output.accept(item.get());
                        }
                    })
                    .build());

    public static final class DepositBlockItem extends BlockItem {
        private final DepositSpec spec;

        public DepositBlockItem(Block block, DepositSpec spec, Properties props) {
            super(block, props);
            this.spec = spec;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display,
                                    Consumer<Component> out, TooltipFlag flag) {
            out.accept(Component.translatable(
                    "tooltip.earth_core.deposit_summary",
                    spec.mineralFamily(),
                    spec.hostRock(),
                    spec.defaultReserve()
            ).withStyle(ChatFormatting.GRAY));
            out.accept(Component.translatable(
                    "tooltip.earth_core.deposit_grade",
                    String.format("%.2f", spec.gradeFactor())
            ).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
