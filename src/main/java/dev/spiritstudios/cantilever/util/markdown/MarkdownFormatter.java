/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Khan Academy, Aria Buckles, Christopher Jeffrey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.spiritstudios.cantilever.util.markdown;

import dev.spiritstudios.cantilever.Cantilever;
import dev.spiritstudios.cantilever.bridge.BridgeFormatter;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

public class MarkdownFormatter {
	public static List<Text> formatDiscordMarkdown(String discordContent, State defaultState) {
		if (discordContent.isBlank())
			return Collections.emptyList();

		Parser outerParse = (capture, state) -> {
			state.copyFrom(defaultState);
			state.previousCapture = null;
			return nestedParser(capture, state);
		};

		return outerParse.parse(discordContent, new State()).stream()
			.map(mutableText -> BridgeFormatter.prefixAndSuffixText(defaultState.prefix, defaultState.suffix, mutableText))
			.toList();
	}

	private static List<MutableText> nestedParser(String source, MarkdownFormatter.State state) {
		List<MutableText> texts = new ArrayList<>();
		texts.add(Text.empty());
		while (!source.isBlank()) {
			MarkdownRule rule = null;
			String[] capture = null;
			int quality = -1;

			MarkdownRule currentRule = MarkdownRules.get(0);
			int currentOrder;

			int i = 0;

			do {
				currentOrder = currentRule.order;
				String previousCaptureString = state.previousCapture == null ? "" : state.previousCapture[0];
				var matcher = currentRule.match.matcher(source, state, previousCaptureString);
				if (matcher != null) {
					var currentCapture = createMatches(matcher);
					if (currentCapture.length > 0) {
						int currentQuality = currentRule.quality != null ?
							currentRule.quality.quality(currentCapture, state, previousCaptureString) : 0;
						if (!(currentQuality <= quality)) {
							rule = currentRule;
							capture = currentCapture;
							quality = currentQuality;
						}
					}
				}
				++i;
				if (i < MarkdownRules.size()) {
					currentRule = MarkdownRules.get(i);
				}
			} while (
				i < MarkdownRules.size() && currentRule != null &&
					capture == null ||
					(currentRule != null && currentRule.order == currentOrder && currentRule.quality != null)
			);

			if (rule == null) {
				Cantilever.LOGGER.error("Could not find a matching rule for the source. " +
					"The rule with the highest 'order' should always match source provided to it." +
					"Check the defintion of 'match' for the last markdown rule." +
					"It seems to not match the following source: \n{}", source);
				return Collections.emptyList();
			}

			MutableText parsed = rule.parse.parse(capture, MarkdownFormatter::nestedParser, state);
			if (state.newLine) {
				texts.add(Text.empty());
				state.newLine = false;
			}
			if (!parsed.getString().isBlank()) {
				texts.getLast().append(parsed);
			}

			state.previousCapture = capture;
			source = source.substring(state.previousCapture[0].length());

			Style original = parsed.getStyle();
			if (!source.isEmpty() && shouldReset(original)) {
				Style style = Style.EMPTY;
				style = original.isBold() ? style.withBold(false) : style;
				style = original.getColor() != null ? style.withColor(Formatting.WHITE) : style;
				style = original.getClickEvent() != null ? style.withClickEvent(null) : style;
				style = original.isItalic() ? style.withItalic(false) : style;
				style = original.isStrikethrough() ? style.withStrikethrough(false) : style;
				style = original.isUnderlined() ? style.withUnderline(false) : style;
				texts.getLast().append(Text.literal("").setStyle(style));
			}
		}
		return texts;
	}

	private static boolean shouldReset(Style style) {
		return style.isBold() ||
			style.getColor() != null ||
			style.getClickEvent() != null ||
			style.isItalic() ||
			style.isStrikethrough() ||
			style.isUnderlined();
	}

	@Nullable
	private static String[] createMatches(@NotNull Matcher matcher) {
		return matcher.results().map(matchResult -> matcher.group()).toArray(String[]::new);
	}

	private static Text reduceToOneLine(List<Text> texts) {
		MutableText text = Text.empty();
		for (Text line : texts) {
			if (!text.getSiblings().isEmpty()) {
				text.append(" ");
			}
			text.append(line);
		}
		return text;
	}

	public interface Parser {
		List<MutableText> parse(String source, MarkdownFormatter.State state);
	}

	public static class State {
		public boolean inline = true;
		public boolean inQuote, oneLine, newLine = false;
		public Text prefix, suffix;
		public String[] previousCapture;

		private void copyFrom(State other) {
			inline = other.inline || inline;

			inQuote = other.inQuote || inQuote;
			oneLine = other.oneLine || oneLine;
			newLine = other.newLine || newLine;

			prefix = other.prefix;
			suffix = other.suffix;

			previousCapture = other.previousCapture;
		}
	}
}
