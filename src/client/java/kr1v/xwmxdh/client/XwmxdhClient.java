package kr1v.xwmxdh.client;

import net.fabricmc.api.ClientModInitializer;

public class XwmxdhClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }

    public static void printTimeSince(long start) {
        System.out.println("Took " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }
}
