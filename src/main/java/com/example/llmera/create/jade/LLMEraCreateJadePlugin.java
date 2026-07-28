package com.example.llmera.create.jade;

import com.example.llmera.create.network.CreateKineticsBridgeBlock;
import com.example.llmera.create.network.CreateKineticsBridgeBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin("create")
public final class LLMEraCreateJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(CreateKineticsStatusProvider.INSTANCE,
                CreateKineticsBridgeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(CreateKineticsStatusProvider.INSTANCE,
                CreateKineticsBridgeBlock.class);
    }
}
