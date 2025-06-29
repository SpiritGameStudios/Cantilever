package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.CantileverConfig;
import dev.spiritstudios.cantilever.util.markdown.MarkdownFormatter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.List;

public class BridgeFormatter {
	public static List<Text> formatDiscordText(MessageReceivedEvent event) {
		String authorName = event.getMember() != null ?
			event.getMember().getEffectiveName() : event.getAuthor().getEffectiveName();

		MarkdownFormatter.State state = new MarkdownFormatter.State();
		Text prefix = getMinecraftPrefix(authorName, extractColor(event.getMember()));
		Text suffix = getMinecraftSuffix();

		List<Text> text = new ArrayList<>();
		MessageReference potentialReplyTo = event.getMessage().getMessageReference();
		if (potentialReplyTo != null && event.getMessage().getType() == MessageType.INLINE_REPLY && potentialReplyTo.getMessage() != null) {
			Member replyToMember = event.getGuild().getMemberById(event.getMessage().getAuthor().getId());
			String replyToAuthorName = replyToMember != null ?
				replyToMember.getEffectiveName() : potentialReplyTo.getMessage().getAuthor().getEffectiveName();
			state.prefix = Text.literal(CantileverConfig.INSTANCE.replyFormat.get().formatted(getMinecraftPrefix(replyToAuthorName, extractColor(replyToMember)).getString()));
			state.suffix = suffix;
			state.oneLine = true;
			text.addAll(MarkdownFormatter.formatDiscordMarkdown(
					potentialReplyTo.getMessage().getContentRaw(),
					state
				)
			);
		}

		Message message = event.getMessage();
		state = new MarkdownFormatter.State();
		state.prefix = prefix;
		state.suffix = suffix;
		state.oneLine = false; // TODO: One line config.
		text.addAll(MarkdownFormatter.formatDiscordMarkdown(message.getContentRaw(), state));

		for (Message.Attachment attachment : message.getAttachments()) {
			String url = attachment.getUrl();
			Text toAdd = prefixAndSuffixText(prefix, suffix, Text.literal(attachment.getFileName())
				.setStyle(Style.EMPTY.withColor(Formatting.BLUE)
					.withUnderline(true)
					.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
				));
			text.add(toAdd);
		}

		return text;
	}

	@Nullable
	private static TextColor extractColor(@Nullable Member member) {
		if (member == null)
			return null;
		var role = member.getRoles().stream().filter(role1 -> role1.getColorRaw() != 0x1FFFFFFF).max(Comparator.comparingInt(Role::getPosition));
		return role.map(value -> TextColor.fromRgb(value.getColorRaw())).orElse(null);
	}

	public static Text prefixAndSuffixText(Text prefix, Text suffix, Text content) {
		MutableText returnText = Text.empty();
		if (!prefix.equals(Text.empty()))
			returnText.append(prefix);
		if (!content.equals(Text.empty()))
			returnText.append(content);
		if (!suffix.equals(Text.empty()))
			returnText.append(suffix);
		return returnText;
	}

	private static Text getMinecraftPrefix(String discordAuthor, TextColor roleColor) {
		String[] split = CantileverConfig.INSTANCE.gameChatFormat.get().split("%s");
		if (split.length < 2)
			return Text.empty();
		Text authorWithColor = Text.literal(discordAuthor).setStyle(roleColor == null ? Style.EMPTY : Style.EMPTY.withColor(roleColor));
		return Text.empty()
			.append(split[0])
			.append(authorWithColor)
			.append(Text.literal(split[1]).setStyle(roleColor == null ? Style.EMPTY : Style.EMPTY.withColor(Formatting.WHITE)));
	}

	private static Text getMinecraftSuffix() {
		String[] split = CantileverConfig.INSTANCE.gameChatFormat.get().split("%s");
		if (split.length < 3)
			return Text.empty();
		return Text.literal(split[2]);
	}
}
