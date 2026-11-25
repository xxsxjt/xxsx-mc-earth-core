package com.xxsx.earth.core.living;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.xxsx.earth.core.EarthOnMinecraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

public final class SettlementDataManager {
    private static final String NAME_POOL_PATH = "settlements/name_pools";
    private static final String ROLE_PATH = "settlements/resident_roles";
    private static final String PROFILE_PATH = "settlements/profiles";

    private static final Map<Identifier, NamePool> FALLBACK_NAME_POOLS = Map.of(
            EarthOnMinecraft.id("common"), new NamePool(List.of(
                    "resident.earth_core.name.lin_chuan",
                    "resident.earth_core.name.chen_lan",
                    "resident.earth_core.name.zhou_yan",
                    "resident.earth_core.name.su_he",
                    "resident.earth_core.name.nora_hale",
                    "resident.earth_core.name.elias_ward",
                    "resident.earth_core.name.maya_singh",
                    "resident.earth_core.name.luca_moretti"
            ), 1)
    );

    private static final Map<Identifier, ResidentRole> BASE_ROLES = Map.of(
            EarthOnMinecraft.id("miner"), new ResidentRole(
                    "resident.earth_core.role.miner", "resident.earth_core.identity.worker",
                    List.of("minecraft:mason"), List.of(
                            "earth_core:mining_settlement",
                            "earth_core:resource_village",
                            "earth_core:industrial_town",
                            "earth_core:mechanized_city"), 3,
                    List.of(
                            trade(1, "minecraft:charcoal", 8, "earth_core:magnetite_chunk", 4, 16, 2),
                            trade(1, "minecraft:coal", 8, "earth_core:chalcopyrite_chunk", 4, 16, 2),
                            trade(2, "earth_core:coke", 4, "earth_core:iron_concentrate", 3, 12, 10),
                            trade(2, "earth_core:magnetite_dust", 6, "earth_core:firebrick", 4, 12, 10),
                            trade(3, "earth_core:iron_concentrate", 4, "earth_core:steel", 2, 8, 20),
                            trade(3, "earth_core:coke", 6, "earth_core:steel_pickaxe", 1, 6, 20))),
            EarthOnMinecraft.id("geologist"), new ResidentRole(
                    "resident.earth_core.role.geologist", "resident.earth_core.identity.specialist",
                    List.of("minecraft:cartographer", "minecraft:librarian"), List.of(
                            "earth_core:primitive_hamlet",
                            "earth_core:agricultural_village",
                            "earth_core:mining_settlement",
                            "earth_core:resource_village",
                            "earth_core:industrial_town",
                            "earth_core:mechanized_city"), 2,
                    List.of(
                            trade(1, "minecraft:charcoal", 6, "earth_core:field_geology_notebook", 1, 8, 2),
                            trade(1, "minecraft:coal", 6, "earth_core:geology_hammer", 1, 8, 4),
                            trade(2, "earth_core:calcite_dust", 8, "earth_core:lime_dust", 6, 12, 10),
                            trade(2, "earth_core:sulfur_dust", 6, "earth_core:copper_concentrate", 3, 12, 10),
                            trade(3, "earth_core:coke", 6, "earth_core:ore_scanner", 1, 6, 20),
                            trade(3, "earth_core:copper_concentrate", 4, "earth_core:auriferous_quartz_chunk", 3, 8, 20))),
            EarthOnMinecraft.id("mechanic"), new ResidentRole(
                    "resident.earth_core.role.mechanic", "resident.earth_core.identity.technician",
                    List.of("minecraft:armorer", "minecraft:toolsmith", "minecraft:weaponsmith"), List.of(
                            "earth_core:mining_settlement",
                            "earth_core:resource_village",
                            "earth_core:industrial_town",
                            "earth_core:mechanized_city",
                            "earth_core:mechanized_town"), 3,
                    List.of(
                            trade(1, "earth_core:coke", 4, "earth_core:firebrick", 4, 12, 2),
                            trade(1, "earth_core:lime_dust", 6, "earth_core:wrought_iron", 2, 12, 4),
                            trade(2, "earth_core:iron_concentrate", 4, "earth_core:steel", 2, 12, 10),
                            trade(2, "earth_core:coke", 6, "earth_core:rubber_gasket", 4, 12, 10),
                            trade(3, "earth_core:sulfur_dust", 6, "earth_core:iron_catalyst", 1, 8, 20),
                            trade(3, "earth_core:steel", 3, "earth_core:steel_pickaxe", 1, 8, 20))),
            EarthOnMinecraft.id("electrician"), new ResidentRole(
                    "resident.earth_core.role.electrician", "resident.earth_core.identity.technician",
                    List.of("minecraft:toolsmith", "minecraft:cartographer"), List.of(
                            "earth_core:industrial_town",
                            "earth_core:mechanized_city",
                            "earth_core:mechanized_town"), 2,
                    List.of(
                            trade(1, "earth_core:copper_concentrate", 4, "earth_core:copper_wire", 6, 12, 2),
                            trade(1, "earth_core:coke", 4, "earth_core:ceramic_insulator", 4, 12, 2),
                            trade(2, "earth_core:sulfur_dust", 6, "earth_core:simple_battery_cell", 2, 12, 10),
                            trade(2, "earth_core:salt_dust", 8, "earth_core:copper_power_cable", 4, 12, 10),
                            trade(3, "earth_core:copper_concentrate", 6, "earth_core:ore_scanner", 1, 6, 20),
                            trade(3, "earth_core:sulfur_dust", 8, "earth_core:vanadium_catalyst", 1, 8, 20))),
            EarthOnMinecraft.id("water_operator"), new ResidentRole(
                    "resident.earth_core.role.water_operator", "resident.earth_core.identity.operator",
                    List.of("minecraft:cleric", "minecraft:fisherman"), List.of(), 2,
                    List.of(
                            trade(1, "minecraft:charcoal", 8, "earth_core:activated_carbon", 4, 12, 2),
                            trade(1, "earth_core:salt_dust", 6, "earth_core:lime_dust", 4, 12, 2),
                            trade(2, "earth_core:lime_dust", 6, "earth_core:softened_water", 4, 12, 10),
                            trade(2, "earth_core:sulfur_dust", 6, "earth_core:sulfuric_acid", 2, 10, 10),
                            trade(3, "earth_core:salt_dust", 8, "earth_core:sodium_hydroxide", 3, 8, 20),
                            trade(3, "earth_core:sulfur_dust", 8, "earth_core:vanadium_catalyst", 1, 6, 20))),
            EarthOnMinecraft.id("warehouse_manager"), new ResidentRole(
                    "resident.earth_core.role.warehouse_manager", "resident.earth_core.identity.merchant",
                    List.of("minecraft:farmer", "minecraft:shepherd", "minecraft:butcher", "minecraft:leatherworker", "minecraft:fletcher"),
                    List.of(), 3,
                    List.of(
                            trade(1, "minecraft:oak_log", 8, "earth_core:timber_beam", 4, 16, 2),
                            trade(1, "minecraft:bread", 6, "minecraft:charcoal", 8, 16, 2),
                            trade(2, "minecraft:coal", 8, "earth_core:coke", 4, 12, 10),
                            trade(2, "earth_core:rye_bread", 6, "earth_core:firebrick", 4, 12, 10),
                            trade(3, "earth_core:coke", 6, "earth_core:conveyor_drive", 2, 8, 20),
                            trade(3, "earth_core:iron_concentrate", 4, "earth_core:geology_hammer", 1, 8, 20))),
            EarthOnMinecraft.id("resident"), new ResidentRole(
                    "resident.earth_core.role.resident", "resident.earth_core.identity.resident",
                    List.of("minecraft:none", "minecraft:nitwit"), List.of(), 4, List.of()),
            EarthOnMinecraft.id("arcane_resident"), new ResidentRole(
                    "resident.earth_core.role.arcane_resident", "resident.earth_core.identity.arcane",
                    List.of("earth_magic:arcane_settler"), List.of("earth_core:arcane_settlement"), 1, List.of()),
            EarthOnMinecraft.id("cultivation_resident"), new ResidentRole(
                    "resident.earth_core.role.cultivation_resident", "resident.earth_core.identity.cultivator",
                    List.of("earth_xuanxue:cultivation_settler"), List.of("earth_core:cultivation_settlement"), 1, List.of())
    );

