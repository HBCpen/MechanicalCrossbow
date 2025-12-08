package net.hbcpen.mechanicalcrossbow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public class MechanicalCrossbowClient implements ClientModInitializer {
    private boolean autoReloading = false;

    @Override
    public void onInitializeClient() {
        // Use START_CLIENT_TICK to override input before game logic runs
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            ItemStack mainHandStack = client.player.getMainHandItem();
            ItemStack offHandStack = client.player.getOffhandItem();

            ItemStack crossbowStack = null;
            InteractionHand hand = null;
            if (mainHandStack.getItem() instanceof CrossbowItem) {
                crossbowStack = mainHandStack;
                hand = InteractionHand.MAIN_HAND;
            } else if (offHandStack.getItem() instanceof CrossbowItem) {
                crossbowStack = offHandStack;
                hand = InteractionHand.OFF_HAND;
            }

            if (crossbowStack != null) {
                boolean isCharged = CrossbowItem.isCharged(crossbowStack);

                if (!isCharged) {
                    // AUTO-RELOAD
                    // Force key down regardless of user input.
                    client.options.keyUse.setDown(true);
                    autoReloading = true;
                } else {
                    // CHARGED
                    if (autoReloading) {
                        // Reload finished. Release key.
                        client.options.keyUse.setDown(false);
                        autoReloading = false;
                    } else {
                        // AUTO-FIRE
                        // If user is holding right click, Force Fire immediately.
                        if (client.options.keyUse.isDown()) {
                            // Direct interaction bypasses input polling issues
                            if (client.gameMode != null) {
                                client.gameMode.useItem(client.player, hand);
                                // Note: firing usually consumes the charge immediately on client side.
                            }
                            // We don't need to mess with setDown here if we use gameMode directly.
                            // But maybe release it to prevent double-activation?
                            // Actually, keeping it Down is fine because next tick it will be Uncharged ->
                            // Auto Reload logic takes over(setDown True).

                            // To be safe, let's allow natural input state to remain,
                            // but since we manually fired, the game might double-fire if we don't handle
                            // it?
                            // Crossbows need reload after fire, so it shouldn't matter.

                            // However, let's clear the toggle logic.
                        }
                    }
                }
            } else {
                // Not holding crossbow
                if (autoReloading) {
                    client.options.keyUse.setDown(false);
                    autoReloading = false;
                }
            }
        });
    }
}
