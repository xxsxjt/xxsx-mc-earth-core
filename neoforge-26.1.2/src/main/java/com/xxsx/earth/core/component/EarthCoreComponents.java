package com.xxsx.earth.core.component;

import com.xxsx.earth.core.EarthCore;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EarthCoreComponents {
    private EarthCoreComponents() {}

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EarthCore.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MaterialBatch>> MATERIAL_BATCH =
            DATA_COMPONENTS.registerComponentType("material_batch", builder ->
                    builder.persistent(MaterialBatch.CODEC).networkSynchronized(MaterialBatch.STREAM_CODEC));
}
