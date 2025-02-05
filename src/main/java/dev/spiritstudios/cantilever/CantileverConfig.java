package dev.spiritstudios.cantilever;

import com.mojang.serialization.Codec;
import dev.spiritstudios.specter.api.config.Config;
import dev.spiritstudios.specter.api.config.ConfigHolder;
import dev.spiritstudios.specter.api.config.Value;

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
	public final Value<String> statusMessage = stringValue("")
		.build();
}
