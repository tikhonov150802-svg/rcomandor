package com.rcomandor;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class RcomandorClient implements ClientModInitializer {

	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rcomandor", "controls"));

	public static KeyMapping keyToggle;
	public static KeyMapping keyNoclip;
	public static KeyMapping keyFaster;
	public static KeyMapping keySlower;

	@Override
	public void onInitializeClient() {
		keyToggle = register("toggle", GLFW.GLFW_KEY_F4);
		keyNoclip = register("collision", GLFW.GLFW_KEY_N);
		keyFaster = register("faster", GLFW.GLFW_KEY_RIGHT_BRACKET);
		keySlower = register("slower", GLFW.GLFW_KEY_LEFT_BRACKET);

		ClientTickEvents.START_CLIENT_TICK.register(RcomandorClient::onTick);
	}

	private static KeyMapping register(String name, int key) {
		return KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.rcomandor." + name, InputConstants.Type.KEYSYM, key, CATEGORY));
	}

	private static void onTick(Minecraft mc) {
		while (keyToggle.consumeClick()) {
			FreeCam.toggle(mc);
		}
		while (keyNoclip.consumeClick()) {
			FreeCam.noclip = !FreeCam.noclip;
			message(mc, FreeCam.noclip ? "rcomandor.msg.noclip_on" : "rcomandor.msg.noclip_off");
		}
		while (keyFaster.consumeClick()) {
			FreeCam.changeSpeed(mc, 1);
		}
		while (keySlower.consumeClick()) {
			FreeCam.changeSpeed(mc, -1);
		}
		FreeCam.tick(mc);
	}

	static void message(Minecraft mc, String key, Object... args) {
		if (mc.player != null) {
			mc.player.displayClientMessage(Component.translatable(key, args), true);
		}
	}
}
