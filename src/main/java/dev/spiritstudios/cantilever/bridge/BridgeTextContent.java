package dev.spiritstudios.cantilever.bridge;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public record BridgeTextContent(Component content) implements ComponentContents {
	public static MapCodec<BridgeTextContent> CODEC = MapCodec.assumeMapUnsafe(ComponentSerialization.CODEC
		.xmap(BridgeTextContent::new, content -> content.content));

	/* public static final ComponentContents.Type<BridgeTextContent> TYPE = new Type<>(
		CODEC,
		Cantilever.MODID + ":bridge"
	); */

	@Override
	public <T> @NonNull Optional<T> visit(FormattedText.@NonNull ContentConsumer<T> visitor) {
		Optional<T> visitResult = content.visit(visitor);
		if (visitResult.isEmpty()) {
			// This is a workaround for the game resolving the Bridge content as empty. Don't ask why!
			return visitor.accept("");
		}
		return visitResult;
	}

	@Override
	public @NonNull MapCodec<? extends ComponentContents> codec() {
		return CODEC;
	}

	@Override
	public <T> @NonNull Optional<T> visit(FormattedText.@NonNull StyledContentConsumer<T> visitor, @NonNull Style style) {
		return content.visit(visitor, style);
	}
}
