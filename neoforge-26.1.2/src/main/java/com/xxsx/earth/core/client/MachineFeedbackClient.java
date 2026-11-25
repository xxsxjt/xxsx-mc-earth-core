package com.xxsx.earth.core.client;

import com.xxsx.earth.core.MachineFeedback;
import com.xxsx.earth.core.ProcessingMachineBlock;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

final class MachineFeedbackClient {
    private MachineFeedbackClient() {
    }

    static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        for (ProcessingMachineBlock.Kind kind : ProcessingMachineBlock.Kind.values()) {
            event.registerSpriteSet(MachineFeedback.particle(kind), sprites ->
                    (options, level, x, y, z, xSpeed, ySpeed, zSpeed, random) ->
                            new MachineProcessParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, kind, random));
        }
        for (MachineFeedback.DeviceKind kind : MachineFeedback.DeviceKind.values()) {
            event.registerSpriteSet(MachineFeedback.particle(kind), sprites ->
                    (options, level, x, y, z, xSpeed, ySpeed, zSpeed, random) ->
                            new DeviceProcessParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, sprites, kind, random));
        }
    }
}
