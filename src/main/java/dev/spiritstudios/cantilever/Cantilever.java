package dev.spiritstudios.cantilever;

import dev.spiritstudios.cantilever.bridge.Bridge;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.message.MessageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cantilever implements ModInitializer {
	public static final String MODID = "cantilever";
	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
	public static final RegistryKey<MessageType> D2M_MESSAGE_TYPE = RegistryKey.of(
		RegistryKeys.MESSAGE_TYPE,
		id("d2m")
	);
	private static Bridge bridge;

	@Override
	public void onInitialize() {
		CantileverConfig.INSTANCE.id(); // touch for QOMC
		ServerLifecycleEvents.SERVER_STARTING.register(
			id("before_bridge"),
			server -> {
				LOGGER.info("Initialising Cantilever...");
				bridge = new Bridge(server);
			}
		);

		ServerLifecycleEvents.SERVER_STARTING.addPhaseOrdering(
			id("before_bridge"),
			id("after_bridge")
		);

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
			JDA api = bridge().api();
			if (api == null)
				return;
			api.getPresence().setActivity(CantileverConfig.INSTANCE.statusMessage.value().isEmpty() ?
				null :
				Activity.of(CantileverConfig.INSTANCE.activityType.value(), CantileverConfig.INSTANCE.statusMessage.value()));
		});
	}

	public static Bridge bridge() {
		return bridge;
	}

	public static Identifier id(String path) {
		return Identifier.of(MODID, path);
	}
}
