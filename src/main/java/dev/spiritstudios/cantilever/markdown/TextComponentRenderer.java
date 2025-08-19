package dev.spiritstudios.cantilever.markdown;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.commonmark.internal.renderer.NodeRendererMap;
import org.commonmark.node.Node;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.text.LineBreakRendering;

import java.util.*;

@SuppressWarnings({"unused"})
public class TextComponentRenderer {
	private final LineBreakRendering lineBreakRendering;
	private final List<TextComponentNodeRendererFactory> nodeRendererFactories;

	private TextComponentRenderer(Builder builder) {
		this.lineBreakRendering = builder.lineBreakRendering;
		nodeRendererFactories = new ArrayList<>(builder.factories.size());
		nodeRendererFactories.addAll(builder.factories);
		nodeRendererFactories.add(new TextComponentNodeRendererFactory() {
			@Override
			public NodeRenderer create(TextComponentNodeRendererContext context) {
				return new CoreTextComponentNodeRenderer(context);
			}

			@Override
			public Set<Character> getSpecialCharacters() {
				return Collections.emptySet();
			}
		});
	}

	public void render(Node node, List<MutableText> output) {
		RendererContext context = new RendererContext(new TextComponentWriter(output, lineBreakRendering));
		context.render(node);
	}

	public List<MutableText> render(Node node) {
		List<MutableText> output = new ArrayList<>();
		output.add(Text.empty());
		render(node, output);
		return output;
	}

	public static class Builder {
		private final List<TextComponentNodeRendererFactory> factories = new ArrayList<>();
		private LineBreakRendering lineBreakRendering = LineBreakRendering.COMPACT;

		public TextComponentRenderer build() {
			return new TextComponentRenderer(this);
		}

		public Builder nodeRendererFactory(TextComponentNodeRendererFactory nodeRendererFactory) {
			this.factories.add(nodeRendererFactory);
			return this;
		}

		public Builder stripNewLines(boolean stripNewLines) {
			this.lineBreakRendering = stripNewLines ? LineBreakRendering.STRIP : LineBreakRendering.COMPACT;
			return this;
		}
	}

	private class RendererContext implements TextComponentNodeRendererContext {
		private final TextComponentWriter writer;
		private final NodeRendererMap nodeRendererMap = new NodeRendererMap();
		private final Set<Character> additionalTextEscapes;

		private RendererContext(TextComponentWriter writer) {
			this.writer = writer;
			Set<Character> escapes = new HashSet<>();
			additionalTextEscapes = Collections.unmodifiableSet(escapes);

			for (var factory : nodeRendererFactories) {
				var renderer = factory.create(this);
				escapes.addAll(factory.getSpecialCharacters());
				nodeRendererMap.add(renderer);
			}
		}

		@Override
		public TextComponentWriter getWriter() {
			return writer;
		}

		@Override
		public void render(Node node) {
			nodeRendererMap.render(node);
		}

		@Override
		public Set<Character> getSpecialCharacters() {
			return additionalTextEscapes;
		}

		@Override
		public LineBreakRendering lineBreakRendering() {
			return lineBreakRendering;
		}
	}
}
