package dev.spiritstudios.cantilever.bridge;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.CantileverConfig;
import eu.pb4.styledchat.StyledChatUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static dev.spiritstudios.cantilever.Cantilever.LOGGER;

public class Bridge {
	private final @Nullable JDA api;
	private TextChannel bridgeChannel;
	private WebhookClient bridgeChannelWebhook;
	public final MinecraftServer server;

	public Bridge(MinecraftServer server) {
		this.server = server;
		JDA api = null;

		try {
			if (Objects.equals(CantileverConfig.INSTANCE.token.value(), CantileverConfig.INSTANCE.token.getDefaultValue()))
				throw new IllegalStateException("You forgot to set your bot token in the config file! Please create a discord bot application and add it's token to the config file.");

			api = JDABuilder
				.createLight(
					CantileverConfig.INSTANCE.token.value(),
					GatewayIntent.GUILD_MESSAGES,
					GatewayIntent.MESSAGE_CONTENT
				)
				.setActivity(CantileverConfig.INSTANCE.statusMessage.value().isEmpty() ?
					null :
					Activity.of(CantileverConfig.INSTANCE.activityType.value(), CantileverConfig.INSTANCE.statusMessage.value()))
				.addEventListeners(new ListenerAdapter() {
					@Override
					public void onReady(@NotNull ReadyEvent event) {
						ready();
					}
				})
				.build();

		} catch (IllegalArgumentException | IllegalStateException e) {
			LOGGER.error("API initialisation error whilst starting Cantilever", e);
		}

		this.api = api;
	}

	private void ready() {
		if (api == null) return;

		LOGGER.trace("Connected to Discord");

		long bridgeChannelId = CantileverConfig.INSTANCE.channelId.value();

		bridgeChannel = api.getChannelById(TextChannel.class, bridgeChannelId);
		if (bridgeChannel == null)
			throw new IllegalStateException("Channel with id %s could not be found".formatted(bridgeChannelId));

		LOGGER.info(
			"Cantilever connected to channel \"{}\"",
			bridgeChannel.getName()
		);

		bridgeChannel.retrieveWebhooks().queue(webhooks -> {
			Webhook existingWebhook = webhooks.stream().filter(hook -> hook.getName()
				.equals("Cantilever Bridge Webhook %s".formatted(bridgeChannelId))).findAny().orElse(null);

			if (existingWebhook != null) {
				LOGGER.info("Successfully found existing webhook for channel {}", bridgeChannelId);
				bridgeChannelWebhook = JDAWebhookClient.from(existingWebhook);
				return;
			}

			bridgeChannel.createWebhook("Cantilever Bridge Webhook " + bridgeChannelId)
				.onSuccess(webhook -> bridgeChannelWebhook = JDAWebhookClient.from(webhook))
				.queue();
		});

		BridgeEvents.init(this);
	}

	public void stop() {
		if (api != null) {
			api.shutdownNow();
		}
		if (bridgeChannelWebhook != null) {
			bridgeChannelWebhook.close();
		}
	}

	private String filterMessage(Map<String, String> map, String message) {
		final String[] replacedMessage = {message};
		map.forEach(
			(key, replacement) -> replacedMessage[0] = replacedMessage[0].replace(key, replacement)
		);
		return replacedMessage[0];
	}

	private String filterMessageM2D(String message) {
		return filterMessage(CantileverConfig.INSTANCE.m2dReplacements.value(), message);
	}

	private String filterMessageD2M(String message) {
		return filterMessage(CantileverConfig.INSTANCE.d2mReplacements.value(), message);
	}

	public void sendBasicMessageM2D(String message) {
		bridgeChannel.sendMessage(message).queue();
	}

	public void sendShutdownMessageM2D(String message) {
		bridgeChannel.sendMessage(message).complete();
	}

	public void sendWebhookMessageM2D(Component message, ServerPlayer sender) {
		if (this.bridgeChannelWebhook == null) {
			sendBasicMessageM2D(message.getString());
			LOGGER.error("Webhook does not exist in channel {}. Please make sure to allow your bot to manage webhooks!", bridgeChannel.getId());
			return;
		}
		String username = CantileverConfig.INSTANCE.useMinecraftNicknames.value() ? sender.getDisplayName().getString() : sender.getName().getString();

		//MinecraftProfileTexture skin = sender.getSessionService().getTextures(sender.getGameProfile()).skin();

		this.bridgeChannelWebhook.send(
			new WebhookMessageBuilder()
				.setUsername(username)
				.setAvatarUrl(CantileverConfig.INSTANCE.webhookFaceApi.value().formatted(sender.getGameProfile().name()))
				.append(filterMessageM2D(message.getString()))
				.build()
		);
	}

	public void sendUserMessageD2M(String author, String message) {
		sendBasicMessageD2M(
			CantileverConfig.INSTANCE.gameChatFormat.value()
				.formatted(author, message)
		);
	}

	public void sendBasicMessageD2M(String text) {
		PlayerChatMessage message = PlayerChatMessage.system(filterMessageD2M(text));
		CommandSourceStack commandSource = this.server.createCommandSourceStack();
		Component formattedText;
		if (FabricLoader.getInstance().isModLoaded("styledchat")) {
			formattedText = StyledChatUtils.formatMessage(
				message,
				commandSource,
				Cantilever.D2M_MESSAGE_TYPE
			);
		} else {
			formattedText = message.decoratedContent();
		}
		MutableComponent bridgeText =
			MutableComponent.create(new BridgeTextContent(formattedText));
		this.server.getPlayerList().broadcastSystemMessage(bridgeText, false);
	}

	public JDA api() {
		return this.api;
	}

	public Optional<TextChannel> channel() {
		return Optional.ofNullable(bridgeChannel);
	}

	public long getWebhookId() {
		return this.bridgeChannelWebhook.getId();
	}
}