    private static final Map<Identifier, ResidentRole> FALLBACK_ADDON_ROLES = Map.of(
            EarthOnMinecraft.id("fantasy_hedge_witch"), new ResidentRole(
                    "resident.earth_core.role.fantasy_hedge_witch", "resident.earth_core.identity.arcane",
                    List.of("earth_magic:arcane_settler"), List.of("earth_core:arcane_settlement"), 1, List.of()),
            EarthOnMinecraft.id("fantasy_goblin_appraiser"), new ResidentRole(
                    "resident.earth_core.role.fantasy_goblin_appraiser", "resident.earth_core.identity.arcane",
                    List.of("earth_magic:arcane_settler"), List.of("earth_core:arcane_settlement"), 1, List.of()),
            EarthOnMinecraft.id("fantasy_academy_researcher"), new ResidentRole(
                    "resident.earth_core.role.fantasy_academy_researcher", "resident.earth_core.identity.arcane",
                    List.of("earth_magic:arcane_settler"), List.of("earth_core:arcane_settlement"), 1, List.of()),
            EarthOnMinecraft.id("xuanhuan_wandering_merchant"), new ResidentRole(
                    "resident.earth_core.role.xuanhuan_wandering_merchant", "resident.earth_core.identity.cultivator",
                    List.of("earth_xuanxue:cultivation_settler"), List.of("earth_core:cultivation_settlement"), 1, List.of()),
            EarthOnMinecraft.id("xuanhuan_spirit_spring_keeper"), new ResidentRole(
                    "resident.earth_core.role.xuanhuan_spirit_spring_keeper", "resident.earth_core.identity.cultivator",
                    List.of("earth_xuanxue:cultivation_settler"), List.of("earth_core:cultivation_settlement"), 1, List.of()),
            EarthOnMinecraft.id("xuanhuan_sect_steward"), new ResidentRole(
                    "resident.earth_core.role.xuanhuan_sect_steward", "resident.earth_core.identity.cultivator",
                    List.of("earth_xuanxue:cultivation_settler"), List.of("earth_core:cultivation_settlement"), 1, List.of())
    );

