package com.maeiro.serenebetterwinter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;

public final class EmbeddiumCompat {
    private static final String EMBEDDIUM_MOD_ID = "embeddium";
    private static boolean registered = false;
    private static boolean registrationFailedLogged = false;
    private static final Map<Class<?>, ContextAccessors> ACCESSORS_CACHE = new ConcurrentHashMap<>();

    private EmbeddiumCompat() {
    }

    public static void tryRegisterRenderFilters() {
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

                    Object context = args[0];
                    if (context == null) {
                        return passResult;
                    }

                    ContextAccessors accessors = ACCESSORS_CACHE.computeIfAbsent(context.getClass(), ContextAccessors::new);
                    if (!accessors.valid()) {
                        return passResult;
                    }

                    BlockState state = (BlockState) accessors.state().invoke(context);
                    if (state == null) {
                        return passResult;
                    }

                    BlockPos pos = (BlockPos) accessors.pos().invoke(context);
                    BlockAndTintGetter level = (BlockAndTintGetter) accessors.localSlice().invoke(context);
                    if (ClientVisualRules.shouldHideBlockVisual(level, pos, state)) {
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
                    if (listObj instanceof List<?> list) {
                        @SuppressWarnings("unchecked")
                        List<Object> raw = (List<Object>) list;
                        raw.add(rendererProxy);
                    }
                    return null;
                }
            });

            Object registry = registryClass.getMethod("instance").invoke(null);
            registryClass.getMethod("registerRenderPopulator", populatorInterface).invoke(registry, populatorProxy);
            registered = true;
            SereneBetterWinterMod.LOGGER.info("[{}] Registered Embeddium render filters via BlockRendererRegistry API.", SereneBetterWinterMod.MOD_ID);
        } catch (Throwable t) {
            if (!registrationFailedLogged) {
                registrationFailedLogged = true;
                SereneBetterWinterMod.LOGGER.warn("[{}] Failed to register Embeddium compatibility filter.", SereneBetterWinterMod.MOD_ID, t);
            }
        }
    }

    private record ContextAccessors(Method state, Method pos, Method localSlice) {
        private ContextAccessors(Class<?> contextClass) {
            this(find(contextClass, "state"), find(contextClass, "pos"), find(contextClass, "localSlice"));
        }

        private static Method find(Class<?> type, String name) {
            try {
                return type.getMethod(name);
            } catch (Exception ignored) {
                return null;
            }
        }

        private boolean valid() {
            return state != null && pos != null && localSlice != null;
        }
    }
}
