package dev.spiritstudios.cantilever;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.spiritstudios.specter.api.config.Config;
import dev.spiritstudios.specter.api.config.ConfigHolder;
import dev.spiritstudios.specter.api.config.Value;
import net.dv8tion.jda.api.entities.Activity;

import java.util.List;

public class CantileverConfig extends Config<CantileverConfig> {
	public static final ConfigHolder<CantileverConfig, ?> HOLDER = ConfigHolder.builder(Cantilever.id(Cantilever.MODID), CantileverConfig.class)
		.build();

	public static final CantileverConfig INSTANCE = HOLDER.get();

	public final Value<String> token = stringValue("<YOUR_BOT_TOKEN>").build();

	public final Value<Long> channelId = value(123456789L, Codec.LONG)
		.comment("You can get this value by enabling developer mode in discord and right clicking the channel you wish to use as your bridge.")
		.build();

	public final Value<String> gameEventFormat = stringValue("**%s**")
		.comment("Use %s in your value to slot in the game event text being sent.")
		.build();

	public final Value<String> gameChatFormat = stringValue("<@%s> %s")
		.comment("Use a first %s in your value to slot in a username, and a second to slot in the chat message content.")
		.build();

	public final Value<String> webhookFaceApi = stringValue("https://vzge.me/face/256/%s.png")
		.comment("Use a %s slot to set the player UUID for your head service of choice!")
		.build();

	public final Value<Long> d2mMessageDelay = value(0L, Codec.LONG)
		.comment("The delay for sending a message from Discord to Minecraft in milliseconds. Set up to make sure that Webhook related Discord Bots such as PluralKit and Tupperbox may send messages from users..")
		.build();

	public final Value<WebhooksForRemoval> webhooksForRemoval = value(WebhooksForRemoval.DEFAULT, WebhooksForRemoval.CODEC)
		.comment("Discord Webhooks to attempt to not send any original messages from. 'Original messages' are classified as being posted from a user before a webhook is sent and having matching content within the message. (Will only function when d2mMessageDelay has been set).")
		.build();

	public final Value<Integer> webhookMessagesToCheck = intValue(3)
		.comment("The amount of messages after the initial message to search through to find a webhooksForRemoval webhook. (Will only function when d2mMessageDelay has been set).")
		.build();

	public final Value<String> statusMessage = stringValue("")
		.build();

	public final Value<Activity.ActivityType> activityType = enumValue(Activity.ActivityType.PLAYING, Activity.ActivityType.class)
		.comment("Options: [playing, streaming, listening, watching, competing]")
		.build();

	public final Value<Boolean> useMinecraftNicknames = booleanValue(true)
		.comment("Whether to use nicknames defined by players or Minecraft account name on Discord")
		.build();

	public record WebhooksForRemoval(List<Long> webhookIds, boolean inverted) {
		public static final WebhooksForRemoval DEFAULT = new WebhooksForRemoval(List.of(), true);
		public static final Codec<WebhooksForRemoval> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.LONG.listOf().fieldOf("webhookIds").forGetter(WebhooksForRemoval::webhookIds),
			Codec.BOOL.fieldOf("inverted").forGetter(WebhooksForRemoval::inverted)
		).apply(inst, WebhooksForRemoval::new));
	}
}
