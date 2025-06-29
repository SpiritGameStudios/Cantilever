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

/*
 * Copyright (c) 2021 ItzDerock
 *
 * Changes:
 * The changes here are porting to work with Java.
 * Uses regex and logic from https://github.com/ItzDerock/discord-markdown-parser/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.spiritstudios.cantilever.util.markdown;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings({"RegExpUnexpectedAnchor", "unused"})
public class MarkdownRules {
	private static List<MarkdownRule> RULES = new ArrayList<>();
	private static final String LINK_ALT_REGEX = "(?:\\[.*]|[^\\[\\]]|](?=[^\\[]*]))+";
	private static final String LINK_REGEX = "https://[a-zA-Z0-9._-]+";

	private static int currentOrder = 0;
	public static final MarkdownRule SUBTEXT = register(new MarkdownRule(
		currentOrder++,
		inlineRegex(Pattern.compile("-# +([^\\n]+?)(\\n|$)")),
		(capture, parser, state) ->
			parser.parse(capture[2].trim(), state).getFirst()));
	public static final MarkdownRule HEADING = register(new MarkdownRule(
		currentOrder++,
		inlineRegex(Pattern.compile("(#{1,3}) +([^\\n]+?)(\\n|$)")), // Discord only allows for up to h3.
		(capture, parser, state) ->
			parser.parse(capture[2].trim(), state).getFirst()));
	public static final MarkdownRule CODE_BLOCK = register(new MarkdownRule(
		currentOrder++,
		inlineRegex(Pattern.compile("`{3}(([a-z0-9_+-.#]+?)\n+)?\n*(.+?)\n*`{3}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)),
		(capture, parser, state) -> {
			var contents = Pattern.compile("^ {4}", Pattern.DOTALL)
				.matcher(capture[0])
				.replaceAll("")
				.split("\n");
			for (String content : contents) {
				parseNotInline(parser,  "> " + content, state);
			}
			return Text.empty();
		}));
	public static final MarkdownRule BLOCK_QUOTE = register(new MarkdownRule(
		currentOrder++,
		(source, state, previous) ->
			previous.matches("![$|\\n *]") || state.inQuote ? null :
				Pattern.compile("`{3}([a-z0-9_+\\-.#]+?\n+)?\n*(.+?)\n*`{3}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(source),
		(capture, parser, state) -> {
			var content = capture[0];
			boolean isBlock = content.matches("^ *>>> ?");
			var contents = Pattern.compile(isBlock ? "^ *>>> ?" : "^ *> ?", !isBlock ? Pattern.MULTILINE : 0)
				.matcher(content)
				.replaceAll("")
				.split("\n");
			boolean prevInQuote = state.inQuote;
			state.inQuote = true;
			for (String line : contents) {
				parseNotInline(parser,  "> " + line, state);
			}
			state.inQuote = prevInQuote;
			return Text.empty();
		}));
	// TODO: List
//	public static final MarkdownRule LIST = register(new MarkdownRule(
//		currentOrder++,
//
//	));
	public static final MarkdownRule ESCAPE = register(new MarkdownRule(
		currentOrder++,
		blockRegex(Pattern.compile("^\\\\([^0-9A-Za-z\\s])")),
		(capture, parser, state) ->
			Text.literal(capture[1])
	));
	public static final MarkdownRule AUTO_LINK = register(new MarkdownRule(
		currentOrder++,
		inlineRegex(Pattern.compile("<(" + LINK_REGEX + ")>")),
		(capture, parser, state) ->
			url(capture[1], capture[1], state)
	));
	public static final MarkdownRule URL = register(new MarkdownRule(
		currentOrder++,
		inlineRegex(Pattern.compile("(" + LINK_REGEX + ")")),
		(capture, parser, state) ->
			url(capture[1], capture[1], state)
	));
	public static final MarkdownRule LINK = register(new MarkdownRule(
		currentOrder++,
		inlineRegex(Pattern.compile("\\[(" + LINK_ALT_REGEX + ")]\\((" + LINK_REGEX + ")\\)", Pattern.DOTALL)),
		(capture, parser, state) ->
			url(capture[2], capture[1], state)
	));
	public static final MarkdownRule ITALIC = register(new MarkdownRule(
		currentOrder, // Same as bold/underline.
		inlineRegex(Pattern.compile(
				// only match _s surrounding words.
				"\\b_" +
				"((?:_|\\\\[\\s\\S]|[^_])+?)_" +
				"\\b" +
				// Or match *s:
				"|" +
				// Only match *s that are followed by a non-space:
				"\\*(?=\\S)(" +
				// Match at least one of:
				"(?:" +
					//  - `**`: so that bolds inside italics don't close the
					//          italics
					"\\*\\*|" +
					//  - escape sequence: so escaped *s don't close us
					"\\\\[\\s\\S]|" +
					//  - whitespace: followed by a non-* (we don't
					//          want " *" to close an italics--it might
					//          start a list)
					"\\s+(?:\\\\[\\s\\S]|[^\\s*\\\\]|\\*\\*)|" +
					//  - non-whitespace, non-*, non-backslash characters
					"[^\\s*\\\\]" +
				")+?" +
				// followed by a non-space, non-* then *
				")\\*(?!\\*)", Pattern.DOTALL)),
		(capture, state, previousCapture) ->
			capture.length + 2, // Italic wins ties against bold and underline.
		(capture, parser, state) -> {
			String str = capture[1];
			if (capture.length > 2) {
				str = capture[2];
			}
			return parser.parse(str, state).getFirst().setStyle(Style.EMPTY.withItalic(true));
		}
	));
	public static final MarkdownRule BOLD = register(new MarkdownRule(
		currentOrder, // Same as italic/underline.
		inlineRegex(Pattern.compile("\\*\\*((?:[\\s\\S]|.)+?)\\*\\*(?!\\*)", Pattern.DOTALL)),
		(capture, state, previousCapture) ->
			capture.length + 1, // Bold wins ties against underline.
		(capture, parser, state) ->
			parser.parse(capture[1], state).getFirst().setStyle(Style.EMPTY.withBold(true))));
	public static final MarkdownRule UNDERLINE = register(new MarkdownRule(
		currentOrder++, // Same as italic/bold. Increment for next value.
		inlineRegex(Pattern.compile("__((?:[\\s\\S]|.)+?)__(?!_)", Pattern.DOTALL)),
		(capture, state, previousCapture) ->
			capture.length, // Underline loses ties against italic or bold.
		(capture, parser, state) ->
			parser.parse(capture[1], state).getFirst().setStyle(Style.EMPTY.withUnderline(true))));
	public static final MarkdownRule INLINE_CODE = register(new MarkdownRule(
		currentOrder++,
		inlineRegex(Pattern.compile("(`+)([\\s\\S]*?[^`])\1(?!`)")),
		(capture, parser, state) ->
			parser.parse("> " + capture[2].replaceAll("^ (?= *`)|(` *) $", ""), state).getFirst()));
	public static final MarkdownRule LINEBREAK = register(new MarkdownRule(
		currentOrder++,
		anyScopeRegex(Pattern.compile("\n+")),
		(capture, parser, state) -> {
			state.newLine = true;
			parser.parse(capture[0].trim(), state);
			return Text.empty();
		})
	);
	public static final MarkdownRule TEXT = register(new MarkdownRule(
		currentOrder++,
		anyScopeRegex(Pattern.compile("[\\s\\S]+?(?=[^0-9A-Za-z\\s]|\n\n|\n|\\w+:\\S|$)")),
		(capture, parser, state) ->
			Text.literal(capture[0]))
	);

	private static MarkdownRule register(MarkdownRule rule) {
		RULES.add(rule);
		return rule;
	}

	public static MarkdownRule get(int index) {
		return RULES.get(index);
	}

	public static int size() {
		return RULES.size();
	}

	public static void init() {
		RULES = RULES.stream().sorted((ruleA, ruleB) -> {
			int orderA = ruleA.order;
			int orderB = ruleB.order;

			if (orderA != orderB)
				return Integer.compare(orderA, orderB);

			int qualityA = ruleA.quality != null ? 0 : 1;
			int qualityB = ruleB.quality != null ? 0 : 1;

			if (qualityA != qualityB) {
				return Integer.compare(qualityA, qualityB);
			}

			return 0;
		}).toList();
	}

	public static MarkdownRule.MatchFunction blockRegex(Pattern regex) {
		return (source, state, previous) -> {
			if (state.inline)
				return null;

			return regex.matcher(source);
		};
	}

	public static MarkdownRule.MatchFunction inlineRegex(Pattern regex) {
		return (source, state, previous) -> {
			if (!state.inline)
				return null;

			return regex.matcher(source);
		};
	}

	public static MarkdownRule.MatchFunction anyScopeRegex(Pattern regex) {
		return (source, state, previous) -> regex.matcher(source);
	}

	public static MutableText url(String link, String title, MarkdownFormatter.State state) {
		return Text.literal(title).setStyle(Style.EMPTY
			.withColor(Formatting.BLUE)
			.withUnderline(true)
			.withClickEvent(new ClickEvent.OpenUrl(URI.create(link)))
		);
	}

	public static void parseNotInline(MarkdownFormatter.Parser parser, String content, MarkdownFormatter.State state) {
		boolean currentInline = state.inline;
		state.inline = false;
		var result = parser.parse(content, state);
		state.inline = currentInline;
	}

	public static MutableText ignoreCapture(String[] capture, MarkdownFormatter.Parser parser, MarkdownFormatter.State state) {
		return Text.empty();
	}
}
