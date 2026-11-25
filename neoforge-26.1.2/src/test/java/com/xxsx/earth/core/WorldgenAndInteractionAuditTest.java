package com.xxsx.earth.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xxsx.earth.core.living.ResidentProfileManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WorldgenAndInteractionAuditTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Set<String> SHALLOW_DEPOSITS = Set.of(
            "bauxite_laterite_deposit",
            "cassiterite_placer_deposit"
    );

    @Test
    void undergroundWorldgenFeaturesStayDeepAndAirSuppressed() throws IOException {
        Path pfRoot = RESOURCES.resolve(Path.of("data", "earth_core", "worldgen", "placed_feature"));
        Path cfRoot = RESOURCES.resolve(Path.of("data", "earth_core", "worldgen", "configured_feature"));
        try (Stream<Path> paths = Files.list(pfRoot)) {
            for (Path pfFile : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String name = pfFile.getFileName().toString().replace(".json", "");
                if ("lode_surface_signal".equals(name)) {
                    continue;
                }
                JsonObject pf = JsonParser.parseString(Files.readString(pfFile, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray placements = pf.getAsJsonArray("placement");
                int chance = -1;
                int count = -1;
                Integer maxY = null;
                for (JsonElement el : placements) {
                    JsonObject obj = el.getAsJsonObject();
                    String type = obj.get("type").getAsString();
                    if ("minecraft:rarity_filter".equals(type)) {
                        chance = obj.get("chance").getAsInt();
                    } else if ("minecraft:count".equals(type)) {
                        count = obj.get("count").getAsInt();
                    } else if ("minecraft:height_range".equals(type)) {
                        maxY = obj.getAsJsonObject("height")
                                .getAsJsonObject("max_inclusive")
                                .get("absolute").getAsInt();
                    }
                }
                assertTrue(chance >= 1 || count >= 1,
                        "placed feature must have rarity_filter or count for " + name);
                assertTrue(maxY != null, "height_range max_inclusive missing for " + name);
                if (!SHALLOW_DEPOSITS.contains(name)) {
                    assertTrue(maxY <= 36, "Underground feature " + name + " max_inclusive must be <= 36, got " + maxY);
                }

                Path cfFile = cfRoot.resolve(name + ".json");
                assertTrue(Files.isRegularFile(cfFile), "Configured feature missing for " + name);
                JsonObject cf = JsonParser.parseString(Files.readString(cfFile, StandardCharsets.UTF_8)).getAsJsonObject();
                assertEquals("minecraft:ore", cf.get("type").getAsString(),
                        "Mineral/strata feature must use minecraft:ore with air discard: " + name);
                double discard = cf.getAsJsonObject("config").get("discard_chance_on_air_exposure").getAsDouble();
                assertTrue(discard >= 0.0 && discard <= 0.85,
                        "discard_chance_on_air_exposure must be in [0.0, 0.85] for " + name + ", got " + discard);
            }
        }
    }

    @Test
    void lodeSurfaceSignalUsesRestrainedThresholds() {
        assertEquals(8, LodeSurfaceSignalFeature.MIN_BLOCKS, "MIN_BLOCKS must be 8");
        assertEquals(1, LodeSurfaceSignalFeature.MAX_SIGNALS, "MAX_SIGNALS must be 1");
        assertEquals(0.28F, LodeSurfaceSignalFeature.SIGNAL_CHANCE, 1.0E-6F, "SIGNAL_CHANCE must be restrained to 28%");
        assertEquals(0.35F, LodeSurfaceSignalFeature.OPEN_SOIL_CHANCE, 1.0E-6F, "Open grass/soil float pebble weight must be 35%");
        assertEquals(0.15F, LodeSurfaceSignalFeature.FOREST_CANOPY_CHANCE, 1.0E-6F, "Forest canopy float pebble weight must be 15%");
        assertEquals(1, LodeSurfaceSignalFeature.MIN_SURFACE_BLOCKS);
        assertEquals(3, LodeSurfaceSignalFeature.MAX_SURFACE_BLOCKS);

        List<LodeSectionScan.Hit> hits = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            hits.add(new LodeSectionScan.Hit(new BlockPos(i, -20, 4), "chalcopyrite_vein"));
        }
        assertTrue(LodeSectionScan.planSignals(hits).isEmpty(),
                "7 blocks (< MIN_BLOCKS=8) must not plan a surface signal");

        for (int i = 0; i < 8; i++) {
            hits.add(new LodeSectionScan.Hit(new BlockPos(3, -30 + i, 5), "magnetite_lens"));
        }
        for (int i = 0; i < 22; i++) {
            hits.add(new LodeSectionScan.Hit(new BlockPos(6, -40 + i, 8), "galena_lens"));
        }
        var planned = LodeSectionScan.planSignals(hits);
        assertEquals(1, planned.size(), "At most MAX_SIGNALS=1 signal per chunk");
        assertEquals("galena_lens", planned.getFirst().lodeId(), "Largest qualifying lode wins");
    }

    @Test
    void lodeSurfaceSignalNeverReplacesGrassOrSoilWithHaloAndSupportsLoosePebbles() throws IOException {
        Identifier grass = Identifier.withDefaultNamespace("grass_block");
        Identifier dirt = Identifier.withDefaultNamespace("dirt");
        Identifier podzol = Identifier.withDefaultNamespace("podzol");
        Identifier clay = Identifier.withDefaultNamespace("clay");
        Identifier dirtPath = Identifier.withDefaultNamespace("dirt_path");
        Identifier farmland = Identifier.withDefaultNamespace("farmland");
        Identifier cobblestone = Identifier.withDefaultNamespace("cobblestone");
        Identifier oakPlanks = Identifier.withDefaultNamespace("oak_planks");
        Identifier stone = Identifier.withDefaultNamespace("stone");
        Identifier gravel = Identifier.withDefaultNamespace("gravel");
        Identifier tuff = Identifier.withDefaultNamespace("tuff");

        assertTrue(SurfaceSignalRules.isForbiddenHaloSoil(grass));
        assertTrue(SurfaceSignalRules.isForbiddenHaloSoil(dirt));
        assertFalse(SurfaceSignalRules.isRockyOutcropBlock(grass),
                "GRASS_BLOCK must never be replaced full-block by *_halo");
        assertFalse(SurfaceSignalRules.isRockyOutcropBlock(dirt),
                "DIRT must never be replaced full-block by *_halo");
        assertFalse(SurfaceSignalRules.isRockyOutcropBlock(podzol),
                "PODZOL must never be replaced full-block by *_halo");
        assertFalse(SurfaceSignalRules.isRockyOutcropBlock(clay),
                "CLAY must never be replaced full-block by *_halo");
        assertFalse(SurfaceSignalRules.isRockyOutcropBlock(dirtPath),
                "DIRT_PATH must never be replaced by *_halo");

        assertTrue(SurfaceSignalRules.isRockyOutcropBlock(stone),
                "Exposed STONE may show rocky outcrop/halo");
        assertTrue(SurfaceSignalRules.isRockyOutcropBlock(gravel),
                "Exposed GRAVEL talus/riverbed may show rocky outcrop/halo");
        assertTrue(SurfaceSignalRules.isRockyOutcropBlock(tuff),
                "Exposed TUFF may show rocky outcrop/halo");

        assertTrue(SurfaceSignalRules.isPebbleHostBlock(grass),
                "GRASS_BLOCK may host loose surface_ore_pebble in top.above()");
        assertTrue(SurfaceSignalRules.isPebbleHostBlock(dirt),
                "DIRT may host loose surface_ore_pebble in top.above()");
        assertFalse(SurfaceSignalRules.isPebbleHostBlock(dirtPath),
                "Village DIRT_PATH must reject surface pebbles");
        assertFalse(SurfaceSignalRules.isPebbleHostBlock(farmland),
                "Village FARMLAND must reject surface pebbles");
        assertFalse(SurfaceSignalRules.isPebbleHostBlock(cobblestone),
                "Constructed COBBLESTONE must reject surface pebbles");
        assertTrue(SurfaceSignalRules.isArtificialOrVillageBlock(dirtPath),
                "Village paths must trigger settlement avoidance");
        assertTrue(SurfaceSignalRules.isArtificialOrVillageBlock(farmland),
                "Farmland must trigger settlement avoidance");
        assertTrue(SurfaceSignalRules.isArtificialOrVillageBlock(oakPlanks),
                "Planks must trigger settlement avoidance");

        // Verify LodeSurfaceSignalFeature source directly enforces Blocks.GRASS_BLOCK / DIRT exclusion & top.above() pebbles
        Path sourceRoot = Path.of("src", "main", "java", "com", "xxsx", "earth", "core");
        String featureSrc = Files.readString(sourceRoot.resolve("LodeSurfaceSignalFeature.java"), StandardCharsets.UTF_8);
        assertTrue(featureSrc.contains("state.is(Blocks.GRASS_BLOCK)") && featureSrc.contains("state.is(Blocks.DIRT)")
                        && featureSrc.contains("state.is(BlockTags.SUBSTRATE_OVERWORLD)") && featureSrc.contains("return false;"),
                "isRockyOutcropSurface must explicitly reject Blocks.GRASS_BLOCK, Blocks.DIRT, and SUBSTRATE_OVERWORLD");
        assertTrue(featureSrc.contains("top.above()") && featureSrc.contains("SURFACE_ORE_PEBBLE"),
                "Soil/grass signals must place loose SURFACE_ORE_PEBBLE in top.above() instead of replacing ground");

        // Verify magnetite lens placed_feature no longer floods 100% of chunks with count: 2
        Path pfRoot = RESOURCES.resolve(Path.of("data", "earth_core", "worldgen", "placed_feature"));
        for (String magName : List.of("poor_magnetite_lens.json", "magnetite_lens.json")) {
            String raw = Files.readString(pfRoot.resolve(magName), StandardCharsets.UTF_8);
            assertTrue(raw.contains("minecraft:rarity_filter"),
                    magName + " must use rarity_filter rather than unconditional per-chunk count");
            assertFalse(raw.contains("\"minecraft:count\""),
                    magName + " must not flood every chunk with minecraft:count");
        }

        // Verify SurfaceOrePebbleBlock mineral mapping and flat VoxelShape
        assertEquals("magnetite", SurfaceSignalRules.pebbleMineralKeyForLode("magnetite_lens").orElseThrow());
        assertEquals("chalcopyrite", SurfaceSignalRules.pebbleMineralKeyForLode("chalcopyrite_vein").orElseThrow());
        assertEquals("cassiterite", SurfaceSignalRules.pebbleMineralKeyForLode("cassiterite_placer_deposit").orElseThrow());
        assertEquals(3.0D / 16.0D, SurfaceSignalRules.PEBBLE_SHAPE.max(net.minecraft.core.Direction.Axis.Y), 1.0E-6D);
    }

    @Test
    void adultUnemployedVillagerBootstrapIsDeterministicAndCoversAroundEightyPercent() {
        int total = 1000;
        int employedAgri = 0;
        int employedMining = 0;
        Map<ResourceKey<VillagerProfession>, Integer> agriCounts = new HashMap<>();
        Map<ResourceKey<VillagerProfession>, Integer> miningCounts = new HashMap<>();
        ResourceKey<VillagerProfession> mason = ResidentProfileManager.vanillaProfessionKey("mason");
        ResourceKey<VillagerProfession> farmer = ResidentProfileManager.vanillaProfessionKey("farmer");
        ResourceKey<VillagerProfession> cartographer = ResidentProfileManager.vanillaProfessionKey("cartographer");
        ResourceKey<VillagerProfession> none = ResidentProfileManager.vanillaProfessionKey("none");
        ResourceKey<VillagerProfession> nitwit = ResidentProfileManager.vanillaProfessionKey("nitwit");

        for (int i = 0; i < total; i++) {
            UUID uuid = new UUID(0x454F4D5F56494C4CL ^ (i * 0x9E3779B97F4A7C15L), i * 1315423911L + 7L);
            long seed = ResidentProfileManager.residentSeed(uuid);

            Optional<ResourceKey<VillagerProfession>> firstAgri =
                    ResidentProfileManager.chooseBootstrapProfession("earth_core:agricultural_village", seed);
            Optional<ResourceKey<VillagerProfession>> secondAgri =
                    ResidentProfileManager.chooseBootstrapProfession("earth_core:agricultural_village", seed);
            assertEquals(firstAgri, secondAgri, "Bootstrap profession choice must be strictly deterministic");

            firstAgri.ifPresent(prof -> {
                agriCounts.merge(prof, 1, Integer::sum);
            });
            if (firstAgri.isPresent()) {
                employedAgri++;
            }

            Optional<ResourceKey<VillagerProfession>> miningProf =
                    ResidentProfileManager.chooseBootstrapProfession("earth_core:mining_settlement", seed);
            miningProf.ifPresent(prof -> miningCounts.merge(prof, 1, Integer::sum));
            if (miningProf.isPresent()) {
                employedMining++;
            }
        }

        double agriRatio = (double) employedAgri / total;
        double miningRatio = (double) employedMining / total;
        assertTrue(agriRatio >= 0.75D && agriRatio <= 0.85D,
                "Agricultural settlement employment ratio should be ~75%-85%, got " + agriRatio);
        assertTrue(miningRatio >= 0.75D && miningRatio <= 0.85D,
                "Mining settlement employment ratio should be ~75%-85%, got " + miningRatio);

        // Industrial/mining settlements emphasize mason/toolsmith/cartographer/cleric
        assertTrue(miningCounts.getOrDefault(mason, 0) > agriCounts.getOrDefault(mason, 0),
                "Mining settlements should assign more masons (miners) than agricultural villages");
        assertTrue(agriCounts.getOrDefault(farmer, 0) > 0,
                "Agricultural villages should assign farmers (warehouse managers)");
        assertTrue(miningCounts.getOrDefault(cartographer, 0) > 0,
                "Mining settlements should assign cartographers (geologists)");
        assertFalse(agriCounts.containsKey(none) || miningCounts.containsKey(none),
                "Employed bootstrap pool must never return VillagerProfession.NONE");
        assertFalse(agriCounts.containsKey(nitwit) || miningCounts.containsKey(nitwit),
                "Employed bootstrap pool must never turn villagers into NITWIT");
    }

    @Test
    void allOreBlockLootTablesIncludeTailingsDustAndMiningTiersAreDisjoint() throws IOException {
        Path lootRoot = RESOURCES.resolve(Path.of("data", "earth_core", "loot_table", "blocks"));
        try (Stream<Path> paths = Files.list(lootRoot)) {
            List<Path> oreFiles = paths.filter(p -> p.getFileName().toString().endsWith("_ore.json")).sorted().toList();
            assertFalse(oreFiles.isEmpty(), "Must find *_ore.json loot tables");
            for (Path oreFile : oreFiles) {
                JsonObject json = JsonParser.parseString(Files.readString(oreFile, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray pools = json.getAsJsonArray("pools");
                assertTrue(pools.size() >= 2, oreFile.getFileName() + " must have primary + gangue pools");
                boolean hasTailings = false;
                for (JsonElement poolEl : pools) {
                    for (JsonElement entryEl : poolEl.getAsJsonObject().getAsJsonArray("entries")) {
                        JsonObject entry = entryEl.getAsJsonObject();
                        if (entry.has("name") && "earth_core:tailings_dust".equals(entry.get("name").getAsString())) {
                            hasTailings = true;
                        }
                    }
                }
                assertTrue(hasTailings, oreFile.getFileName() + " must drop earth_core:tailings_dust");
            }
        }

        Path tagsRoot = RESOURCES.resolve(Path.of("data", "minecraft", "tags", "block"));
        JsonArray stoneValues = JsonParser.parseString(
                Files.readString(tagsRoot.resolve("needs_stone_tool.json"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("values");
        JsonArray ironValues = JsonParser.parseString(
                Files.readString(tagsRoot.resolve("needs_iron_tool.json"), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("values");
        Set<String> stoneSet = new HashSet<>();
        for (JsonElement el : stoneValues) {
            stoneSet.add(el.getAsString());
        }
        for (JsonElement el : ironValues) {
            String blockId = el.getAsString();
            assertFalse(stoneSet.contains(blockId),
                    "Block must not be duplicated in both needs_stone_tool and needs_iron_tool: " + blockId);
        }
    }

    @Test
    void machineGuiHasNoManualButtonAndCrankInstructionsUseWorldInteraction() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java", "com", "xxsx", "earth", "core");
        String screen = Files.readString(sourceRoot.resolve(Path.of("client", "ProcessingMachineScreen.java")),
                StandardCharsets.UTF_8);
        assertFalse(screen.contains("manualButton"), "The processing-machine GUI must not own a manual button");
        assertFalse(screen.contains("triggerManualStep"), "The processing-machine GUI must not send manual-step clicks");
        assertFalse(screen.contains("machine.manual.button"), "The obsolete manual-button language key must not be used");

        String menu = Files.readString(sourceRoot.resolve("ProcessingMachineMenu.java"), StandardCharsets.UTF_8);
        assertFalse(menu.contains("BUTTON_MANUAL_STEP"), "Manual drive must not use an inventory/menu click id");

        String crank = Files.readString(sourceRoot.resolve("HandCrankBlock.java"), StandardCharsets.UTF_8);
        assertTrue(crank.contains("class HandCrankBlock extends Block"), "hand_crank must be a placeable world block");
        assertTrue(crank.contains("getStateForPlacement"), "hand_crank must be mountable against a machine");
        assertTrue(crank.contains("machine.applyCrankTurn(HandCrankItem.CRANK_WORK_TICKS)"), "right-clicking the mounted crank must drive its adjacent machine");
        assertTrue(crank.contains("canSurvive"), "the crank must remain attached to its machine");
        String machine = Files.readString(sourceRoot.resolve("ProcessingMachineBlock.java"), StandardCharsets.UTF_8);
        assertTrue(machine.contains("Ae2CrankCompat.isAe2CrankItem(stack)"), "AE2 crank compatibility must remain supported");
        String registration = Files.readString(sourceRoot.resolve("EarthOnMinecraft.java"), StandardCharsets.UTF_8);
        assertTrue(registration.contains("DeferredBlock<HandCrankBlock> HAND_CRANK_BLOCK"));
        assertTrue(registration.contains("BLOCKS.registerBlock(\"hand_crank\""));

        Path langRoot = RESOURCES.resolve(Path.of("assets", "earth_core", "lang"));
        JsonObject zh = JsonParser.parseString(
                Files.readString(langRoot.resolve("zh_cn.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject en = JsonParser.parseString(
                Files.readString(langRoot.resolve("en_us.json"), StandardCharsets.UTF_8)).getAsJsonObject();

        assertFalse(zh.has("screen.earth_core.machine.manual.button"));
        assertFalse(en.has("screen.earth_core.machine.manual.button"));
        assertFalse(zh.get("tooltip.earth_core.hand_crank.0").getAsString().contains("直接右键"));
        assertFalse(en.get("tooltip.earth_core.hand_crank.0").getAsString().contains("right-click Ball Mill"));
        assertTrue(zh.get("tooltip.earth_core.hand_crank.0").getAsString().contains("曲柄方块"));
        assertTrue(en.get("tooltip.earth_core.hand_crank.0").getAsString().contains("crank block"));
        for (JsonObject lang : List.of(zh, en)) {
            String tooltip = lang.get("screen.earth_core.machine.manual.tooltip").getAsString();
            assertTrue(tooltip.contains("hand_crank"));
            assertTrue(tooltip.contains("ae2:crank"));
            assertFalse(tooltip.contains("screen.earth_core.machine.manual.button"));
        }
        assertTrue(zh.get("tooltip.earth_core.machine.crank_hint").getAsString().contains("hand_crank"));
        assertTrue(zh.get("tooltip.earth_core.machine.crank_hint").getAsString().contains("ae2:crank"));
        assertTrue(en.get("tooltip.earth_core.machine.crank_hint").getAsString().contains("hand_crank"));
        assertTrue(en.get("tooltip.earth_core.machine.crank_hint").getAsString().contains("ae2:crank"));
        assertFalse(zh.get("screen.earth_core.machine.manual_wait").getAsString().contains("【手摇】"));
        assertFalse(en.get("screen.earth_core.machine.manual_wait").getAsString().contains("[Crank]"));
        assertFalse(zh.get("screen.earth_core.machine.kinetic_wait").getAsString().contains("点击【手摇】"));
        assertFalse(en.get("screen.earth_core.machine.kinetic_wait").getAsString().contains("[Crank]"));
    }

    @Test
    void machineGuiProfileAndScreenLayoutAreConsistent() throws IOException {
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            assertDoesNotThrow(() -> MachineGuiProfile.forKind(kind), "GUI profile must be registry-independent for " + kind);
        }
        for (ProcessingMachineBlock.Kind kind : List.of(ProcessingMachineBlock.Kind.CRUSHER,
                ProcessingMachineBlock.Kind.BALL_MILL, ProcessingMachineBlock.Kind.SIEVE)) {
            MachineGuiProfile profile = MachineGuiProfile.forKind(kind);
            assertEquals(0, profile.secondarySlotCount(), kind + " must not expose secondary inputs");
            assertFalse(profile.usesReagentSlot(), kind + " must not expose a reagent slot");
            assertFalse(profile.usesTempGauge(), kind + " must not show a temperature gauge");
        }
        MachineGuiProfile furnace = MachineGuiProfile.forKind(ProcessingMachineBlock.Kind.REDUCTION_FURNACE);
        assertTrue(furnace.usesReagentSlot(), "Reduction furnace must expose its coke slot");
        assertTrue(furnace.usesTempGauge(), "Reduction furnace must show temperature");
        assertEquals(4, MachineGuiProfile.forKind(ProcessingMachineBlock.Kind.CHEMICAL_REACTOR).visibleOutputSlots());
        assertEquals(4, MachineGuiProfile.forKind(ProcessingMachineBlock.Kind.ELECTROLYTIC_CELL).visibleOutputSlots());
        assertEquals(6, MachineGuiProfile.forKind(ProcessingMachineBlock.Kind.CRUSHER).visibleOutputSlots());

        int[][] sixOutputs = ProcessingMachineMenu.outputSlotPositions(
                MachineGuiProfile.forKind(ProcessingMachineBlock.Kind.CRUSHER));
        assertEquals(7, sixOutputs.length, "All physical outputs must have stable coordinates");
        for (int i = 0; i < 6; i++) {
            for (int j = i + 1; j < 6; j++) {
                int dx = sixOutputs[i][0] - sixOutputs[j][0];
                int dy = sixOutputs[i][1] - sixOutputs[j][1];
                assertTrue(dx * dx + dy * dy >= 18 * 18,
                        "Six visible output slots must be at least 18px apart");
            }
        }

        String screen = Files.readString(Path.of("src", "main", "java", "com", "xxsx", "earth", "core",
                "client", "ProcessingMachineScreen.java"), StandardCharsets.UTF_8);
        assertTrue(screen.contains("this.imageHeight = 188"));
        for (ProcessingMachineBlock.ProcessFamily family : ProcessingMachineBlock.ProcessFamily.values()) {
            assertTrue(screen.contains("case " + family.name() + " ->"),
                    "Screen must draw the " + family + " process core");
        }
    }

    @Test
    void medicineDurationUsesIntegerFormattingForIntegerSeconds() throws IOException {
        String source = Files.readString(Path.of("src", "main", "java", "com", "xxsx", "earth", "core",
                "EarthMedicalAndWeaponSupport.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("duration %ds"));
        assertFalse(source.contains("duration %.1fs"));
    }

    @Test
    void processingMachineShapesFollowModelsAndFacing() {
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            VoxelShape northShape = ProcessingMachineShapes.get(kind, net.minecraft.core.Direction.NORTH);
            assertFalse(northShape.isEmpty(), kind + " needs a non-empty model shape");
            assertTrue(northShape.toAabbs().size() > 1, kind + " needs a multi-part model shape");
            assertTrue(northShape.max(net.minecraft.core.Direction.Axis.Y) <= 1.0D,
                    kind + " shape must stay within its block vertically");
            assertFalse(northShape.bounds().minX == 0.0D && northShape.bounds().minY == 0.0D
                            && northShape.bounds().minZ == 0.0D && northShape.bounds().maxX == 1.0D
                            && northShape.bounds().maxY == 1.0D && northShape.bounds().maxZ == 1.0D,
                    kind + " must not use a full-cube selection/collision shape");

            VoxelShape southShape = ProcessingMachineShapes.get(kind, net.minecraft.core.Direction.SOUTH);
            assertEquals(northShape.toAabbs().stream().mapToDouble(box ->
                            (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ)).sum(),
                    southShape.toAabbs().stream().mapToDouble(box ->
                            (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ)).sum(),
                    1.0E-8, kind + " rotation must preserve model volume");
            assertEquals(northShape.min(net.minecraft.core.Direction.Axis.Y),
                    southShape.min(net.minecraft.core.Direction.Axis.Y));
            assertEquals(northShape.max(net.minecraft.core.Direction.Axis.Y),
                    southShape.max(net.minecraft.core.Direction.Axis.Y));
        }

        VoxelShape north = ProcessingMachineShapes.get(
                ProcessingMachineBlock.Kind.DISTILLATION_COLUMN, net.minecraft.core.Direction.NORTH);
        VoxelShape south = ProcessingMachineShapes.get(
                ProcessingMachineBlock.Kind.DISTILLATION_COLUMN, net.minecraft.core.Direction.SOUTH);
        assertTrue(containsPoint(north, 15.5D, 10.0D, 8.0D),
                "North-facing side_draw must match the model");
        assertTrue(containsPoint(south, 0.5D, 10.0D, 8.0D),
                "South-facing side_draw must rotate 180 degrees");
    }

    private static boolean containsPoint(VoxelShape shape, double x, double y, double z) {
        var point = new net.minecraft.world.phys.Vec3(x / 16, y / 16, z / 16);
        return shape.toAabbs().stream().anyMatch(box -> box.contains(point));
    }
}
