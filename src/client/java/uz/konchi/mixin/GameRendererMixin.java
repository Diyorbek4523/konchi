package uz.konchi.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.GameRenderer;

import uz.konchi.EspRenderer;

/** O'yin yopilganda GPU buferlarini tozalaydi. */
@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "close", at = @At("RETURN"))
	private void konchi$onClose(CallbackInfo ci) {
		EspRenderer.close();
	}
}
