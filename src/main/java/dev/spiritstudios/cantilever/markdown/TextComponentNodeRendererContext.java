package dev.spiritstudios.cantilever.markdown;

import org.commonmark.node.Node;
import org.commonmark.renderer.text.LineBreakRendering;

import java.util.Set;

public interface TextComponentNodeRendererContext {
	TextComponentWriter getWriter();

	void render(Node node);

	Set<Character> getSpecialCharacters();

	LineBreakRendering lineBreakRendering();
}
