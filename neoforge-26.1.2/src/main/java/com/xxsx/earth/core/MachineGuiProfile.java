package com.xxsx.earth.core;

import java.util.EnumSet;
import java.util.Set;

/** Stable, kind-specific slot visibility and arrangement for processing machine menus. */
public record MachineGuiProfile(int secondarySlotCount, boolean usesReagentSlot,
                                boolean usesFuelOrDriveSlot, int visibleOutputSlots,
                                boolean usesFluidGauge, boolean usesTempGauge,
                                boolean usesEnergyGauge) {
    private static final Set<ProcessingMachineBlock.Kind> MULTI_INPUT_KINDS = EnumSet.of(
            ProcessingMachineBlock.Kind.MIXER,
            ProcessingMachineBlock.Kind.CHEMICAL_REACTOR,
            ProcessingMachineBlock.Kind.POLYMERIZER,
            ProcessingMachineBlock.Kind.SYNTHESIS_LOOP,
            ProcessingMachineBlock.Kind.ABSORPTION_TOWER,
            ProcessingMachineBlock.Kind.HERB_PROCESSING_TABLE,
            ProcessingMachineBlock.Kind.BAR_COUNTER,
            ProcessingMachineBlock.Kind.TABLET_PRESS,
            ProcessingMachineBlock.Kind.FERTILIZER_GRANULATOR
    );

    private static final Set<ProcessingMachineBlock.Kind> GUI_TWO_BY_TWO_KINDS = EnumSet.of(
            ProcessingMachineBlock.Kind.CHEMICAL_REACTOR,
            ProcessingMachineBlock.Kind.ELECTROLYTIC_CELL
    );

    public static MachineGuiProfile forKind(ProcessingMachineBlock.Kind kind) {
        int secondary = 0;
        boolean reagent = kind == ProcessingMachineBlock.Kind.REDUCTION_FURNACE;
        boolean fluid = false;
        int outputs = 1;
        for (SpecMachineRecipes.Route route : SpecMachineRecipes.ROUTES) {
            if (!route.kind().equals(kind.name())) {
                continue;
            }
            secondary = Math.max(secondary, Math.max(0, route.inputs().length - 1));
            reagent |= route.catalyst() != null && !route.catalyst().isEmpty();
            fluid |= route.fluidInputs().length > 0 || route.fluidOutputs().length > 0;
            outputs = Math.max(outputs, route.outputs().length);
        }
        secondary = switch (kind) {
            case CRUSHER, BALL_MILL, SIEVE, MAGNETIC_SEPARATOR, FLOTATION_CELL, ROASTER -> 0;
            case REDUCTION_FURNACE -> Math.max(1, secondary);
            default -> secondary;
        };
        if (MULTI_INPUT_KINDS.contains(kind)) {
            secondary = Math.max(2, secondary);
        }
        if (kind == ProcessingMachineBlock.Kind.REDUCTION_FURNACE) {
            reagent = true;
            outputs = Math.max(3, outputs);
        }
        outputs = switch (kind) {
            case CRUSHER, BALL_MILL -> Math.max(5, outputs);
            case SIEVE, MAGNETIC_SEPARATOR, FLOTATION_CELL, ROASTER -> Math.max(4, outputs);
            default -> outputs;
        };
        if (GUI_TWO_BY_TWO_KINDS.contains(kind)) outputs = Math.min(4, outputs);
        int visibleOutputs = outputs <= 2 ? 2 : outputs <= 4 ? 4 : 6;
        return new MachineGuiProfile(Math.min(4, secondary), reagent,
                kind.acceptsLocalFuel() || kind.powerMode() == ProcessingMachineBlock.PowerMode.KINETIC,
                visibleOutputs, fluid, kind.usesThermalTemperature(), !kind.powerMode().passive());
    }

    public boolean activeMachineSlot(int slot) {
        if (slot >= ProcessingMachineBlockEntity.SLOT_SECONDARY_START
                && slot < ProcessingMachineBlockEntity.SLOT_SECONDARY_START + 4) {
            return slot - ProcessingMachineBlockEntity.SLOT_SECONDARY_START < secondarySlotCount;
        }
        if (slot == ProcessingMachineBlockEntity.SLOT_FUEL) return usesFuelOrDriveSlot;
        if (slot == ProcessingMachineBlockEntity.SLOT_REAGENT) return usesReagentSlot;
        if (slot >= ProcessingMachineBlockEntity.SLOT_OUTPUT_START
                && slot < ProcessingMachineBlockEntity.SLOT_OUTPUT_START + ProcessingMachineBlockEntity.OUTPUT_SLOT_COUNT) {
            return slot - ProcessingMachineBlockEntity.SLOT_OUTPUT_START < visibleOutputSlots;
        }
        return true;
    }
}