    private static final Map<Identifier, ResidentRole> FALLBACK_ROLES = fallbackRoles();

    private static Map<Identifier, ResidentRole> fallbackRoles() {
        Map<Identifier, ResidentRole> values = new LinkedHashMap<>(BASE_ROLES);
        values.putAll(FALLBACK_ADDON_ROLES);
        return Map.copyOf(values);
    }

    private static final Map<Identifier, SettlementProfile> FALLBACK_PROFILES = Map.ofEntries(
            Map.entry(EarthOnMinecraft.id("primitive_hamlet"), new SettlementProfile(
                    1,
                    "settlement.earth_core.profile.primitive_hamlet",
                    "settlement.earth_core.scale.hamlet",
                    "settlement.earth_core.technology.primitive",
                    List.of("settlement.earth_core.name.ash_hollow", "settlement.earth_core.name.claybank"),
                    List.of("settlement.earth_core.industry.foraging", "settlement.earth_core.industry.pit_firing"),
                    List.of("settlement.earth_core.material.firewood", "settlement.earth_core.material.clay"),
                    List.of("settlement.earth_core.material.charcoal", "settlement.earth_core.material.ceramics"), 4)),
            Map.entry(EarthOnMinecraft.id("mining_settlement"), new SettlementProfile(
                    2,
                    "settlement.earth_core.profile.mining_settlement",
                    "settlement.earth_core.scale.village",
                    "settlement.earth_core.technology.mining",
                    List.of("settlement.earth_core.name.coppervein", "settlement.earth_core.name.slagbrook"),
                    List.of("settlement.earth_core.industry.ore_dressing", "settlement.earth_core.industry.bloomery_smelting"),
                    List.of("settlement.earth_core.material.charcoal", "settlement.earth_core.material.support_timber"),
                    List.of("settlement.earth_core.material.ore_samples", "settlement.earth_core.material.wrought_iron"), 3)),
            Map.entry(EarthOnMinecraft.id("industrial_town"), new SettlementProfile(
                    3,
                    "settlement.earth_core.profile.industrial_town",
                    "settlement.earth_core.scale.town",
                    "settlement.earth_core.technology.industrial",
                    List.of("settlement.earth_core.name.cokevale", "settlement.earth_core.name.shaftborough"),
                    List.of("settlement.earth_core.industry.metallurgy", "settlement.earth_core.industry.coking"),
                    List.of("settlement.earth_core.material.coke", "settlement.earth_core.material.flux"),
                    List.of("settlement.earth_core.material.steel", "settlement.earth_core.material.machine_parts"), 2)),
            Map.entry(EarthOnMinecraft.id("mechanized_city"), new SettlementProfile(
                    4,
                    "settlement.earth_core.profile.mechanized_city",
                    "settlement.earth_core.scale.city",
                    "settlement.earth_core.technology.grid_city",
                    List.of("settlement.earth_core.name.gridpoint", "settlement.earth_core.name.volt_harbor"),
                    List.of("settlement.earth_core.industry.power_grid", "settlement.earth_core.industry.chemical_engineering"),
                    List.of("settlement.earth_core.material.reagents", "settlement.earth_core.material.crushed_ore"),
                    List.of("settlement.earth_core.material.catalysts", "settlement.earth_core.material.cable"), 1)),
            Map.entry(EarthOnMinecraft.id("agricultural_village"), new SettlementProfile(
                    1,
                    "settlement.earth_core.profile.agricultural_village",
                    "settlement.earth_core.scale.village",
                    "settlement.earth_core.technology.agricultural",
                    List.of("settlement.earth_core.name.riverbend", "settlement.earth_core.name.greenfield"),
                    List.of("settlement.earth_core.industry.agriculture", "settlement.earth_core.industry.irrigation"),
                    List.of("settlement.earth_core.material.tools", "settlement.earth_core.material.fertilizer"),
                    List.of("settlement.earth_core.material.food", "settlement.earth_core.material.fiber"), 4)),
            Map.entry(EarthOnMinecraft.id("resource_village"), new SettlementProfile(
                    2,
                    "settlement.earth_core.profile.resource_village",
                    "settlement.earth_core.scale.village",
                    "settlement.earth_core.technology.mechanized",
                    List.of("settlement.earth_core.name.bluestone", "settlement.earth_core.name.redcliff"),
                    List.of("settlement.earth_core.industry.mining", "settlement.earth_core.industry.quarrying"),
                    List.of("settlement.earth_core.material.support_timber", "settlement.earth_core.material.lighting"),
                    List.of("settlement.earth_core.material.ore_samples", "settlement.earth_core.material.aggregate"), 3)),
            Map.entry(EarthOnMinecraft.id("mechanized_town"), new SettlementProfile(
                    4,
                    "settlement.earth_core.profile.mechanized_town",
                    "settlement.earth_core.scale.town",
                    "settlement.earth_core.technology.mechanized",
                    List.of("settlement.earth_core.name.pine_ridge_works", "settlement.earth_core.name.ironbridge"),
                    List.of("settlement.earth_core.industry.manufacturing", "settlement.earth_core.industry.logistics"),
                    List.of("settlement.earth_core.material.coal", "settlement.earth_core.material.lubricant"),
                    List.of("settlement.earth_core.material.machine_parts", "settlement.earth_core.material.cable"), 2)),
            Map.entry(EarthOnMinecraft.id("arcane_settlement"), new SettlementProfile(
                    2,
                    "settlement.earth_core.profile.arcane_settlement",
                    "settlement.earth_core.scale.village",
                    "settlement.earth_core.technology.arcane",
                    List.of("settlement.earth_core.name.witchlight", "settlement.earth_core.name.runehollow"),
                    List.of("settlement.earth_core.industry.arcane_research", "settlement.earth_core.industry.alchemy"),
                    List.of("settlement.earth_core.material.arcane_focus", "settlement.earth_core.material.crystal"),
                    List.of("settlement.earth_core.material.runes", "settlement.earth_core.material.potions"), 1)),
            Map.entry(EarthOnMinecraft.id("cultivation_settlement"), new SettlementProfile(
                    2,
                    "settlement.earth_core.profile.cultivation_settlement",
                    "settlement.earth_core.scale.village",
                    "settlement.earth_core.technology.cultivation",
                    List.of("settlement.earth_core.name.cloudstep", "settlement.earth_core.name.springvale"),
                    List.of("settlement.earth_core.industry.cultivation", "settlement.earth_core.industry.herbalism"),
                    List.of("settlement.earth_core.material.spirit_grass", "settlement.earth_core.material.spirit_springs"),
                    List.of("settlement.earth_core.material.talisman_paper", "settlement.earth_core.material.spirit_stones"), 1))
    );

