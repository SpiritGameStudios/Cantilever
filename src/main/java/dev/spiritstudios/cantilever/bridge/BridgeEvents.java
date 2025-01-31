package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.Cantilever;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BridgeEvents {
	private static Bridge bridge;

	public static void init(Bridge bridge) {
		BridgeEvents.bridge = bridge;

		registerMinecraftEvents();
		registerDiscordEvents();
	}

	private static void registerMinecraftEvents() {
		ServerLifecycleEvents.SERVER_STARTING.register(
			Identifier.of(Cantilever.MODID, "after_bridge"),
			server -> {
				BridgeEvents.bridge.sendBasicMessageM2D("**Server starting...**");
			}
		);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			BridgeEvents.bridge.sendBasicMessageM2D("**Server started**");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			BridgeEvents.bridge.sendBasicMessageM2D("**Server stopping...**");
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BridgeEvents.bridge.sendBasicMessageM2D("**Server stopped**");
		});

		ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
			if (message.getContent() instanceof BridgeTextContent content && content.bot()) {
				return;
			}
			BridgeEvents.bridge.sendBasicMessageM2D("**%s**".formatted(message.getString()));
		});
		ServerMessageEvents.CHAT_MESSAGE.register((message, user, params) -> {
			BridgeEvents.bridge.sendBasicMessageM2D("%s: %s".formatted(user.getName().getString(), message.getContent().getString()));
		});
	}

	private static void registerDiscordEvents() {
		BridgeEvents.bridge.api().addMessageCreateListener(event -> {
			if (BridgeEvents.bridge.channel().map(c -> c == event.getChannel()).orElse(false) && !event.getMessageAuthor().isYourself()) {
				BridgeEvents.bridge.sendBasicMessageD2M(
					new BridgeTextContent(
						Text.of(
							"<@%s> %s".formatted(
								event.getMessageAuthor().getName(), event.getMessageContent()
							)
						), true
					)
				);
			}
		});
	}
}
