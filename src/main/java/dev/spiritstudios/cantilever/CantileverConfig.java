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
}
