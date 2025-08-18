package dev.spiritstudios.cantilever.bridge;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.external.JDAWebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.CantileverConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Bridge {
	private final JDA api;
	private TextChannel bridgeChannel;
	private WebhookClient bridgeChannelWebhook;
	public final MinecraftServer server;

	public Bridge(MinecraftServer server) {
		this.server = server;

		if (Objects.equals(CantileverConfig.INSTANCE.token.get(), CantileverConfig.INSTANCE.token.defaultValue()))
			throw new IllegalStateException("You forgot to set your bot token in the config file! Please create a discord bot application and add it's token to the config file.");

		api = JDABuilder
			.createLight(
				CantileverConfig.INSTANCE.token.get(),
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.MESSAGE_CONTENT
			)
			.setMemberCachePolicy(MemberCachePolicy.ALL) // Used for getting member from original replies.
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
	}

	private void ready() {
		Cantilever.LOGGER.trace("Connected to Discord");

		long bridgeChannelId = CantileverConfig.INSTANCE.channelId.get();

		bridgeChannel = api.getChannelById(TextChannel.class, bridgeChannelId);
		if (bridgeChannel == null)
			throw new IllegalStateException("Channel with id %s could not be found".formatted(bridgeChannelId));

		Cantilever.LOGGER.info(
			"Cantilever connected to channel \"{}\"",
			bridgeChannel.getName()
		);

		bridgeChannel.retrieveWebhooks().queue(webhooks -> {
			Webhook existingWebhook = webhooks.stream().filter(hook -> hook.getName()
				.equals("Cantilever Bridge Webhook %s".formatted(bridgeChannelId))).findAny().orElse(null);

			if (existingWebhook != null) {
				Cantilever.LOGGER.info("Successfully found existing webhook for channel {}", bridgeChannelId);
				bridgeChannelWebhook = JDAWebhookClient.from(existingWebhook);
				return;
			}

			bridgeChannel.createWebhook("Cantilever Bridge Webhook " + bridgeChannelId)
				.onSuccess(webhook -> bridgeChannelWebhook = JDAWebhookClient.from(webhook))
				.queue();
		});

		BridgeEvents.init(this);
	}

	public void sendBasicMessageM2D(String message) {
		bridgeChannel.sendMessage(message).queue();
	}

	public void sendShutdownMessageM2D(String message) {
		bridgeChannel.sendMessage(message).complete();
	}

	public void sendWebhookMessageM2D(SignedMessage message, ServerPlayerEntity sender) {
		if (this.bridgeChannelWebhook == null) {
			sendBasicMessageM2D(message.getContent().getString());
			Cantilever.LOGGER.error("Webhook does not exist in channel {}. Please make sure to allow your bot to manage webhooks!", bridgeChannel.getId());
			return;
		}
		String username = CantileverConfig.INSTANCE.useMinecraftNicknames.get() && sender.getDisplayName() != null ? sender.getDisplayName().getString() : sender.getName().getString();

		this.bridgeChannelWebhook.send(
			new WebhookMessageBuilder()
				.setUsername(username)
				.setAvatarUrl(CantileverConfig.INSTANCE.webhookFaceApi.get().formatted(sender.getUuidAsString()))
				.append(BridgeFormatter.filterMessageM2D(message.getContent().getString()))
				.build()
		);
	}

	public void sendUserMessageD2M(MessageReceivedEvent event) {
		List<Text> texts = BridgeFormatter.formatUserDiscordText(event);
		for (Text text : texts) {
			sendBasicMessageD2M(new BridgeTextContent(text));
		}
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
