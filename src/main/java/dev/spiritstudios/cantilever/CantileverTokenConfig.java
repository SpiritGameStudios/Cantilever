package dev.spiritstudios.cantilever;

import folk.sisby.kaleido.api.ReflectiveConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.ChangeWarning;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;
import net.fabricmc.loader.api.FabricLoader;

public class CantileverTokenConfig extends ReflectiveConfig {
	public static final CantileverTokenConfig INSTANCE = CantileverTokenConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", Cantilever.MODID + "-token", CantileverTokenConfig.class);
	@Comment("Your discord bot token. Read the tutorial at https://docs.spiritstudios.dev/use/cantilever for more info.")
	@ChangeWarning(Type.RequiresRestart)
	public final TrackedValue<String> token = value("<YOUR_BOT_TOKEN>");

	@Comment("You can get this TrackedValue by enabling developer mode in discord and right clicking the channel you wish to use as your bridge.")
	@ChangeWarning(Type.RequiresRestart)
	public final TrackedValue<Long> channelId = value(123456789L);
}
