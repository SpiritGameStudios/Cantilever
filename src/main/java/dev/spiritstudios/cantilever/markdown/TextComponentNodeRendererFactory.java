package dev.spiritstudios.cantilever.markdown;

import org.commonmark.renderer.NodeRenderer;

import java.util.Set;

public interface TextComponentNodeRendererFactory {
	NodeRenderer create(TextComponentNodeRendererContext context);

	Set<Character> getSpecialCharacters();
}
