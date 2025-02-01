package dev.spiritstudios.cantilever;

import dev.spiritstudios.cantilever.bridge.Bridge;
import dev.spiritstudios.cantilever.bridge.BridgeTextContent;
import dev.spiritstudios.specter.api.serialization.text.TextContentRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cantilever implements ModInitializer {
	public static final String MODID = "cantilever";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	private static Bridge bridge;

	@Override
	public void onInitialize() {
		TextContentRegistry.register("bot", BridgeTextContent.TYPE);

		ServerLifecycleEvents.SERVER_STARTING.register(
			Identifier.of(MODID, "before_bridge"),
			server -> {
				LOGGER.info("Initialising Cantilever...");
				bridge = new Bridge(server);
			}
		);

		ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(
			Identifier.of(MODID, "before_bridge"),
			Identifier.of(MODID, "after_bridge")
		);


	}

	public static Bridge bridge() {
		return bridge;
	}

	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}
