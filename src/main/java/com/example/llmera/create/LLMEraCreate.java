package com.example.llmera.create;

import com.example.llmera.create.item.CreateConnectionToolItem;
import com.example.llmera.create.llm.CreateToolExtension;
import com.example.llmera.create.network.CreateKineticsBridgeBlock;
import com.example.llmera.create.network.CreateKineticsBridgeBlockEntity;
import com.example.llmera.create.network.CreateMachineConnectorBlock;
import com.example.llmera.create.network.CreateMachineConnectorBlockEntity;
import com.example.llmera.llm.ToolExtensionRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(LLMEraCreate.MODID)
public final class LLMEraCreate {
    public static final String MODID = "llmera_create";

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<CreateKineticsBridgeBlock> KINETICS_BRIDGE_BLOCK =
            BLOCKS.register("create_kinetics_bridge", CreateKineticsBridgeBlock::new);
    public static final DeferredItem<BlockItem> KINETICS_BRIDGE_ITEM =
            ITEMS.registerSimpleBlockItem("create_kinetics_bridge", KINETICS_BRIDGE_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreateKineticsBridgeBlockEntity>> KINETICS_BRIDGE =
            BLOCK_ENTITIES.register("create_kinetics_bridge", () -> BlockEntityType.Builder.of(
                    CreateKineticsBridgeBlockEntity::new, KINETICS_BRIDGE_BLOCK.get()).build(null));
    public static final DeferredBlock<CreateMachineConnectorBlock> MACHINE_CONNECTOR_BLOCK =
            BLOCKS.register("create_machine_connector", CreateMachineConnectorBlock::new);
    public static final DeferredItem<BlockItem> MACHINE_CONNECTOR_ITEM =
            ITEMS.registerSimpleBlockItem("create_machine_connector", MACHINE_CONNECTOR_BLOCK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreateMachineConnectorBlockEntity>> MACHINE_CONNECTOR =
            BLOCK_ENTITIES.register("create_machine_connector", () -> BlockEntityType.Builder.of(
                    CreateMachineConnectorBlockEntity::new, MACHINE_CONNECTOR_BLOCK.get()).build(null));
    public static final DeferredItem<CreateConnectionToolItem> CONNECTION_TOOL =
            ITEMS.register("create_connection_tool", CreateConnectionToolItem::new);
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.llmera_create"))
                    .icon(() -> new ItemStack(KINETICS_BRIDGE_ITEM.get()))
                    .displayItems((params, output) -> {
                        output.accept(KINETICS_BRIDGE_ITEM.get());
                        output.accept(MACHINE_CONNECTOR_ITEM.get());
                        output.accept(CONNECTION_TOOL.get());
                    })
                    .build());

    public LLMEraCreate(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        CREATIVE_TABS.register(modEventBus);
        ToolExtensionRegistry.register(CreateToolExtension.extension());
    }
}
