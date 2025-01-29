package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.Cantilever;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.intent.Intent;
import org.javacord.api.entity.message.MessageBuilder;

public class Bridge {
	private final DiscordApi discordApi;
	public final TextChannel bridgeChannel;
	private final BridgeEvents bridgeEvents;
	public final MinecraftServer server;
	private final String token;

	public Bridge(String token, long bridgeChannelid, MinecraftServer server) {
		this.token = token;
		this.server = server;

		DiscordApiBuilder discordApiBuilder = new DiscordApiBuilder();
		discordApiBuilder
			.setToken(this.token)
			.addIntents(Intent.GUILD_MESSAGES, Intent.MESSAGE_CONTENT);

		this.discordApi = discordApiBuilder.login().join();
		this.discordApi.updateActivity(ActivityType.STREAMING, "pain directly to WorldWidePixel");

		this.bridgeChannel = this.discordApi
			.getChannelById(bridgeChannelid)
			.orElseThrow(
				() -> new IllegalStateException("Channel with id %s could not be found".formatted(bridgeChannelid))
			)
			.asTextChannel()
			.orElseThrow();

		this.bridgeEvents = new BridgeEvents(this);
		Cantilever.LOGGER.info("Successfully initialised Cantilever to channel %s".formatted(bridgeChannelid));
	}

	public void sendBasicMessageM2D(String message) {
		MessageBuilder msg = new MessageBuilder()
			.append(message);
		msg.send(Cantilever.BRIDGE.bridgeChannel);
	}

	public void sendWebhookMessageM2D(String message) {}

	public void sendBasicMessageD2M(BridgeTextContent textContent) {
		this.server.getPlayerManager().broadcast(MutableText.of(textContent), false);
	}

	public DiscordApi getDiscordApi() {
		return this.discordApi;
	}
}
