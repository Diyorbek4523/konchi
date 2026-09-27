package uz.konchi;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class KonchiClient implements ClientModInitializer {
	public static final String MOD_ID = "konchi";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(MOD_ID, "main")
	);

	/** Yuklangan konteynerlar (sandiq, bochka va h.k.). */
	private static final Set<BlockEntity> CONTAINERS = Collections.newSetFromMap(new WeakHashMap<>());

	private static KeyMapping menuKey;
	/** Tungi ko'rish effektini biz qo'yganmizmi. */
	private static boolean fullbrightApplied = false;

	@Override
	public void onInitializeClient() {
		Settings.load();

		menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.konchi.menu",
				InputConstants.Type.KEYSYM,
				InputConstants.KEY_RSHIFT,
				CATEGORY
		));

		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, level) -> {
			if (EspRenderer.isContainer(be)) {
				CONTAINERS.add(be);
			}
		});
		ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, level) -> CONTAINERS.remove(be));

		ClientTickEvents.END_CLIENT_TICK.register(KonchiClient::onTick);

		WorldRenderEvents.BEFORE_TRANSLUCENT.register(context -> EspRenderer.render(context, CONTAINERS));
	}

	private static void onTick(Minecraft client) {
		while (menuKey.consumeClick()) {
			if (client.screen == null) {
				client.setScreen(new KonchiScreen());
			}
		}

		updateFullbright(client.player);
		OreScanner.tick(client);
	}

	/** Fullbright: o'yinchiga faqat o'zingiz ko'radigan tungi ko'rish effektini beradi. */
	private static void updateFullbright(LocalPlayer player) {
		if (player == null) {
			fullbrightApplied = false;
			return;
		}

		if (Settings.fullbright) {
			if (!player.hasEffect(MobEffects.NIGHT_VISION)) {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1_000_000, 0, false, false, false));
				fullbrightApplied = true;
			}
		} else if (fullbrightApplied) {
			player.removeEffect(MobEffects.NIGHT_VISION);
			fullbrightApplied = false;
		}
	}
}
