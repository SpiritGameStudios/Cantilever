package dev.spiritstudios.cantilever;

import dev.spiritstudios.cantilever.bridge.Bridge;
import dev.spiritstudios.cantilever.bridge.BridgeTextContent;
import dev.spiritstudios.specter.api.serialization.text.TextContentRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.text.TextContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cantilever implements ModInitializer {
    public static final String MODID = "cantilever";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static Bridge BRIDGE;

    @Override
    public void onInitialize() {
		LOGGER.info("Initialising Cantilever");

		TextContentRegistry.register("bot", BridgeTextContent.TYPE);

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			BRIDGE = new Bridge("token", 1234L, server);
		});
    }
}
