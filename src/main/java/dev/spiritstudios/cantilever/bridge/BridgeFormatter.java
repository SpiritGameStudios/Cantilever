package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.CantileverConfig;
import dev.spiritstudios.cantilever.markdown.TextComponentRenderer;
import dev.spiritstudios.cantilever.markdown.processors.DiscordLinkProcessor;
import dev.spiritstudios.cantilever.markdown.processors.StrikethroughDelimiterProcessor;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.*;
import java.util.List;

public class BridgeFormatter {
	public static List<Text> formatUserDiscordText(MessageReceivedEvent event) {
		String authorName = event.getMember() != null ?
			event.getMember().getEffectiveName() : event.getAuthor().getEffectiveName();

		Text prefix = getMinecraftPrefix(authorName, extractColor(event.getMember()));
		Text suffix = getMinecraftSuffix();

		List<Text> text = new ArrayList<>();
		MessageReference replyTo = event.getMessage().getMessageReference();
		if (replyTo != null && event.getMessage().getType() == MessageType.INLINE_REPLY && replyTo.getMessage() != null) {
			String replyToAuthorName = replyTo.getMessage().getMember() != null ?
				replyTo.getMessage().getMember().getEffectiveName() : replyTo.getMessage().getAuthor().getEffectiveName();
			text.addAll(formatDiscordMarkdown(
				replyTo.getMessage().getContentRaw(),
				createReplyPrefix(replyToAuthorName, extractColor(replyTo.getMessage().getMember())), suffix, true));
		}

		Message message = event.getMessage();
		text.addAll(formatDiscordMarkdown(message.getContentRaw(),
			prefix, suffix,
			false));

		for (Message.Attachment attachment : message.getAttachments()) {
			String url = attachment.getUrl();
			Text toAdd = prefixAndSuffixText(Text.empty().append("⛓").append(prefix), suffix, Text.literal(attachment.getFileName())
				.setStyle(Style.EMPTY.withColor(Formatting.BLUE)
					.withUnderline(true)
					.withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
				));
			text.add(toAdd);
		}

		return text;
	}

	private static final TextComponentRenderer RENDERER = new TextComponentRenderer.Builder()
		.build();
	private static final TextComponentRenderer SINGLE_LINE_RENDERER = new TextComponentRenderer.Builder()
		.stripNewLines(true)
		.build();

	public static List<Text> formatDiscordMarkdown(String discordContent, Text prefix, Text suffix, boolean singleLine) {
		if (discordContent.isBlank())
			return Collections.emptyList();

		Parser parser = Parser.builder()
			.linkProcessor(DiscordLinkProcessor.INSTANCE)
			.customDelimiterProcessor(StrikethroughDelimiterProcessor.INSTANCE)
			.build();
		Node node = parser.parse(discordContent);
		var renderer = singleLine ? SINGLE_LINE_RENDERER : RENDERER;
		return renderer.render(node).stream()
			.map(mutableText -> prefixAndSuffixText(prefix, suffix, mutableText))
			.toList();
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

	public static String filterMessageM2D(String message) {
		return filterMessage(CantileverConfig.INSTANCE.m2dReplacements.get(), message);
	}

	public static String filterMessageD2M(String message) {
		return filterMessage(CantileverConfig.INSTANCE.d2mReplacements.get(), message);
	}

	private static String filterMessage(Map<String, String> map, String message) {
		final String[] replacedMessage = {message};
		map.forEach(
			(key, replacement) -> replacedMessage[0] = replacedMessage[0].replace(key, replacement)
		);
		return replacedMessage[0];
	}

	private static List<MutableText> reduceToOneLine(List<MutableText> texts) {
		MutableText text = Text.empty();
		for (Text line : texts) {
			if (!text.getSiblings().isEmpty()) {
				text.append(" ");
			}
			text.append(line);
		}
		return List.of(text);
	}

	@Nullable
	private static TextColor extractColor(@Nullable Member member) {
		if (member == null)
			return null;
		var role = member.getRoles().stream().filter(role1 -> role1.getColorRaw() != 0x1FFFFFFF).max(Comparator.comparingInt(Role::getPosition));
		return role.map(value -> TextColor.fromRgb(value.getColorRaw())).orElse(null);
	}

	private static Text getMinecraftPrefix(String authorName, TextColor roleColor) {
		String[] split = CantileverConfig.INSTANCE.gameChatFormat.get().split("%s");
		if (split.length < 2)
			return Text.empty();
		Text authorWithColor = Text.literal(authorName).setStyle(roleColor == null ? Style.EMPTY : Style.EMPTY.withColor(roleColor));
		MutableText text = Text.empty();
		return text.append(split[0])
			.append(authorWithColor)
			.append(Text.literal(split[1]).setStyle(roleColor == null ? Style.EMPTY : Style.EMPTY.withColor(Formatting.WHITE)));
	}

	private static Text getMinecraftSuffix() {
		String[] split = CantileverConfig.INSTANCE.gameChatFormat.get().split("%s");
		if (split.length < 3)
			return Text.empty();
		return Text.literal(split[2]);
	}

	private static Text createReplyPrefix(String authorName, TextColor roleColor) {
		String[] replySplit = CantileverConfig.INSTANCE.replyFormat.get().split("%s");
		MutableText text = Text.empty();
		if (replySplit.length > 0) {
			text.append(replySplit[0]);
		}
		text.append(getMinecraftPrefix(authorName, roleColor));
		if (replySplit.length > 1) {
			text.append(replySplit[1]);
		}
		return text;
	}
}
