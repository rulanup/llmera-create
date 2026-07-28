package com.example.llmera.create.network;

import com.example.llmera.network.CodeNetworkMember;
import com.example.llmera.network.CodeNetworkScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

public final class CreateKineticsBridgeBlock extends Block implements EntityBlock, CodeNetworkMember {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public CreateKineticsBridgeBlock() {
        super(Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(2.5f));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return rotate(state, mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreateKineticsBridgeBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            boolean online = CodeNetworkScanner.scan(serverLevel, pos).memberHasChannel(pos);
            CreateKineticsBridgeBlockEntity bridge = serverLevel.getBlockEntity(pos) instanceof CreateKineticsBridgeBlockEntity be ? be : null;
            boolean connected = bridge != null && bridge.hasKineticTarget();
            player.displayClientMessage(Component.literal("Create Kinetics Bridge: code "
                    + (online ? "1/1" : "0/1") + ", target " + (connected ? "connected" : "disconnected")), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
