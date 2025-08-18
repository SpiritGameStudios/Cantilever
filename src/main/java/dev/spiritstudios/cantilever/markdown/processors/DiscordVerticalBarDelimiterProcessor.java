package dev.spiritstudios.cantilever.markdown.processors;

import org.commonmark.internal.inline.EmphasisDelimiterProcessor;

/**
 * Processor for Spoiler text.
 */
public class DiscordVerticalBarDelimiterProcessor extends EmphasisDelimiterProcessor {
	public DiscordVerticalBarDelimiterProcessor() {
		super('|');
	}

	@Override
	public int getMinLength() {
		return 2;
	}
}
