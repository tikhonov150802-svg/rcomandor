package com.rcomandor.mixin;

import com.rcomandor.FreeCam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

	/** Когда свободная камера включена, мышь поворачивает камеру, а не игрока. */
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void rcomandor$turnCamera(double yRot, double xRot, CallbackInfo ci) {
		if (FreeCam.isActive() && (Object) this == Minecraft.getInstance().player) {
			FreeCam.turn(yRot, xRot);
			ci.cancel();
		}
	}
}
