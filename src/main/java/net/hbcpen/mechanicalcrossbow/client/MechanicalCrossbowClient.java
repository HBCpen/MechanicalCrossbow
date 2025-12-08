package net.hbcpen.mechanicalcrossbow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public class MechanicalCrossbowClient implements ClientModInitializer {
    private boolean autoReloading = false;
    private boolean fireToggle = false;

    @Override
    public void onInitializeClient() {
        // Use START_CLIENT_TICK to override input before game logic runs
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            ItemStack mainHandStack = client.player.getMainHandItem();
            ItemStack offHandStack = client.player.getOffhandItem();

            ItemStack crossbowStack = null;
            if (mainHandStack.getItem() instanceof CrossbowItem) {
                crossbowStack = mainHandStack;
            } else if (offHandStack.getItem() instanceof CrossbowItem) {
                crossbowStack = offHandStack;
            }

            if (crossbowStack != null) {
                boolean isCharged = CrossbowItem.isCharged(crossbowStack);

                if (!isCharged) {
                    // AUTO-RELOAD
                    // Force key down regardless of user input.
                    // Since this is START_TICK, it overrides the polling done just before.
                    client.options.keyUse.setDown(true);
                    autoReloading = true;
                    fireToggle = false; // Reset fire toggle
                } else {
                    // CHARGED
                    if (autoReloading) {
                        // Just finished reloading.
                        // Force release to reset "Using" state so we can fire next.
                        client.options.keyUse.setDown(false);
                        autoReloading = false;
                        fireToggle = true; // Prepare to fire if holding
                    } else {
                        // AUTO-FIRE CHECK
                        // If user is holding the key (polled input is True), we want to fire.
                        if (client.options.keyUse.isDown()) {
                            // Oscillation Logic:
                            // We need to simulate distinct Press events.
                            // The game needs: False (Release) -> True (Press) -> Used!

                            if (fireToggle) {
                                // Tick A: Force Release.
                                client.options.keyUse.setDown(false);
                            } else {
                                // Tick B: Allow Press (User's input is True).
                                // Ensure it's true just in case.
                                client.options.keyUse.setDown(true);
                            }
                            // Flip state for next tick
                            fireToggle = !fireToggle;
                        } else {
                            // User not holding, reset toggle
                            fireToggle = false;
                        }
                    }
                }
            } else {
                // Not holding crossbow
                if (autoReloading) {
                    client.options.keyUse.setDown(false);
                    autoReloading = false;
                }
                fireToggle = false;
            }
        });
    }
}
