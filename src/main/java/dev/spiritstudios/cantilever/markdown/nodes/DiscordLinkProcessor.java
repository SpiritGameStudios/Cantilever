package dev.spiritstudios.cantilever.markdown.nodes;

import org.commonmark.node.Link;
import org.commonmark.parser.InlineParserContext;
import org.commonmark.parser.beta.LinkInfo;
import org.commonmark.parser.beta.LinkProcessor;
import org.commonmark.parser.beta.LinkResult;
import org.commonmark.parser.beta.Scanner;

public class DiscordLinkProcessor implements LinkProcessor {
	public static final DiscordLinkProcessor INSTANCE = new DiscordLinkProcessor();

	private DiscordLinkProcessor() {
	}

	@Override
	public LinkResult process(LinkInfo linkInfo, Scanner scanner, InlineParserContext context) {
		return LinkResult.wrapTextIn(new Link(linkInfo.destination(), linkInfo.text()), scanner.position());
	}
}
