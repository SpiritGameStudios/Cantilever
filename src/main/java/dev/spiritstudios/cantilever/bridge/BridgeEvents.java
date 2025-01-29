package dev.spiritstudios.cantilever.bridge;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.text.Text;

public class BridgeEvents {
	private final Bridge bridge;

	public BridgeEvents(Bridge bridge) {
		this.bridge = bridge;

		registerMinecraftEvents();
		registerDiscordEvents();
	}

	private void registerMinecraftEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			this.bridge.sendBasicMessageM2D("**Server started**");
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			this.bridge.sendBasicMessageM2D("**Server stopping**");
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			this.bridge.sendBasicMessageM2D("**Server stopped**");
		});

		ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
			if (message.getContent() instanceof BridgeTextContent content && content.bot()) {
				return;
			}
			this.bridge.sendBasicMessageM2D("**%s**".formatted(message.getString()));
		});
		ServerMessageEvents.CHAT_MESSAGE.register((message, user, params) -> {
			this.bridge.sendBasicMessageM2D("%s: %s".formatted(user.getName().getString(), message.getContent().getString()));
		});
	}

	private void registerDiscordEvents() {
		this.bridge.getDiscordApi().addMessageCreateListener(event -> {
			if (event.getChannel() == this.bridge.bridgeChannel && !event.getMessageAuthor().isYourself()) {
				this.bridge.sendBasicMessageD2M(
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
