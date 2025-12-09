package kr1v.xwmxdh.client;

import net.fabricmc.api.ClientModInitializer;

public class XwmxdhClient implements ClientModInitializer {
    public static Thread t;
    public static final Object lock = new Object();

    public static void doStuff() {
    }

    @Override
    public void onInitializeClient() {
    }

    public static void printTimeSince(long start) {
        System.out.println("Took " + (System.nanoTime() - start) / 1_000_000 + " ms");
    }
}
