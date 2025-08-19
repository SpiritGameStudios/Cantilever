package dev.spiritstudios.cantilever.markdown.processors;

import org.commonmark.internal.inline.EmphasisDelimiterProcessor;

/**
 * Processor for Strikethrough text.
 */
public class DiscordTildeDelimiterProcessor extends EmphasisDelimiterProcessor {
	public DiscordTildeDelimiterProcessor() {
		super('~');
	}

	@Override
	public int getMinLength() {
		return 2;
	}
}
