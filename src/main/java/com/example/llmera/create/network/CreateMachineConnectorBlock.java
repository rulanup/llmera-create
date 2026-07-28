package com.example.llmera.create.network;

import com.example.llmera.create.LLMEraCreate;
import com.example.llmera.network.CodeNetworkMember;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public final class CreateMachineConnectorBlock extends Block implements EntityBlock, CodeNetworkMember {
    public CreateMachineConnectorBlock() {
        super(Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(2.5f));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreateMachineConnectorBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        if (level.isClientSide || type != LLMEraCreate.MACHINE_CONNECTOR.get()) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) ->
                CreateMachineConnectorBlockEntity.serverTick(
                        tickLevel, tickPos, tickState, (CreateMachineConnectorBlockEntity) blockEntity);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CreateMachineConnectorBlockEntity connector) {
            player.displayClientMessage(connector.statusMessage(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
