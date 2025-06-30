package dev.spiritstudios.cantilever.markdown.processors;

import org.commonmark.internal.inline.EmphasisDelimiterProcessor;

public class StrikethroughDelimiterProcessor extends EmphasisDelimiterProcessor {
	public static final StrikethroughDelimiterProcessor INSTANCE = new StrikethroughDelimiterProcessor();

	protected StrikethroughDelimiterProcessor() {
		super('~');
	}

	@Override
	public int getMinLength() {
		return 2;
	}
}
