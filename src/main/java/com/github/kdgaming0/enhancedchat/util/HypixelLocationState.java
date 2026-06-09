package com.github.kdgaming0.enhancedchat.util;

import net.azureaaron.hmapi.events.HypixelPacketEvents;
import net.azureaaron.hmapi.network.packet.v1.s2c.LocationUpdateS2CPacket;

/**
 * Tracks whether the player is currently on Hypixel and/or SkyBlock,
 * using hm-api's packet events.
 */
public final class HypixelLocationState {

    private static boolean onHypixel = false;
    private static boolean onSkyblock = false;

    private HypixelLocationState() {
    }

    public static void register() {
        HypixelPacketEvents.HELLO.register(packet -> onHypixel = true);

        HypixelPacketEvents.LOCATION_UPDATE.register(packet -> {
            if (!(packet instanceof LocationUpdateS2CPacket location)) return;

            onSkyblock = location.serverType()
                    .map("SKYBLOCK"::equals)
                    .orElse(false);
        });
    }

    public static boolean isOnHypixel() {
        return onHypixel;
    }

    public static boolean isOnSkyblock() {
        return onSkyblock;
    }

    public static void reset() {
        onHypixel = false;
        onSkyblock = false;
    }
}
