package dev.spiritstudios.cantilever.markdown.processors;

import org.commonmark.node.Link;
import org.commonmark.node.Text;
import org.commonmark.parser.InlineParserContext;
import org.commonmark.parser.beta.*;

import java.util.regex.Pattern;

public class DiscordLinkProcessor implements LinkProcessor {
	private static final Pattern URL = Pattern.compile("^https?://[-a-zA-Z0-9-._~:/?#\\[\\]@!$&'()*+,;=]+$");

	public DiscordLinkProcessor() {
	}

	@Override
	public LinkResult process(LinkInfo linkInfo, Scanner scanner, InlineParserContext context) {
		if (!URL.matcher(linkInfo.destination()).matches()) {
			Position prevPosition = scanner.position();
			scanner.setPosition(linkInfo.afterTextBracket());
			String text = linkInfo.openingBracket().getLiteral() +
				linkInfo.text() +
				"]" +
				scanner.peek() +
				linkInfo.destination() +
				")";
			scanner.setPosition(prevPosition);
			return LinkResult.replaceWith(new Text(text), scanner.position());
		}
		String title = linkInfo.text() == null ? linkInfo.destination() : linkInfo.text();
		return LinkResult.wrapTextIn(new Link(linkInfo.destination(), title), scanner.position());
	}
}
