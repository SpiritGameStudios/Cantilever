/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Khan Academy, Aria Buckles, Christopher Jeffrey
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.spiritstudios.cantilever.util.markdown;

import net.minecraft.text.MutableText;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;

public class MarkdownRule {
	public final int order;
	public final MatchFunction match;
	@Nullable
	public final QualityFunction quality;
	public final ParseFunction parse;

	protected MarkdownRule(int order, MatchFunction match, ParseFunction parse) {
		this(order, match, null, parse);
	}

	protected MarkdownRule(int order, MatchFunction match, @Nullable QualityFunction quality, ParseFunction parse) {
		this.order = order;
		this.match = match;
		this.quality = quality;
		this.parse = parse;
	}

	@FunctionalInterface
	public interface MatchFunction {
		Matcher matcher(String source, MarkdownFormatter.State state, String previousSource);
	}

	@FunctionalInterface
	public interface QualityFunction {
		int quality(String[] capture, MarkdownFormatter.State state, String previousCapture);
	}

	public interface ParseFunction {
		MutableText parse(String[] capture, MarkdownFormatter.Parser nestedParser, MarkdownFormatter.State state);
	}
}
