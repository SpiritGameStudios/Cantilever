package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.CantileverConfig;
import dev.spiritstudios.specter.api.core.exception.UnreachableException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;

import java.util.Objects;
import java.util.Optional;

public class Bridge {
	private DiscordApi api;
	private TextChannel bridgeChannel;
	private final MinecraftServer server;

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

				this.api.updateActivity(ActivityType.COMPETING, "with java clipboard to cause Pixel pain");

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

				BridgeEvents.init(this);
			});
	}

	public void sendBasicMessageM2D(String message) {
		MessageBuilder msg = new MessageBuilder()
			.append(message);
		msg.send(Cantilever.bridge().bridgeChannel);
	}

	public void sendWebhookMessageM2D(String message) {
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
}
