package dev.spiritstudios.cantilever.bridge;

import com.mojang.serialization.MapCodec;
import dev.spiritstudios.cantilever.Cantilever;
import net.minecraft.text.*;

import java.util.Optional;

public record BridgeTextContent(Text content) implements TextContent {
	public static MapCodec<BridgeTextContent> CODEC = MapCodec.assumeMapUnsafe(TextCodecs.CODEC
		.xmap(BridgeTextContent::new, content -> content.content));

	public static final TextContent.Type<BridgeTextContent> TYPE = new Type<>(
		CODEC,
		Cantilever.MODID + ":bridge"
	);

	@Override
	public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		Optional<T> visitResult = content.visit(visitor);
		if (visitResult.isEmpty()) {
			// This is a workaround for the game resolving the Bridge content as empty. Don't ask why!
			return visitor.accept("");
		}
		return visitResult;
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		Optional<T> visitResult = content.visit(visitor, style);
		if (visitResult.isEmpty()) {
			return visitor.accept(style, "");
		}
		return visitResult;
	}

	@Override
	public Type<?> getType() {
		return TYPE;
	}
}
