package com.xxsx.earth.core;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xxsx.earth.core.api.v1.EarthCoreApi;
import com.xxsx.earth.core.component.Grade;
import com.xxsx.earth.core.component.MaterialBatch;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EarthCoreThinFoundationTest {
    @Test
    void thermalAndMaterialApisAreConsistent() {
        assertEquals(273.15D, EarthCoreApi.thermal().toKelvin(0.0D), 1e-6);
        assertEquals(15.0D, EarthCoreApi.thermal().toCelsius(288.15D), 1e-6);
        assertTrue(EarthCoreApi.thermal().ambientKelvinAt(120, 15.0D) < EarthCoreApi.thermal().ambientKelvinAt(63, 15.0D));
        assertTrue(EarthCoreApi.materials().find("iron").isPresent());
        assertEquals(32, EarthCoreApi.geology().catalog().size());

        MaterialBatch batch = new MaterialBatch("iron", Grade.REFINED, 8750, 293);
        assertEquals(0.875D, batch.purityFraction(), 1e-6);
    }

    @Test
    void allAssetsLangLoreAndAgplLicenseExist() throws IOException {
        Path assets = Path.of("src/main/resources/assets/earth_core");
        Path data = Path.of("src/main/resources/data/earth_core");

        JsonObject zh = JsonParser.parseString(Files.readString(assets.resolve("lang/zh_cn.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject en = JsonParser.parseString(Files.readString(assets.resolve("lang/en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(zh.keySet(), en.keySet(), "zh_cn and en_us must match 1:1");

        for (DepositSpec spec : DepositSpec.ALL) {
            assertTrue(Files.exists(assets.resolve("textures/block/" + spec.id() + ".png")), "Missing texture: " + spec.id());
            assertTrue(Files.exists(assets.resolve("blockstates/" + spec.id() + ".json")), "Missing blockstate: " + spec.id());
            assertTrue(Files.exists(data.resolve("loot_table/blocks/" + spec.id() + ".json")), "Missing loot: " + spec.id());
            assertTrue(zh.has("block.earth_core." + spec.id()), "Missing zh lang: " + spec.id());
        }

        String toml = Files.readString(Path.of("src/main/resources/META-INF/neoforge.mods.toml"), StandardCharsets.UTF_8);
        assertTrue(toml.contains("modId=\"earth_core\""));
        assertTrue(toml.contains("license=\"AGPL-3.0-only\""));
        String lic = Files.readString(Path.of("../LICENSE"), StandardCharsets.UTF_8);
        assertTrue(lic.contains("GNU AFFERO GENERAL PUBLIC LICENSE"));
    }
}
