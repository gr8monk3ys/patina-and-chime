package io.github.gr8monk3ys.patinachime.client;

import io.github.gr8monk3ys.patinachime.client.enchanting.EnchantingClient;
import net.fabricmc.api.ClientModInitializer;

public class PatinaChimeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EnchantingClient.init();
	}
}
