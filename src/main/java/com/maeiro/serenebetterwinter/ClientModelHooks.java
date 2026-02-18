package com.maeiro.serenebetterwinter;

import java.util.Map;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SereneBetterWinterMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientModelHooks {
    private ClientModelHooks() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        int wrapped = 0;
        for (Map.Entry<ResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            if (!(entry.getValue() instanceof LeafHiderBakedModel)) {
                entry.setValue(new LeafHiderBakedModel(entry.getValue()));
                wrapped++;
            }
        }
        SereneBetterWinterMod.LOGGER.info("[{}] Wrapped {} baked models for conditional leaf hiding.", SereneBetterWinterMod.MOD_ID, wrapped);
    }
}
