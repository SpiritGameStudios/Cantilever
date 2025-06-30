package dev.spiritstudios.cantilever.markdown;

import net.minecraft.text.*;
import org.commonmark.renderer.text.LineBreakRendering;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

public class TextComponentWriter {
	private static final int WHITE = 16777215;

	private final List<MutableText> texts;
	private final LineBreakRendering lineBreakRendering;

	private int lines = 0;
	private boolean isBold, isItalic, isStrikethrough, isUnderline = false;
	private boolean previousBoldState, previousItalicState, previousStrikethroughState, previousUnderlineState = false;
	private int textColor = -1;
	private int previousTextColor = -1;
	private boolean atLineStart = true;

	private final LinkedList<String> prefixes = new LinkedList<>();

	public TextComponentWriter(List<MutableText> out, LineBreakRendering lineBreakRendering) {
		this.texts = out;
		this.lineBreakRendering = lineBreakRendering;
	}

	public void withBold(boolean bold) {
		this.isBold = bold;
	}

	public void withItalic(boolean italic) {
		this.isItalic = italic;
	}

	public void withStrikethrough(boolean strikethrough) {
		this.isStrikethrough = strikethrough;
	}

	public void withUnderline(boolean underline) {
		this.isUnderline = underline;
	}

	public void withColor(int color) {
		this.textColor = color;
	}

	public void text(String s) {
		if (s.isEmpty())
			return;
		flushLines();
		texts.getLast().append(getText(s));
		atLineStart = false;
	}

	public void prefix(String s) {
		if (s.isEmpty())
			return;
		flushLines();
		texts.set(texts.size() - 1, Text.empty()
			.append(getText(s))
			.append(texts.getLast()));
		atLineStart= false;
	}

	public void pushPrefix(String s) {
		prefixes.addLast(s);
	}

	public void popPrefix() {
		prefixes.removeLast();
	}

	public void newLine() {
		if (lineBreakRendering == LineBreakRendering.STRIP)
			return;
		texts.add(Text.empty());
		atLineStart = true;
	}

	public void link(String[] title, String url) {
		Style style = Style.EMPTY.withColor(5592575).withUnderline(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(url)));
		for (int i = 0; i < title.length; ++i) {
			if (i > 0) {
				newLine();
			}
			texts.getLast().append(Text.literal(title[i]).setStyle(style));
		}
		texts.getLast().append(Text.literal("").setStyle(Style.EMPTY.withColor(WHITE).withUnderline(false)));
	}

	public void lineWithSeparator() {
		lines = 1;
		atLineStart = lineBreakRendering != LineBreakRendering.STRIP;
	}

	public boolean isAtLineStart() {
		return atLineStart;
	}

	private void flushLines() {
		if (lines != 0) {
			if (lineBreakRendering != LineBreakRendering.STRIP) {
				newLine();
			}
			writePrefixes();
			lines = 0;
		}
	}

	private void writePrefixes() {
		if (!prefixes.isEmpty()) {
			for (String prefix : prefixes) {
				text(prefix);
			}
		}
	}

	private MutableText getText(String string) {
		MutableText literal = Text.literal(string);
		if (isBold != previousBoldState) {
			literal.setStyle(literal.getStyle().withBold(isBold));
			previousBoldState = !isBold;
		}
		if (isItalic != previousItalicState) {
			literal.setStyle(literal.getStyle().withItalic(isItalic));
			previousItalicState = !isItalic;
		}
		if (isStrikethrough != previousStrikethroughState) {
			literal.setStyle(literal.getStyle().withStrikethrough(isStrikethrough));
			previousStrikethroughState = !isStrikethrough;
		}
		if (isUnderline != previousUnderlineState) {
			literal.setStyle(literal.getStyle().withUnderline(isUnderline));
			previousUnderlineState = !isUnderline;
		}
		if (textColor != previousTextColor) {
			literal.setStyle(literal.getStyle().withColor(TextColor.fromRgb(textColor != -1 ? WHITE : textColor)));
			previousTextColor = textColor;
		}
		return literal;
	}
}
