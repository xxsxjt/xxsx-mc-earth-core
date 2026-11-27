package com.xxsx.earth.core;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

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
            float hardness = spec.alterationHalo() ? 2.2F : (spec.hostRock().equals("deepslate") || spec.hostRock().equals("kimberlite") ? 4.2F : 3.0F);
            DeferredBlock<DepositBlock> blk = BLOCKS.registerBlock(
                    spec.id(),
                    props -> new DepositBlock(spec, props),
                    () -> BlockBehaviour.Properties.of()
                            .strength(hardness, 6.0F)
                            .sound(spec.hostRock().equals("gravel") ? SoundType.GRAVEL : SoundType.STONE)
                            .requiresCorrectToolForDrops()
            );
            DEPOSIT_BLOCKS.put(spec.id(), blk);
            DEPOSIT_ITEMS.put(spec.id(), ITEMS.registerSimpleBlockItem(spec.id(), blk));
        }
    }

    public static final DeferredItem<Item> GEOLOGY_HAMMER = ITEMS.registerItem(
            "geology_hammer", CoreTools.GeologyHammerItem::new, () -> new Item.Properties().stacksTo(1).durability(256));
    public static final DeferredItem<Item> ORE_SCANNER = ITEMS.registerItem(
            "ore_scanner", CoreTools.OreScannerItem::new, () -> new Item.Properties().stacksTo(1).durability(128));
    public static final DeferredItem<Item> FIELD_GEOLOGY_NOTEBOOK = ITEMS.registerItem(
            "field_geology_notebook", CoreTools.FieldCompendiumItem::new, () -> new Item.Properties().stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DepositBlockEntity>> DEPOSIT_BE =
            BLOCK_ENTITIES.register("deposit", () -> new BlockEntityType<>(
                    DepositBlockEntity::new,
                    DEPOSIT_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)
            ));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.earth_core.main"))
                    .icon(() -> new ItemStack(GEOLOGY_HAMMER.get()))
                    .displayItems((params, output) -> {
                        output.accept(GEOLOGY_HAMMER.get());
                        output.accept(ORE_SCANNER.get());
                        output.accept(FIELD_GEOLOGY_NOTEBOOK.get());
                        for (DeferredItem<BlockItem> item : DEPOSIT_ITEMS.values()) {
                            output.accept(item.get());
                        }
                    })
                    .build());
}
