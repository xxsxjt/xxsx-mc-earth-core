package com.xxsx.earth.core;

import com.xxsx.earth.core.api.v1.EarthCoreApi;
import com.xxsx.earth.core.api.v1.FluidMixtureApi;
import com.xxsx.earth.core.api.v1.ForgingPresetApi;
import com.xxsx.earth.core.component.SolutionComposition;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EarthCoreThinFoundationTest {

    @Test
    void coreHasNoMisplacedGeologyPlaceholderTools() {
        assertFalse(Files.exists(Path.of("src/main/java/com/xxsx/earth/core/CoreTools.java")),
                "Geology placeholder tools belong in earth_geography, not earth_core");
        assertEquals(32, EarthCoreApi.geology().catalog().size());
    }

    @Test
    void fluidMixtureApiConservesMassDuringMixingDilutionAndDiffusion() {
        SolutionComposition pureSpirit = SolutionComposition.singleSolute(
                "minecraft:water", "earth_xuanxue:spirit_qi", 1000, 1000, 295);
        SolutionComposition diluted = EarthCoreApi.fluids().diluteWithSolvent(pureSpirit, 1000, 293);
        assertEquals(2000, diluted.volumeMb());
        assertEquals(500, diluted.concentrationOf("earth_xuanxue:spirit_qi"));
        assertEquals(294, diluted.temperatureK());

        SolutionComposition bottleA = SolutionComposition.singleSolute(
                "minecraft:water", "earth_xuanxue:spirit_qi", 750, 333, 296);
        SolutionComposition bottleB = SolutionComposition.singleSolute(
                "minecraft:water", "earth_xuanxue:spirit_qi", 250, 333, 292);
        SolutionComposition mixed = EarthCoreApi.fluids().mix(bottleA, bottleB);
        assertEquals(666, mixed.volumeMb());
        assertEquals(500, mixed.concentrationOf("earth_xuanxue:spirit_qi"));

        FluidMixtureApi.DiffusionStep step = EarthCoreApi.fluids().diffuseStep(4, 0, 4);
        assertTrue(step.changed());
        assertEquals(3, step.nextA());
        assertEquals(1, step.nextB());
        assertEquals(4, step.nextA() + step.nextB());

        FluidMixtureApi.DiffusionStep eq = EarthCoreApi.fluids().diffuseStep(2, 1, 4);
        assertFalse(eq.changed());
    }

    @Test
    void forgingPresetApiProvidesGracefulFallbackPresetsWithoutForgingAddon() {
        ForgingPresetApi api = EarthCoreApi.forging();
        assertTrue(api.defaultPresets().size() >= 7);
        ForgingPresetApi.MaterialProfile iron = api.resolvePresetByKey("iron_sword");
        assertEquals("iron", iron.id());
        assertTrue(iron.fromFallbackPreset());
        ForgingPresetApi.MaterialProfile spirit = api.resolvePresetByKey("spirit_iron_sword");
        assertEquals("spirit_iron", spirit.id());
        assertTrue(spirit.qiAffinity() > iron.qiAffinity());
        ForgingPresetApi.MaterialProfile astral = api.resolvePresetByKey("astral_fangtian_halberd");
        assertEquals("astral_jade", astral.id());
        assertTrue(astral.attackBonus() > spirit.attackBonus());
    }
}