    private static volatile Map<Identifier, NamePool> namePools = FALLBACK_NAME_POOLS;
    private static volatile Map<Identifier, ResidentRole> roles = FALLBACK_ROLES;
    private static volatile Map<Identifier, SettlementProfile> profiles = FALLBACK_PROFILES;

    private SettlementDataManager() {
    }

    public static void registerReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(EarthOnMinecraft.id("settlement_name_pools"), new NamePoolReloadListener());
        event.addListener(EarthOnMinecraft.id("settlement_resident_roles"), new RoleReloadListener());
        event.addListener(EarthOnMinecraft.id("settlement_profiles"), new ProfileReloadListener());
    }

    public static String chooseNameKey(long seed) {
        List<Map.Entry<Identifier, NamePool>> pools = sortedEntries(namePools);
        NamePool pool = weightedPick(pools, entry -> entry.getValue().weight(), seed).getValue();
        int index = Math.floorMod((int) mix(seed ^ 0x5DEECE66DL), pool.nameKeys().size());
        return pool.nameKeys().get(index);
    }

    public static RoleSelection chooseRole(String professionId, String settlementProfileId, long seed) {
        List<Map.Entry<Identifier, ResidentRole>> all = sortedEntries(roles);
        List<Map.Entry<Identifier, ResidentRole>> professionMatches = all.stream()
                .filter(entry -> entry.getValue().vanillaProfessions().contains(professionId))
                .toList();
        List<Map.Entry<Identifier, ResidentRole>> exact = professionMatches.stream()
                .filter(entry -> entry.getValue().settlementProfiles().isEmpty()
                        || entry.getValue().settlementProfiles().contains(settlementProfileId))
                .toList();
        List<Map.Entry<Identifier, ResidentRole>> profileMatches = all.stream()
                .filter(entry -> entry.getValue().settlementProfiles().isEmpty()
                        || entry.getValue().settlementProfiles().contains(settlementProfileId))
                .toList();
        List<Map.Entry<Identifier, ResidentRole>> candidates = !exact.isEmpty()
                ? exact
                : !professionMatches.isEmpty() ? professionMatches : profileMatches;
        if (candidates.isEmpty()) {
            candidates = all;
        }
        Map.Entry<Identifier, ResidentRole> selected = weightedPick(candidates, entry -> entry.getValue().weight(), seed);
        return new RoleSelection(selected.getKey(), selected.getValue());
    }

    public static ProfileSelection chooseProfile(long seed) {
        List<Map.Entry<Identifier, SettlementProfile>> all = sortedEntries(profiles);
        Map.Entry<Identifier, SettlementProfile> selected = weightedPick(all, entry -> entry.getValue().weight(), seed);
        return new ProfileSelection(selected.getKey(), selected.getValue());
    }

    public static String chooseSettlementNameKey(Identifier profileId, long seed) {
        SettlementProfile profile = profile(profileId);
        int index = Math.floorMod((int) mix(seed), profile.nameKeys().size());
        return profile.nameKeys().get(index);
    }

    public static ResidentRole role(Identifier id) {
        return roles.getOrDefault(id, FALLBACK_ROLES.get(EarthOnMinecraft.id("resident")));
    }

    public static SettlementProfile profile(Identifier id) {
        return profiles.getOrDefault(id, FALLBACK_PROFILES.get(EarthOnMinecraft.id("primitive_hamlet")));
    }

    public static Identifier canonicalProfileForTier(int tier) {
        int clamped = Math.max(1, Math.min(4, tier));
        return switch (clamped) {
            case 1 -> EarthOnMinecraft.id("primitive_hamlet");
            case 2 -> EarthOnMinecraft.id("mining_settlement");
            case 3 -> EarthOnMinecraft.id("industrial_town");
            default -> EarthOnMinecraft.id("mechanized_city");
        };
    }

    public static int defaultTierForProfile(Identifier id) {
        if (id == null) {
            return 1;
        }
        SettlementProfile loaded = profiles.get(id);
        if (loaded != null) {
            return loaded.tier();
        }
        return switch (id.getPath()) {
            case "primitive_hamlet", "agricultural_village" -> 1;
            case "mining_settlement", "resource_village", "arcane_settlement", "cultivation_settlement" -> 2;
            case "industrial_town" -> 3;
            case "mechanized_city", "mechanized_town" -> 4;
            default -> 1;
        };
    }

    public static String tierProgressKey(int tier, Map<String, Integer> facilityCounts) {
        int clamped = Math.max(1, Math.min(4, tier));
        return switch (clamped) {
            case 1 -> "settlement.earth_core.tier.status.tier_1_need_mining";
            case 2 -> "settlement.earth_core.tier.status.tier_2_need_industrial";
            case 3 -> "settlement.earth_core.tier.status.tier_3_need_grid";
            default -> "settlement.earth_core.tier.status.tier_4_max";
        };
    }

    public static List<RoleTradeSelection> tradesForProfession(String professionId) {
        return sortedEntries(roles).stream()
                .filter(entry -> entry.getValue().vanillaProfessions().contains(professionId))
                .flatMap(entry -> entry.getValue().trades().stream()
                        .map(trade -> new RoleTradeSelection(entry.getKey(), trade)))
                .toList();
    }

    public static List<TradeDefinition> tradesForRole(Identifier roleId) {
        return role(roleId).trades();
    }

    private static TradeDefinition trade(int level, String inputId, int inputCount, String outputId,
                                         int outputCount, int maxUses, int xp) {
        return new TradeDefinition(level, inputId, inputCount, outputId, outputCount, maxUses, xp, 0.05F);
    }

    private static <T> List<Map.Entry<Identifier, T>> sortedEntries(Map<Identifier, T> values) {
        return values.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .toList();
    }

    private static <T> T weightedPick(List<T> values, ToIntFunction<T> weight, long seed) {
        int total = values.stream().mapToInt(value -> Math.max(1, weight.applyAsInt(value))).sum();
        int target = Math.floorMod((int) mix(seed), total);
        for (T value : values) {
            target -= Math.max(1, weight.applyAsInt(value));
            if (target < 0) {
                return value;
            }
        }
        return values.getFirst();
    }

    public static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xFF51AFD7ED558CCDL;
        value ^= value >>> 33;
        value *= 0xC4CEB9FE1A85EC53L;
        return value ^ value >>> 33;
    }

    private static <T> Map<Identifier, T> validated(Map<Identifier, T> loaded, Map<Identifier, T> fallback, String kind) {
        if (loaded.isEmpty()) {
            EarthOnMinecraft.LOGGER.warn("[Living World] No {} loaded; using built-in fallback data", kind);
            return fallback;
        }
        return Map.copyOf(loaded);
    }

    public record NamePool(List<String> nameKeys, int weight) {
        static final Codec<NamePool> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.listOf().fieldOf("name_keys").forGetter(NamePool::nameKeys),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(NamePool::weight)
        ).apply(instance, NamePool::new));

        public NamePool {
            nameKeys = List.copyOf(nameKeys);
            weight = Math.max(1, weight);
            if (nameKeys.isEmpty()) {
                throw new IllegalArgumentException("A resident name pool must contain at least one name key");
            }
        }
    }

    public record ResidentRole(String titleKey, String identityKey, List<String> vanillaProfessions,
                               List<String> settlementProfiles, int weight, List<TradeDefinition> trades) {
        static final Codec<ResidentRole> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("title_key").forGetter(ResidentRole::titleKey),
                Codec.STRING.fieldOf("identity_key").forGetter(ResidentRole::identityKey),
                Codec.STRING.listOf().optionalFieldOf("vanilla_professions", List.of()).forGetter(ResidentRole::vanillaProfessions),
                Codec.STRING.listOf().optionalFieldOf("settlement_profiles", List.of()).forGetter(ResidentRole::settlementProfiles),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(ResidentRole::weight),
                TradeDefinition.CODEC.listOf().optionalFieldOf("trades", List.of()).forGetter(ResidentRole::trades)
        ).apply(instance, ResidentRole::new));

        public ResidentRole {
            vanillaProfessions = List.copyOf(vanillaProfessions);
            settlementProfiles = List.copyOf(settlementProfiles);
            trades = List.copyOf(trades);
            weight = Math.max(1, weight);
        }
    }

    public record TradeDefinition(int level, String inputId, int inputCount, String outputId,
                                  int outputCount, int maxUses, int xp, float priceMultiplier) {
        static final Codec<TradeDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("level").forGetter(TradeDefinition::level),
                Codec.STRING.fieldOf("input").forGetter(TradeDefinition::inputId),
                Codec.INT.fieldOf("input_count").forGetter(TradeDefinition::inputCount),
                Codec.STRING.fieldOf("output").forGetter(TradeDefinition::outputId),
                Codec.INT.fieldOf("output_count").forGetter(TradeDefinition::outputCount),
                Codec.INT.optionalFieldOf("max_uses", 12).forGetter(TradeDefinition::maxUses),
                Codec.INT.optionalFieldOf("xp", 2).forGetter(TradeDefinition::xp),
                Codec.FLOAT.optionalFieldOf("price_multiplier", 0.05F).forGetter(TradeDefinition::priceMultiplier)
        ).apply(instance, TradeDefinition::new));

        public TradeDefinition {
            level = Math.max(1, Math.min(5, level));
            inputCount = Math.max(1, inputCount);
            outputCount = Math.max(1, outputCount);
            maxUses = Math.max(1, maxUses);
            xp = Math.max(0, xp);
            priceMultiplier = Math.max(0.0F, priceMultiplier);
        }
    }

    public record SettlementProfile(int tier, String displayNameKey, String scaleKey, String technologyKey,
                                    List<String> nameKeys, List<String> industryKeys, List<String> demandKeys,
                                    List<String> supplyKeys, int weight) {
        static final Codec<SettlementProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("tier", 1).forGetter(SettlementProfile::tier),
                Codec.STRING.fieldOf("display_name_key").forGetter(SettlementProfile::displayNameKey),
                Codec.STRING.fieldOf("scale_key").forGetter(SettlementProfile::scaleKey),
                Codec.STRING.fieldOf("technology_key").forGetter(SettlementProfile::technologyKey),
                Codec.STRING.listOf().fieldOf("name_keys").forGetter(SettlementProfile::nameKeys),
                Codec.STRING.listOf().optionalFieldOf("industry_keys", List.of()).forGetter(SettlementProfile::industryKeys),
                Codec.STRING.listOf().optionalFieldOf("demand_keys", List.of()).forGetter(SettlementProfile::demandKeys),
                Codec.STRING.listOf().optionalFieldOf("supply_keys", List.of()).forGetter(SettlementProfile::supplyKeys),
                Codec.INT.optionalFieldOf("weight", 1).forGetter(SettlementProfile::weight)
        ).apply(instance, SettlementProfile::new));

        public SettlementProfile {
            tier = Math.max(1, Math.min(4, tier));
            nameKeys = List.copyOf(nameKeys);
            industryKeys = List.copyOf(industryKeys);
            demandKeys = List.copyOf(demandKeys);
            supplyKeys = List.copyOf(supplyKeys);
            weight = Math.max(1, weight);
            if (nameKeys.isEmpty()) {
                throw new IllegalArgumentException("A settlement profile must contain at least one settlement name key");
            }
        }

        public SettlementProfile(String displayNameKey, String scaleKey, String technologyKey,
                                 List<String> nameKeys, List<String> industryKeys, List<String> demandKeys,
                                 List<String> supplyKeys, int weight) {
            this(1, displayNameKey, scaleKey, technologyKey, nameKeys, industryKeys, demandKeys, supplyKeys, weight);
        }
    }

    public record RoleSelection(Identifier id, ResidentRole role) {
    }

    public record RoleTradeSelection(Identifier roleId, TradeDefinition trade) {
    }

    public record ProfileSelection(Identifier id, SettlementProfile profile) {
    }

    private static final class NamePoolReloadListener extends SimpleJsonResourceReloadListener<NamePool> {
        private NamePoolReloadListener() {
            super(NamePool.CODEC, FileToIdConverter.json(NAME_POOL_PATH));
        }

        @Override
        protected void apply(Map<Identifier, NamePool> loaded, ResourceManager manager, ProfilerFiller profiler) {
            namePools = validated(loaded, FALLBACK_NAME_POOLS, "resident name pools");
            EarthOnMinecraft.LOGGER.info("[Living World] Loaded {} resident name pools", namePools.size());
        }
    }

    private static final class RoleReloadListener extends SimpleJsonResourceReloadListener<ResidentRole> {
        private RoleReloadListener() {
            super(ResidentRole.CODEC, FileToIdConverter.json(ROLE_PATH));
        }

        @Override
        protected void apply(Map<Identifier, ResidentRole> loaded, ResourceManager manager, ProfilerFiller profiler) {
            roles = validated(loaded, FALLBACK_ROLES, "resident roles");
            EarthOnMinecraft.LOGGER.info("[Living World] Loaded {} resident roles", roles.size());
        }
    }

    private static final class ProfileReloadListener extends SimpleJsonResourceReloadListener<SettlementProfile> {
        private ProfileReloadListener() {
            super(SettlementProfile.CODEC, FileToIdConverter.json(PROFILE_PATH));
        }

        @Override
        protected void apply(Map<Identifier, SettlementProfile> loaded, ResourceManager manager, ProfilerFiller profiler) {
            profiles = validated(loaded, FALLBACK_PROFILES, "settlement profiles");
            EarthOnMinecraft.LOGGER.info("[Living World] Loaded {} settlement profiles", profiles.size());
        }
    }
}
