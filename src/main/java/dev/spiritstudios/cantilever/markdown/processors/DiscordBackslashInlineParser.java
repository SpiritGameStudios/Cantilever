package dev.spiritstudios.cantilever.markdown.processors;

import org.commonmark.internal.inline.BackslashInlineParser;
import org.commonmark.node.Text;
import org.commonmark.parser.beta.*;

import java.util.regex.Pattern;

/**
 * Escaped characters work slightly differently in Discord Markdown,
 * With a singular escape prioritizing escaping strong emphases if there are an even amount of characters.
 */
public class DiscordBackslashInlineParser extends BackslashInlineParser {
	public static final Pattern DOUBLE_ESCAPABLE = Pattern.compile("^[*~_|]");

	@Override
	public ParsedInline tryParse(InlineParserState inlineParserState) {
		Scanner scanner = inlineParserState.scanner();

		scanner.next();
		char next = scanner.peek();
		if (DOUBLE_ESCAPABLE.matcher(String.valueOf(next)).matches()) {
			int charAmount = 0;
			Position originalPos = scanner.position();
			while (scanner.hasNext()) {
				if (!scanner.next(next))
					break;
				++charAmount;
			}
			String nextToString = String.valueOf(next);
			scanner.setPosition(originalPos);
			int charsToEscape = charAmount % 2 == 0 ? 2 : 1;
			for (int i = 0; i < charsToEscape; ++i) {
				scanner.next();
			}
			return ParsedInline.of(new Text(nextToString.repeat(charsToEscape)), scanner.position());
		}

		return ParsedInline.none();
	}

	public static class Factory extends BackslashInlineParser.Factory {
		@Override
		public InlineContentParser create() {
			return new DiscordBackslashInlineParser();
		}
	}
}
