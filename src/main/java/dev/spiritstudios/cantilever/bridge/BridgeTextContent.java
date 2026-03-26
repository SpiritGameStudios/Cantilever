package dev.spiritstudios.cantilever.bridge;

import Type;
import com.mojang.serialization.MapCodec;
import dev.spiritstudios.cantilever.Cantilever;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.text.*;

import java.util.Optional;

public record BridgeTextContent(Component content) implements ComponentContents {
	public static MapCodec<BridgeTextContent> CODEC = MapCodec.assumeMapUnsafe(ComponentSerialization.CODEC
		.xmap(BridgeTextContent::new, content -> content.content));

	public static final ComponentContents.Type<BridgeTextContent> TYPE = new Type<>(
		CODEC,
		Cantilever.MODID + ":bridge"
	);

	@Override
	public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
		Optional<T> visitResult = content.visit(visitor);
		if (visitResult.isEmpty()) {
			// This is a workaround for the game resolving the Bridge content as empty. Don't ask why!
			return visitor.accept("");
		}
		return visitResult;
	}

	@Override
	public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
		return content.visit(visitor, style);
	}

	@Override
	public Type<?> getType() {
		return TYPE;
	}
}
