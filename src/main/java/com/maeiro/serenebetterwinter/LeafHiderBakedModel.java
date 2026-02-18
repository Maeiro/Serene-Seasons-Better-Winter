package com.maeiro.serenebetterwinter;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;

public final class LeafHiderBakedModel extends BakedModelWrapper<BakedModel> {
    public LeafHiderBakedModel(BakedModel originalModel) {
        super(originalModel);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        if (state == null) {
            return super.getQuads(null, side, random);
        }

        boolean active = ClientSeasonTracker.isLeaflessSeasonActive();
        boolean hide = active && LeafTargeting.shouldHide(state);

        if (hide) {
            return List.of();
        }
        return super.getQuads(state, side, random);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData modelData, RenderType renderType) {
        if (state == null) {
            return super.getQuads(null, side, random, modelData, renderType);
        }

        boolean active = ClientSeasonTracker.isLeaflessSeasonActive();
        boolean hide = active && LeafTargeting.shouldHide(state);

        if (hide) {
            return List.of();
        }
        return super.getQuads(state, side, random, modelData, renderType);
    }
}
