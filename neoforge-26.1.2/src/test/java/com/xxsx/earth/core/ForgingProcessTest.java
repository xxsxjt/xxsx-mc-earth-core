package com.xxsx.earth.core;

import com.xxsx.earth.core.component.ForgingProcess;
import com.xxsx.earth.core.component.MetalBatch;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgingProcessTest {
    @Test
    void temperatureFallsByElapsedSecondsAndCannotCrossAmbient() {
        ForgingProcess p = ForgingProcess.heated(100L, 1240.0);
        assertEquals(1240.0, p.currentTemperatureK(100L, 293.0), 0.001);
        assertEquals(840.0, p.currentTemperatureK(1100L, 293.0), 0.001);
        assertEquals(293.0, p.currentTemperatureK(10000L, 293.0), 0.001);
    }

    @Test
    void nearbyHeatPreventsImmediateColdDrop() {
        ForgingProcess p = ForgingProcess.heated(0L, 900.0);
        assertTrue(p.currentTemperatureK(20L, 1300.0) > 900.0);
    }

    @Test
    void foldAndTargetStateAreBoundedAndSerializable() {
        ForgingProcess p = ForgingProcess.DEFAULT.withTarget(ForgingProcess.TargetForm.SWORD_BLADE);
        assertEquals(ForgingProcess.TargetForm.SWORD_BLADE, p.targetForm());
        assertEquals(12, new ForgingProcess(1, 0, 1200, 8, 12, 1,
                p.targetForm(), 1, ForgingProcess.QuenchState.WATER_QUENCHED,
                ForgingProcess.TemperState.TEMPERED, 0, 0, 1).foldCount());
    }

    @Test
    void metalSubstratePotentialRewardsCleanIronAndPunishesSlag() {
        MetalBatch clean = MetallurgyAssay.createIronCarbonBatch(0.010, 0.0002, 0.0002, 0.0005);
        MetalBatch dirty = MetallurgyAssay.createIronCarbonBatch(0.010, 0.0040, 0.0030, 0.060);
        assertTrue(clean.substratePotential() > dirty.substratePotential());
    }
}
