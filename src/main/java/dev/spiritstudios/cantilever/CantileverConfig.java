package dev.spiritstudios.cantilever;

import folk.sisby.kaleido.api.ReflectiveConfig;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.ChangeWarning;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.Comment;
import folk.sisby.kaleido.lib.quiltconfig.api.annotations.IntegerRange;
import folk.sisby.kaleido.lib.quiltconfig.api.metadata.ChangeWarning.Type;
import folk.sisby.kaleido.lib.quiltconfig.api.values.TrackedValue;
import folk.sisby.kaleido.lib.quiltconfig.api.values.ValueMap;
import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Map;

public class CantileverConfig extends ReflectiveConfig {
	public static final CantileverConfig INSTANCE = CantileverConfig.createToml(FabricLoader.getInstance().getConfigDir(), "", Cantilever.MODID, CantileverConfig.class);
	@Comment("Use %s in your TrackedValue to slot in the game event text being sent.")
	public final TrackedValue<String> gameEventFormat = value("**%s**");

	@Comment("Use a first %s in your TrackedValue to slot in a username, and a second to slot in the chat message content.")
	public final TrackedValue<String> gameChatFormat = value("<@%s> %s");

	@Comment("Use a %s to slot in the skin texture ID / username for your head service of choice!")
	public final TrackedValue<String> webhookFaceApi = value("https://vzge.me/face/256/%s.png");

	@Comment("The delay for sending a message from Discord to Minecraft in milliseconds. Set up to make sure that Webhook related Discord Bots such as PluralKit may send messages from users..")
	@IntegerRange(min = 0L, max = Long.MAX_VALUE)
	public final TrackedValue<Long> d2mMessageDelay = value(0L);

	@Comment("The status message to show on the bridge bot")
	@ChangeWarning(value = Type.RequiresRestart, customMessage = "Requires restart or /reload")
	public final TrackedValue<String> statusMessage = value("");

	@Comment("The activity type to show on the bridge bot")
	@ChangeWarning(value = Type.RequiresRestart, customMessage = "Requires restart or /reload")
	public final TrackedValue<Activity.ActivityType> activityType = value(Activity.ActivityType.PLAYING);

	@Comment("Whether to use nicknames defined by players or Minecraft account name on Discord")
	public final TrackedValue<Boolean> useMinecraftNicknames = value(true);

	@Comment("A map of text to text replacements from MC -> Discord; useful for Styled Chat Emoji")
	public final TrackedValue<Map<String, String>> m2dReplacements = value(ValueMap.builder("").build());

	@Comment("A map of text to text replacements from Discord -> MC; useful for Styled Chat Emoji")
	public final TrackedValue<Map<String, String>> d2mReplacements = value(ValueMap.builder("").build());
}
