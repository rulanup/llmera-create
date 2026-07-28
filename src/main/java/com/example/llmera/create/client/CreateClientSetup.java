package com.example.llmera.create.client;

import com.example.llmera.create.LLMEraCreate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = LLMEraCreate.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class CreateClientSetup {
    private CreateClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(LLMEraCreate.MACHINE_CONNECTOR.get(),
                CreateMachineConnectorRenderer::new);
    }
}
