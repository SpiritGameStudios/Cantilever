package dev.spiritstudios.cantilever.bridge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.spiritstudios.cantilever.Cantilever;
import net.minecraft.text.*;

import java.util.Optional;

public record BridgeTextContent(Text content, boolean bot) implements TextContent {
	public static MapCodec<BridgeTextContent> CODEC = RecordCodecBuilder.mapCodec(
		instance -> instance.group(
			TextCodecs.CODEC.fieldOf("text").forGetter(BridgeTextContent::content),
			Codec.BOOL.fieldOf("bot").forGetter(BridgeTextContent::bot)
		).apply(instance, BridgeTextContent::new)
	);

	public static final TextContent.Type<BridgeTextContent> TYPE = new Type<>(
		CODEC,
		Cantilever.MODID + ":bridge"
	);

	@Override
	public <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		return content.visit(visitor);
	}

	@Override
	public <T> Optional<T> visit(StringVisitable.StyledVisitor<T> visitor, Style style) {
		return content.visit(visitor, style);
	}

	@Override
	public Type<?> getType() {
		return TYPE;
	}
}
