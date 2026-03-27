package com.github.ajsnarr98.autorestartreloaded.gametest.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

/**
 * Workaround for a Fabric API bug in Minecraft 1.21.10 where
 * {@code BuiltInRegistries.TEST_FUNCTION} is frozen during Bootstrap before
 * {@code FabricGameTestModInitializer.onInitialize()} can register test functions.
 *
 * <p>Two injections are needed:
 * <ol>
 *   <li>{@code validateWrite(ResourceKey)} — skips the "already frozen" check so that
 *       {@code register()} can proceed.</li>
 *   <li>{@code register(...)} at RETURN — calls {@code Holder.Reference.bindValue()} via
 *       reflection, because {@code MappedRegistry.freeze()} (which normally calls
 *       {@code bindValue()} for every entry) has already run.</li>
 * </ol>
 *
 * <p>The second injection is guarded by {@code frozen && !holder.isBound()} so it only
 * fires for post-freeze registrations and is harmless on versions where the registry is
 * still unfrozen at mod-init time.
 */
@Mixin(MappedRegistry.class)
public abstract class MappedRegistryGameTestMixin<T> {

    @Shadow private boolean frozen;
    @Shadow private ResourceKey<? extends Registry<T>> key;

    private boolean isTestFunctionRegistry() {
        return "minecraft".equals(this.key.location().getNamespace())
                && "test_function".equals(this.key.location().getPath());
    }

    /** Bypass the frozen-registry guard for {@code minecraft:test_function}. */
    @Inject(at = @At("HEAD"), method = "validateWrite(Lnet/minecraft/resources/ResourceKey;)V", cancellable = true)
    private void allowTestFunctionLateRegistration(ResourceKey<T> resourceKey, CallbackInfo ci) {
        if (frozen && isTestFunctionRegistry()) {
            ci.cancel();
        }
    }

    /**
     * After a late registration to {@code minecraft:test_function}, manually bind the value
     * to the holder via reflection (normally done by {@code MappedRegistry.freeze()}).
     */
    @Inject(
            at = @At("RETURN"),
            method = "register(Lnet/minecraft/resources/ResourceKey;Ljava/lang/Object;Lnet/minecraft/core/RegistrationInfo;)Lnet/minecraft/core/Holder$Reference;"
    )
    private void bindValueAfterLateRegistration(
            ResourceKey<T> registeredKey,
            T value,
            RegistrationInfo registrationInfo,
            CallbackInfoReturnable<Holder.Reference<T>> cir
    ) {
        if (frozen && isTestFunctionRegistry()) {
            Holder.Reference<T> holder = cir.getReturnValue();
            if (holder != null && !holder.isBound()) {
                try {
                    Method bindValue = holder.getClass().getDeclaredMethod("bindValue", Object.class);
                    bindValue.setAccessible(true);
                    bindValue.invoke(holder, value);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(
                            "Failed to bind value to late-registered test_function holder: " + registeredKey, e);
                }
            }
        }
    }
}
