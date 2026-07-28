package com.example.llmera.create;

import com.example.llmera.create.network.CreateMachineConnectorBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = LLMEraCreate.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class CreateCapabilities {
    private CreateCapabilities() {
    }

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                LLMEraCreate.MACHINE_CONNECTOR.get(),
                (connector, side) -> connector.forgeEnergyStorage());
    }
}
