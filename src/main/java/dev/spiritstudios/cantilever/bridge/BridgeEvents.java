package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.CantileverConfig;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
			server -> BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.get().formatted("Server starting..."))
		);

		ServerLifecycleEvents.SERVER_STARTED.register(server ->
			BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.get().formatted("Server started"))
		);

		ServerLifecycleEvents.SERVER_STOPPING.register(server ->
			BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.get().formatted("Server stopping..."))
		);

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
				BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.get().formatted("Server stopped"));
				bridge.api().shutdown();
			}
		);

		ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
			if (message.getContent() instanceof BridgeTextContent) return;
			BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.get().formatted(message.getString()));
		});

		ServerMessageEvents.COMMAND_MESSAGE.register((message, source, parameters) -> {
			if (message.getContent().getContent() instanceof BridgeTextContent) return;
			// TODO: Apply decorations to Minecraft messages.
			BridgeEvents.bridge.sendBasicMessageM2D(parameters.applyChatDecoration(message.getContent()).getString());
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, user, params) -> BridgeEvents.bridge.sendWebhookMessageM2D(message, user));
	}

	private static ScheduledExecutorService scheduler;

	private static void registerDiscordEvents() {
		BridgeEvents.bridge.api().addEventListener(new ListenerAdapter() {
			@Override
			public void onMessageReceived(@NotNull MessageReceivedEvent event) {
				if (!BridgeEvents.bridge.channel().map(c -> c == event.getChannel()).orElse(false) ||
					event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong() || event.getAuthor().getIdLong() == BridgeEvents.bridge.getWebhookId()) {
					return;
				}

				var webhooksForRemoval = CantileverConfig.INSTANCE.webhooksForRemoval.get();
				if (scheduler == null && CantileverConfig.INSTANCE.d2mMessageDelay.get() > 0 && (!webhooksForRemoval.webhookIds().isEmpty() || webhooksForRemoval.inverted())) {
					scheduler = Executors.newScheduledThreadPool(1, runnable -> {
						var thread = new Thread(runnable, "Cantilever D2M Message Scheduler");
						thread.setDaemon(true);
						return thread;
					});
				}

				if (scheduler != null) {
					var historyFuture = event.getChannel().asGuildMessageChannel().getHistoryAfter(event.getMessageIdLong(), CantileverConfig.INSTANCE.webhookMessagesToCheck.get());
					scheduler.schedule(() -> {
						try {
							var history = historyFuture.complete();
							if (!history.isEmpty() && !event.isWebhookMessage() && history.getRetrievedHistory().stream()
								.noneMatch(message -> isMessageWebhook(message, event.getMessage()))) {
								return;
							}

							for (Text text : BridgeFormatter.formatDiscordText(event)) {
								BridgeEvents.bridge.sendBasicMessageD2M(new BridgeTextContent(text));
							}
						} catch (Exception ex) {
							Cantilever.LOGGER.error("Caught exception in D2M Message Scheduler", ex);
						}
					}, CantileverConfig.INSTANCE.d2mMessageDelay.get(), TimeUnit.MILLISECONDS);
					return;
				}


				for (Text text : BridgeFormatter.formatDiscordText(event)) {
					BridgeEvents.bridge.sendBasicMessageD2M(new BridgeTextContent(text));
				}
			}

			private static boolean isMessageWebhook(Message message, Message originalMessage) {
				if (!message.isWebhookMessage() || message.getAuthor().getIdLong() == BridgeEvents.bridge.getWebhookId() ||
					!originalMessage.getContentRaw().contains(message.getContentRaw())) {
					return false;
				}

				var webhooksForRemoval = CantileverConfig.INSTANCE.webhooksForRemoval.get();
				for (long webhookId : webhooksForRemoval.webhookIds()) {
					if (webhookId == message.getAuthor().getIdLong()) {
						return !webhooksForRemoval.inverted();
					}
				}
				return webhooksForRemoval.inverted();
			}
		});
	}
}
