package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.CantileverConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BridgeEvents {
	@Nullable
	private static Bridge bridge;

	public static void init(@Nullable Bridge bridge) {
		BridgeEvents.bridge = bridge;

		registerMinecraftEvents();
		registerDiscordEvents();
	}

	private static void registerMinecraftEvents() {
		if (BridgeEvents.bridge == null)
			return;
		ServerLifecycleEvents.SERVER_STARTING.register(
			Identifier.of(Cantilever.MODID, "after_bridge"),
			server -> BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.value().formatted("Server starting..."))
		);

		ServerLifecycleEvents.SERVER_STARTED.register(server ->
			BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.value().formatted("Server started"))
		);

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (scheduler != null) {
				scheduler.shutdownNow();
			}
			BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.value().formatted("Server stopping..."));
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			BridgeEvents.bridge.sendShutdownMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.value().formatted("Server stopped"));
			BridgeEvents.bridge.stop();
		});

		ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
			if (message.getContent() instanceof BridgeTextContent) return;
			BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.value().formatted(message.getString()));
		});

		ServerMessageEvents.COMMAND_MESSAGE.register((message, source, parameters) -> {
			if (message.getContent().getContent() instanceof BridgeTextContent) return;
			if (source.isExecutedByPlayer()) {
				BridgeEvents.bridge.sendWebhookMessageM2D(message.getContent(), source.getPlayer());
				return;
			}
			BridgeEvents.bridge.sendBasicMessageM2D(CantileverConfig.INSTANCE.gameEventFormat.value().formatted(message.getContent().getString()));
		});

		ServerMessageEvents.CHAT_MESSAGE.register((message, user, params) -> BridgeEvents.bridge.sendWebhookMessageM2D(message.getContent(), user));
	}

	private static ScheduledExecutorService scheduler;

	private static void registerDiscordEvents() {
		if (BridgeEvents.bridge == null)
			return;

		JDA api = BridgeEvents.bridge.api();
		if (api == null)
			return;

		api.addEventListener(new ListenerAdapter() {
			@Override
			public void onMessageReceived(@NotNull MessageReceivedEvent event) {
				if (!BridgeEvents.bridge.channel().map(c -> c == event.getChannel()).orElse(false) ||
					event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong() || event.getAuthor().getIdLong() == BridgeEvents.bridge.getWebhookId()) {
					return;
				}

				String authorName = event.getMember() != null ?
					event.getMember().getEffectiveName() : event.getAuthor().getEffectiveName();

				if (scheduler == null && CantileverConfig.INSTANCE.d2mMessageDelay.value() > 0) {
					scheduler = Executors.newScheduledThreadPool(1, runnable -> {
						var thread = new Thread(runnable, "Cantilever D2M Message Scheduler");
						thread.setDaemon(true);
						thread.setUncaughtExceptionHandler((thread1, throwable) -> Cantilever.LOGGER.error("Caught exception in D2M Message Scheduler", throwable));
						return thread;
					});
				}

				if (scheduler != null) {
					scheduler.schedule(() -> {
						event.getChannel().retrieveMessageById(event.getMessageIdLong()).onSuccess(message ->
							BridgeEvents.bridge.sendUserMessageD2M(authorName, message.getContentDisplay())
						).complete();
					}, CantileverConfig.INSTANCE.d2mMessageDelay.value(), TimeUnit.MILLISECONDS);
					return;
				}

				BridgeEvents.bridge.sendUserMessageD2M(authorName, event.getMessage().getContentDisplay());
			}
		});
	}
}
