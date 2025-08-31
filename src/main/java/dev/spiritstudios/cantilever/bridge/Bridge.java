package dev.spiritstudios.cantilever.bridge;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import dev.spiritstudios.cantilever.CantileverConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

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
			if (Objects.equals(CantileverConfig.INSTANCE.token.get(), CantileverConfig.INSTANCE.token.defaultValue()))
				throw new IllegalStateException("You forgot to set your bot token in the config file! Please create a discord bot application and add it's token to the config file. We'll sit here and wait.");

			api = JDABuilder
				.createLight(
					CantileverConfig.INSTANCE.token.get(),
					GatewayIntent.GUILD_MESSAGES,
					GatewayIntent.MESSAGE_CONTENT
				)
				.setActivity(CantileverConfig.INSTANCE.statusMessage.get().isEmpty() ?
					null :
					Activity.of(CantileverConfig.INSTANCE.activityType.get(), CantileverConfig.INSTANCE.statusMessage.get()))
				.addEventListeners(new ListenerAdapter() {
					@Override
					public void onReady(@NotNull ReadyEvent event) {
						ready();
					}
				})
				.build();

		} catch (IllegalArgumentException e) {
			LOGGER.error("API initialisation error whilst starting Cantilever", e);
		}

		this.api = api;
	}

	private void ready() {
		if (api == null) return;

		LOGGER.trace("Connected to Discord");

		long bridgeChannelId = CantileverConfig.INSTANCE.channelId.get();

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

	private String filterMessage(Map<String, String> map, String message) {
		final String[] replacedMessage = {message};
		map.forEach(
			(key, replacement) -> replacedMessage[0] = replacedMessage[0].replace(key, replacement)
		);
		return replacedMessage[0];
	}

	private String filterMessageM2D(String message) {
		return filterMessage(CantileverConfig.INSTANCE.m2dReplacements.get(), message);
	}

	private String filterMessageD2M(String message) {
		return filterMessage(CantileverConfig.INSTANCE.d2mReplacements.get(), message);
	}

	public void sendBasicMessageM2D(String message) {
		bridgeChannel.sendMessage(message).queue();
	}

	public void sendShutdownMessageM2D(String message) {
		bridgeChannel.sendMessage(message).complete();
	}

	public void sendWebhookMessageM2D(Text message, ServerPlayerEntity sender) {
		if (this.bridgeChannelWebhook == null) {
			sendBasicMessageM2D(message.getString());
			LOGGER.error("Webhook does not exist in channel {}. Please make sure to allow your bot to manage webhooks!", bridgeChannel.getId());
			return;
		}
		String username = CantileverConfig.INSTANCE.useMinecraftNicknames.get() && sender.getDisplayName() != null ? sender.getDisplayName().getString() : sender.getName().getString();

		// OB*BO CENSORING
		Pattern direCurse = Pattern.compile("[oO0](?:[^0-9a-zA-Z]*|\\s)*[bB](?:[^0-9a-zA-Z]*|\\s)*[aA](?:[^0-9a-zA-Z]*|\\s)[bB](?:[^0-9a-zA-Z]*|\\s)*[oO0]");
		if (direCurse.matcher(message.getString()).find()) {
			this.bridgeChannelWebhook.send(
				new WebhookMessageBuilder()
					.setUsername(username)
					.setAvatarUrl(CantileverConfig.INSTANCE.webhookFaceApi.get().formatted(sender.getUuidAsString()))
					.append("**%s succumbed to the dire curse**".formatted(username))
					.build()
			);
			return;
		}

		this.bridgeChannelWebhook.send(
			new WebhookMessageBuilder()
				.setUsername(username)
				.setAvatarUrl(CantileverConfig.INSTANCE.webhookFaceApi.get().formatted(sender.getUuidAsString()))
				.append(filterMessageM2D(message.getString()))
				.build()
		);
	}

	public void sendUserMessageD2M(String author, String message) {
		sendBasicMessageD2M(new BridgeTextContent(
			Text.of(CantileverConfig.INSTANCE.gameChatFormat.get().formatted(
				author, filterMessageD2M(message)
			)),
			true
		));
	}

	public void sendBasicMessageD2M(BridgeTextContent textContent) {
		this.server.getPlayerManager().broadcast(MutableText.of(textContent), false);
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
