package com.maeiro.serenebetterwinter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public final class EmbeddiumCompat {
    private static final String EMBEDDIUM_MOD_ID = "embeddium";
    private static boolean registered = false;
    private static boolean registrationFailedLogged = false;

    private EmbeddiumCompat() {
    }

    public static void tryRegisterSnowLayerFilter() {
        if (registered || !ModList.get().isLoaded(EMBEDDIUM_MOD_ID)) {
            return;
        }

        try {
            ClassLoader cl = EmbeddiumCompat.class.getClassLoader();
            Class<?> registryClass = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry", false, cl);
            Class<?> populatorInterface = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry$RenderPopulator", false, cl);
            Class<?> rendererInterface = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry$Renderer", false, cl);
            Class<?> renderResultEnum = Class.forName("org.embeddedt.embeddium.api.BlockRendererRegistry$RenderResult", false, cl);

            Object passResult = renderResultEnum.getField("PASS").get(null);
            Object overrideResult = renderResultEnum.getField("OVERRIDE").get(null);

            Object rendererProxy = Proxy.newProxyInstance(cl, new Class<?>[]{rendererInterface}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (!"renderBlock".equals(method.getName()) || args == null || args.length < 1) {
                        return passResult;
                    }

                    if (!ClientConfig.ENABLED.get() || !ClientConfig.HIDE_SNOW_ABOVE_HIDDEN_LEAVES.get() || !ClientSeasonTracker.isLeaflessSeasonActive()) {
                        return passResult;
                    }

                    Object context = args[0];
                    if (context == null) {
                        return passResult;
                    }

                    Method stateMethod = context.getClass().getMethod("state");
                    Method posMethod = context.getClass().getMethod("pos");
                    Method localSliceMethod = context.getClass().getMethod("localSlice");

                    BlockState state = (BlockState) stateMethod.invoke(context);
                    if (state == null || !state.is(Blocks.SNOW)) {
                        return passResult;
                    }

                    BlockPos pos = (BlockPos) posMethod.invoke(context);
                    BlockAndTintGetter level = (BlockAndTintGetter) localSliceMethod.invoke(context);
                    if (SnowRenderRules.shouldHideSnowLayerAboveHiddenLeaves(level, pos, state)) {
                        return overrideResult;
                    }

                    return passResult;
                }
            });

            Object populatorProxy = Proxy.newProxyInstance(cl, new Class<?>[]{populatorInterface}, new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if (!"fillCustomRenderers".equals(method.getName()) || args == null || args.length < 2) {
                        return null;
                    }
                    Object listObj = args[0];
                    if (listObj != null) {
                        listObj.getClass().getMethod("add", Object.class).invoke(listObj, rendererProxy);
                    }
                    return null;
                }
            });

            Object registry = registryClass.getMethod("instance").invoke(null);
            registryClass.getMethod("registerRenderPopulator", populatorInterface).invoke(registry, populatorProxy);
            registered = true;
            SereneBetterWinterMod.LOGGER.info("[{}] Registered Embeddium snow-layer filter via BlockRendererRegistry API.", SereneBetterWinterMod.MOD_ID);
        } catch (Throwable t) {
            if (!registrationFailedLogged) {
                registrationFailedLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Failed to register Embeddium compatibility filter.", SereneBetterWinterMod.MOD_ID, t);
            }
        }
    }
}
