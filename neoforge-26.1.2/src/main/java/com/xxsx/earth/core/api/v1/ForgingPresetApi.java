package com.xxsx.earth.core.api.v1;

import com.xxsx.earth.core.component.EarthCoreComponents;
import com.xxsx.earth.core.component.MaterialComposition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Core material & forging preset resolver (Tetra-style graceful fallback architecture).
 *
 * <p>Downstream mods (e.g., earth_xuanxue flying sword / spirit weapon forging, earth_magic wands)
 * query {@link #resolveProfile(ItemStack)} without hard-depending on a forging/metallurgy addon:
 * when a specialized addon is absent, clean default material presets are returned automatically;
 * when an addon is installed, it registers a provider or attaches {@link MaterialComposition}
 * and all downstream recipes/weapons seamlessly gain alloy-grade bonuses.</p>
 */
public final class ForgingPresetApi {

    public record MaterialProfile(
            String id,
            String displayNameZh,
            float hardness,
            float conductivity,
            float qiAffinity,
            float durabilityMultiplier,
            float attackBonus,
            Map<String, Integer> alloyPermille,
            boolean fromFallbackPreset
    ) {}

    private final Map<String, MaterialProfile> defaultPresets = new LinkedHashMap<>();
    private final List<Function<ItemStack, Optional<MaterialProfile>>> externalProviders = new ArrayList<>();

    public ForgingPresetApi() {
        registerDefault(new MaterialProfile("copper", "标准红铜（默认预设）", 3.0F, 1.35F, 0.85F, 0.90F, 0.5F, Map.of("cu", 990), true));
        registerDefault(new MaterialProfile("iron", "标准熟铁（默认预设）", 4.5F, 0.85F, 0.75F, 1.00F, 1.0F, Map.of("fe", 985, "c", 15), true));
        registerDefault(new MaterialProfile("gold", "标准赤金（默认预设）", 2.5F, 1.45F, 1.35F, 0.70F, 0.8F, Map.of("au", 995), true));
        registerDefault(new MaterialProfile("diamond", "金刚晶碳（默认预设）", 9.5F, 0.40F, 1.15F, 1.65F, 2.2F, Map.of("c_diamond", 1000), true));
        registerDefault(new MaterialProfile("netherite", "下界玄金（默认预设）", 9.8F, 1.10F, 1.30F, 2.00F, 3.0F, Map.of("au", 450, "ancient_debris", 550), true));
        registerDefault(new MaterialProfile("spirit_iron", "灵纹玄铁（默认预设）", 6.2F, 1.25F, 1.45F, 1.35F, 1.8F, Map.of("fe", 860, "spirit_qi", 140), true));
        registerDefault(new MaterialProfile("astral_jade", "星辰寒玉（默认预设）", 8.8F, 1.60F, 1.95F, 1.85F, 3.4F, Map.of("jade_silicate", 650, "astral_essence", 350), true));
    }

    public void registerDefault(MaterialProfile profile) {
        defaultPresets.put(profile.id(), profile);
    }

    public void registerExternalProvider(Function<ItemStack, Optional<MaterialProfile>> provider) {
        if (provider != null) {
            externalProviders.add(provider);
        }
    }

    public Map<String, MaterialProfile> defaultPresets() {
        return Collections.unmodifiableMap(defaultPresets);
    }

    public Optional<MaterialProfile> presetById(String id) {
        return Optional.ofNullable(defaultPresets.get(id));
    }

    /**
     * Resolves a material profile by checking:
     * 1) Explicit {@link MaterialComposition} DataComponent on the stack;
     * 2) Registered specialized forging/metallurgy addon providers;
     * 3) Built-in item-ID / default fallback preset.
     */
    public MaterialProfile resolveProfile(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            MaterialComposition comp = stack.get(EarthCoreComponents.MATERIAL_COMPOSITION.get());
            if (comp != null) {
                MaterialProfile base = defaultPresets.getOrDefault(comp.presetId(), defaultPresets.get("iron"));
                float purityMul = 0.75F + 0.35F * comp.purity();
                float temperBonus = comp.temperStage() * 0.35F;
                return new MaterialProfile(
                        comp.presetId(),
                        base.displayNameZh().replace("（默认预设）", "（精锻成分）"),
                        base.hardness() * purityMul,
                        base.conductivity() * purityMul,
                        base.qiAffinity() * purityMul,
                        base.durabilityMultiplier() * purityMul + comp.temperStage() * 0.08F,
                        base.attackBonus() * purityMul + temperBonus,
                        comp.alloyPermille().isEmpty() ? base.alloyPermille() : comp.alloyPermille(),
                        false
                );
            }
            for (Function<ItemStack, Optional<MaterialProfile>> provider : externalProviders) {
                Optional<MaterialProfile> resolved = provider.apply(stack);
                if (resolved.isPresent()) {
                    return resolved.get();
                }
            }
            Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return resolvePresetByKey(itemId.getPath());
        }
        return defaultPresets.get("iron");
    }

    public MaterialProfile resolvePresetByKey(String path) {
        String p = path == null ? "" : path.toLowerCase();
        if (p.contains("astral") || p.contains("jade") || p.contains("gather_orb")) return defaultPresets.get("astral_jade");
        if (p.contains("spirit_iron") || p.contains("spirit_sword") || p.contains("flying_sword")
                || p.contains("wind_fan") || p.contains("stone_seal") || p.contains("lantern_rod")) return defaultPresets.get("spirit_iron");
        if (p.contains("netherite")) return defaultPresets.get("netherite");
        if (p.contains("diamond")) return defaultPresets.get("diamond");
        if (p.contains("gold")) return defaultPresets.get("gold");
        if (p.contains("copper")) return defaultPresets.get("copper");
        return defaultPresets.get("iron");
    }
}
