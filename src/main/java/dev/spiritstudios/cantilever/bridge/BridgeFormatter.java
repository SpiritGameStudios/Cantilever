package dev.spiritstudios.cantilever.bridge;

import dev.spiritstudios.cantilever.CantileverConfig;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.*;

public class BridgeFormatter {
	public static List<Text> formatDiscordText(MessageReceivedEvent event) {
		String authorName = event.getMember() != null ?
			event.getMember().getEffectiveName() : event.getAuthor().getEffectiveName();

		Text prefix = getMinecraftPrefix(authorName);
		Text suffix = getMinecraftSuffix();

		Message message = event.getMessage();
		List<Text> text = formatRawDiscordContent(message.getContentRaw(), prefix, suffix, !message.getAttachments().isEmpty());

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

	private static List<Text> formatRawDiscordContent(String discordContent, Text prefix, Text suffix, boolean hasAttachments) {
		List<Text> text = new ArrayList<>();
		if (discordContent.isEmpty() && hasAttachments)
			return text;

		StringBuilder currentString = new StringBuilder();

		Deque<String> formatting = new ArrayDeque<>();

		MutableText lineText;
		Style currentStyle = Style.EMPTY;

		String[] lines = discordContent.split("\n");
		String currentLine = lines[0];

		for (int line = 0; line < lines.length; ++line) {
			lineText = Text.empty();

			for (int index = 0; index < currentLine.length(); ++index) {
				char ch = currentLine.charAt(index);
				switch (ch) {
					case '*' -> {
						boolean doubleChar = index + 1 < currentLine.length() && currentLine.charAt(index + 1) == '*';
						boolean notDoubleChar = index + 1 >= currentLine.length() || currentLine.charAt(index + 1) != '*';
						boolean hasDoubleCharLater = index + 2 < currentLine.length() && currentLine.substring(index + 2).contains("**");

						if (doubleChar && (formatting.contains("bold") || hasDoubleCharLater)) {
							if (!currentString.isEmpty()) {
								lineText.append(Text.literal(currentString.toString()).setStyle(currentStyle));
							}

							if (formatting.contains("bold")) {
								formatting.remove("bold");
								currentStyle = currentStyle.withBold(false);
							} else {
								formatting.push("bold");
								currentStyle = currentStyle.withBold(true);
							}

							++index;
							currentString = new StringBuilder();
							continue;
						} else if (formatting.contains("italic") || notDoubleChar) {
							if (!currentString.isEmpty()) {
								lineText.append(Text.literal(currentString.toString()).setStyle(currentStyle));
							}

							if (formatting.contains("italic")) {
								formatting.remove("italic");
								currentStyle = currentStyle.withItalic(false);
							} else {
								formatting.push("italic");
								currentStyle = currentStyle.withItalic(true);
							}

							currentString = new StringBuilder();
							continue;
						}
					}
					case '_' -> {
						boolean doubleChar = index + 1 < currentLine.length() && currentLine.charAt(index + 1) == '_';
						boolean notDoubleChar = index + 1 >= currentLine.length() || currentLine.charAt(index + 1) != '_';
						boolean hasDoubleCharLater = index + 2 < currentLine.length() && currentLine.substring(index + 2).contains("__");
						if (doubleChar && (formatting.contains("underline") || hasDoubleCharLater)) {
							if (!currentString.isEmpty()) {
								lineText.append(Text.literal(currentString.toString()).setStyle(currentStyle));
							}

							if (formatting.contains("underline")) {
								formatting.remove("underline");
								currentStyle = Style.EMPTY.withParent(currentStyle)
									.withUnderline(false);
							} else {
								formatting.push("underline");
								currentStyle = Style.EMPTY.withParent(currentStyle)
									.withUnderline(true);
							}

							currentString = new StringBuilder();
							++index;
							continue;
						} else if (formatting.contains("italic") || notDoubleChar) {
							if (!currentString.isEmpty()) {
								lineText.append(Text.literal(currentString.toString()).setStyle(currentStyle));
							}

							if (formatting.contains("italic")) {
								formatting.remove("italic");
								currentStyle = Style.EMPTY.withParent(currentStyle)
									.withItalic(false);
							} else {
								formatting.push("italic");
								currentStyle = Style.EMPTY.withParent(currentStyle)
									.withItalic(true);
							}

							currentString = new StringBuilder();
							continue;
						}
					}
					default -> {}
				}
				currentString.append(ch);
			}

			if (!currentString.isEmpty()) {
				lineText.append(Text.literal(currentString.toString()).setStyle(currentStyle));
				currentString = new StringBuilder();
			}

			text.add(prefixAndSuffixText(
				prefix,
				suffix,
				lineText
			));

			int newLine = line + 1;
			if (newLine < lines.length) {
				currentLine = lines[line + 1];
			}
		}
		return text;
	}

	private static Text prefixAndSuffixText(Text prefix, Text suffix, Text content) {
		MutableText returnText = Text.empty();
		if (!prefix.equals(Text.empty()))
			returnText.append(prefix);
		if (!content.equals(Text.empty()))
			returnText.append(content);
		if (!suffix.equals(Text.empty()))
			returnText.append(suffix);
		return returnText;
	}

	private static Text getMinecraftPrefix(String discordAuthor) {
		String[] split = CantileverConfig.INSTANCE.gameChatFormat.get().split("%s");
		if (split.length < 2)
			return Text.empty();
		return Text.literal(split[0] + discordAuthor + split[1]);
	}

	private static Text getMinecraftSuffix() {
		String[] split = CantileverConfig.INSTANCE.gameChatFormat.get().split("%s");
		if (split.length < 3)
			return Text.empty();
		return Text.literal(split[2]);
	}
}
