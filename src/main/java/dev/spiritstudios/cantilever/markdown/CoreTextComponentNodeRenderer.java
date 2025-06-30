package dev.spiritstudios.cantilever.markdown;

import dev.spiritstudios.cantilever.bridge.BridgeFormatter;
import org.commonmark.node.*;
import org.commonmark.renderer.NodeRenderer;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreTextComponentNodeRenderer extends AbstractVisitor implements NodeRenderer {

	protected final TextComponentNodeRendererContext context;
	private final TextComponentWriter writer;

	private ListHolder listHolder;

	private final Pattern orderedListMarkerPattern = Pattern.compile("^([0-9]{1,9})([.)])");

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

	@Override
	public void visit(Emphasis emphasis) {
		writer.withItalic(true);
		super.visit(emphasis);
		writer.withItalic(false);
	}

	@Override
	public void visit(StrongEmphasis emphasis) {
		if (emphasis.getOpeningDelimiter().equals("**")) {
			writer.withBold(true);
			super.visit(emphasis);
			writer.withBold(false);
		}
		if (emphasis.getOpeningDelimiter().equals("~~")) {
			writer.withStrikethrough(true);
			super.visit(emphasis);
			writer.withStrikethrough(false);
		}
		if (emphasis.getOpeningDelimiter().equals("__")) {
			writer.withUnderline(true);
			super.visit(emphasis);
			writer.withUnderline(false);
		}
	}

	@Override
	public void visit(Text text) {
		String literal = BridgeFormatter.filterMessageD2M(text.getLiteral());
		if (writer.isAtLineStart() && !literal.isEmpty()) {
			char c = literal.charAt(0);
			switch (c) {
				case '-': {
					// Would be ambiguous with a bullet list marker, escape
					writer.text("\\-");
					literal = literal.substring(1);
					break;
				}
				case '#': {
					// Would be ambiguous with an ATX heading, escape
					writer.text("\\#");
					literal = literal.substring(1);
					break;
				}
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9': {
					// Check for ordered list marker
					Matcher m = orderedListMarkerPattern.matcher(literal);
					if (m.find()) {
						writer.text(m.group(1));
						writer.text(m.group(2));
						literal = literal.substring(m.end());
					}
					break;
				}
				case '\t' | ' ': {
					writer.text("");
					literal = literal.substring(1);
					break;
				}
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
