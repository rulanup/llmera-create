package com.example.llmera.create.network;

import com.example.llmera.create.LLMEraCreate;
import com.example.llmera.network.CodeNetworkMember;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CreateKineticsBridgeBlockEntity extends BlockEntity implements CodeNetworkMember {
    public static final int CHANNEL_DEMAND = 1;

    public CreateKineticsBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(LLMEraCreate.KINETICS_BRIDGE.get(), pos, state);
    }

    public BlockPos targetPos() {
        return worldPosition.relative(getBlockState().getValue(CreateKineticsBridgeBlock.FACING));
    }

    public BlockEntity target() {
        return level == null ? null : level.getBlockEntity(targetPos());
    }

    public boolean hasKineticTarget() {
        return isKineticTarget(target());
    }

    public static boolean isKineticTarget(BlockEntity target) {
        return target instanceof KineticBlockEntity;
    }

    public String targetType() {
        BlockEntity target = target();
        if (target instanceof KineticBlockEntity) {
            return "kinetic";
        }
        return "none";
    }

    @Override
    public int channelDemand() {
        return CHANNEL_DEMAND;
    }
}
