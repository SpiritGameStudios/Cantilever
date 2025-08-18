package dev.spiritstudios.cantilever.markdown;

import org.commonmark.node.*;
import org.commonmark.renderer.NodeRenderer;

import java.util.Set;
import java.util.function.BiConsumer;

import static dev.spiritstudios.cantilever.bridge.D2MFormatter.filterMessageD2M;

public class CoreTextComponentNodeRenderer extends AbstractVisitor implements NodeRenderer {

	protected final TextComponentNodeRendererContext context;
	private final TextComponentWriter writer;

	private ListHolder listHolder;

	public CoreTextComponentNodeRenderer(TextComponentNodeRendererContext context) {
		this.context = context;
		this.writer = context.getWriter();
	}

	@Override
	public Set<Class<? extends Node>> getNodeTypes() {
		return Set.of(
			BulletList.class,
			Document.class,
			Heading.class,
			Emphasis.class,
			Link.class,
			HardLineBreak.class,
			OrderedList.class,
			SoftLineBreak.class,
			StrongEmphasis.class,
			Text.class
		);
	}

	@Override
	public void render(Node node) {
		node.accept(this);
	}

	@Override
	public void visit(BulletList bulletList) {
		listHolder = new BulletListHolder(listHolder, bulletList);
		visitChildren(bulletList);
		listHolder = listHolder.parent;
	}

	@Override
	public void visit(OrderedList orderedList) {
		listHolder = new OrderedListHolder(listHolder, orderedList);
		visitChildren(orderedList);
		listHolder = listHolder.parent;
	}

	public void visit(Link link) {
		String[] title =  link.getTitle() != null ? link.getTitle().split("\n") :
			new String[] { link.getDestination() };
		writer.link(title, link.getDestination());
	}

	@Override
	public void visit(Heading heading) {
		if (heading.getLevel() > 3) {
			for (int i = 0; i < heading.getLevel(); i++) {
				writer.text("#");
			}
			writer.text(" ");
		}
		visitChildren(heading);
	}

	@Override
	public void visit(ListItem listItem) {
		if (!writer.isAtLineStart()) {
			writer.lineWithSeparator();
		}
		int markerIndent = listItem.getMarkerIndent() != null ? listItem.getMarkerIndent() : 0;
		String marker;
		if (listHolder instanceof BulletListHolder bulletListHolder) {
			marker = " ".repeat(markerIndent) + bulletListHolder.marker;
		} else if (listHolder instanceof OrderedListHolder orderedListHolder) {
			marker = " ".repeat(markerIndent) + orderedListHolder.number + orderedListHolder.delimiter;
			orderedListHolder.number++;
		} else {
			throw new IllegalStateException("Unknown list holder type: " + listHolder);
		}
		Integer contentIndent = listItem.getContentIndent();
		String spaces = contentIndent != null ? " ".repeat(Math.max(contentIndent - marker.length(), 1)) : " ";

		writer.prefix(spaces);
		writer.prefix(marker);

		writer.pushPrefix(" ".repeat(marker.length() + spaces.length()));

		visitChildren(listItem);

		writer.popPrefix();
	}

	@Override
	public void visit(SoftLineBreak softLineBreak) {
		writer.newLine();
	}

	@Override
	public void visit(HardLineBreak hardLineBreak) {
		writer.newLine();
	}

	// TODO: Discord is weird with emphases. Post-process these rather than pre-process like we are now.
	@Override
	public void visit(Emphasis emphasis) {
		format(emphasis, TextComponentWriter::withItalic);
	}

	@Override
	public void visit(StrongEmphasis emphasis) {
		if (emphasis.getOpeningDelimiter().equals("**")) {
			format(emphasis, TextComponentWriter::withBold);
		}
		if (emphasis.getOpeningDelimiter().equals("~~")) {
			format(emphasis, TextComponentWriter::withStrikethrough);
		}
		if (emphasis.getOpeningDelimiter().equals("__")) {
			format(emphasis, TextComponentWriter::withUnderline);
		}
		if (emphasis.getOpeningDelimiter().equals("||")) {
			format(emphasis, TextComponentWriter::withSpoiler);
		}
	}

	private void format(Node node, BiConsumer<TextComponentWriter, Boolean> writerAction) {
		writerAction.accept(writer, true);
		visitChildren(node);
		writerAction.accept(writer, false);
	}

	@Override
	public void visit(Text text) {
		String literal = filterMessageD2M(text.getLiteral());
		if (writer.isAtLineStart() && !literal.isEmpty()) {
			char c = literal.charAt(0);
			if (c == '\t' | c == ' ') {
				literal = literal.substring(1);
			}
		}

		writer.text(literal);
	}

	private static class ListHolder {
		final ListHolder parent;

		protected ListHolder(ListHolder parent) {
			this.parent = parent;
		}
	}

	private static class BulletListHolder extends ListHolder {
		final String marker;

		public BulletListHolder(ListHolder parent, BulletList bulletList) {
			super(parent);
			this.marker = bulletList.getMarker() != null ? bulletList.getMarker() : "-";
		}
	}

	private static class OrderedListHolder extends ListHolder {
		final String delimiter;
		private int number;

		protected OrderedListHolder(ListHolder parent, OrderedList orderedList) {
			super(parent);
			delimiter = orderedList.getMarkerDelimiter() != null ? orderedList.getMarkerDelimiter() : ".";
			number = orderedList.getMarkerStartNumber() != null ? orderedList.getMarkerStartNumber() : 1;
		}
	}
}
