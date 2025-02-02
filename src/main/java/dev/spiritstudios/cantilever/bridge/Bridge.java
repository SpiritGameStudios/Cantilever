package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.CantileverConfig;
import dev.spiritstudios.specter.api.core.exception.UnreachableException;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.WebhookMessageBuilder;
import org.javacord.api.entity.webhook.IncomingWebhook;
import org.javacord.api.entity.webhook.Webhook;
import org.javacord.api.entity.webhook.WebhookBuilder;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

public class Bridge {
	private DiscordApi api;
	private TextChannel bridgeChannel;
	private IncomingWebhook bridgeChannelWebhook;
	public final MinecraftServer server;

	public Bridge(MinecraftServer server) {
		this.server = server;

		if (Objects.equals(CantileverConfig.INSTANCE.token.get(), CantileverConfig.INSTANCE.token.defaultValue()))
			throw new IllegalStateException("You forgot to set your bot token in the config file! Please create a discord bot application and add it's token to the config file.");

		new DiscordApiBuilder()
			.setToken(CantileverConfig.INSTANCE.token.get())
			.addIntents(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT)
			.login()
			.thenAccept(api -> {
				this.api = api;
				Cantilever.LOGGER.trace("Connected to Discord");

				this.api.updateActivity(ActivityType.COMPETING, "the competition to cause Pixel the most pain");

				long bridgeChannelId = CantileverConfig.INSTANCE.channelId.get();

				this.bridgeChannel = this.api
					.getChannelById(bridgeChannelId)
					.orElseThrow(() ->
						new IllegalStateException("Channel with id %s could not be found".formatted(bridgeChannelId)))
					.asTextChannel()
					.orElseThrow(() ->
						new IllegalStateException("Channel with id %s is not a text channel".formatted(bridgeChannelId)));

				Cantilever.LOGGER.info(
					"Cantilever connected to channel \"{}\"",
					bridgeChannel.asServerChannel()
						.orElseThrow(UnreachableException::new)
						.getName()
				);

				this.bridgeChannel.getWebhooks().thenAccept(webhooks -> {
					Webhook existingWebhook = webhooks.stream().filter(hook -> {
							if (hook.getName().isPresent()) {
								return hook.getName().get().equals("Cantilever Bridge Webhook %s".formatted(bridgeChannelId));
							}
							return false;
						})
						.findAny().orElse(null);

					if (existingWebhook != null) {
						Cantilever.LOGGER.info("Successfully found existing webhook for channel {}", bridgeChannelId);
						this.bridgeChannelWebhook = existingWebhook.asIncomingWebhook().orElseThrow(() -> new IllegalStateException("Failed to bind existing webhook %s for channel %s".formatted(existingWebhook.getName(), bridgeChannelId)));
						return;
					}

					new WebhookBuilder(
						this.bridgeChannel.asTextableRegularServerChannel().orElseThrow(
							() -> new IllegalStateException("Failed to create webhook for channel %s".formatted(bridgeChannelId))
						)
					)
						.setName("Cantilever Bridge Webhook %s".formatted(bridgeChannelId))
						.create().thenAccept(
							incomingWebhook -> bridgeChannelWebhook = incomingWebhook
						).exceptionally(error -> {
							Cantilever.LOGGER.error("Failed to create webhook in channel {}", bridgeChannelId, error);
							return null;
						});
				});

				BridgeEvents.init(this);
			});
	}

	public void sendBasicMessageM2D(String message) {
		MessageBuilder msg = new MessageBuilder()
			.append(message);
		msg.send(Cantilever.bridge().bridgeChannel);
	}

	public void sendWebhookMessageM2D(SignedMessage message, ServerPlayerEntity sender) {
		URL avatarUrl;
		try {
			avatarUrl = URI.create(CantileverConfig.INSTANCE.webhookFaceApi.get().formatted(sender.getUuidAsString())).toURL();
		} catch (MalformedURLException e) {
			sendBasicMessageM2D(message.getContent().getString());
			return;
		}

		if (this.bridgeChannelWebhook == null) {
			sendBasicMessageM2D(message.getContent().getString());
			Cantilever.LOGGER.error("Webhook does not exist in channel {}. Please make sure to allow your bot to manage webhooks!", bridgeChannel.getId());
			return;
		}

		WebhookMessageBuilder msg = new WebhookMessageBuilder()
			.setDisplayName(sender.getName().getString())
			.setDisplayAvatar(avatarUrl)
			.append(message.getContent().getString());

		this.bridgeChannelWebhook.getLatestInstanceAsIncomingWebhook().thenAccept(msg::send);
	}

	public void sendBasicMessageD2M(BridgeTextContent textContent) {
		this.server.getPlayerManager().broadcast(MutableText.of(textContent), false);
	}

	public DiscordApi api() {
		return this.api;
	}

	public Optional<TextChannel> channel() {
		return Optional.ofNullable(bridgeChannel);
	}

	public long getWebhookId() {
		return this.bridgeChannelWebhook.getId();
	}
}
