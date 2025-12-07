package net.hbcpen.mechanicalcrossbow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public class MechanicalCrossbowClient implements ClientModInitializer {
    private boolean autoReloading = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
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
                    // Not charged: Auto-Reload
                    // ALWAYS force the key down. Do not check if it's already down.
                    // This prevents gaps if the user clicks/releases the button, which would reset
                    // reload.
                    client.options.keyUse.setDown(true);
                    autoReloading = true;
                } else {
                    // Charged
                    if (autoReloading) {
                        // We just finished loading.
                        // We must release the key to "complete" the reload state and not just sit
                        // holding a loaded bow.
                        client.options.keyUse.setDown(false);
                        autoReloading = false;
                    } else {
                        // Charged and not auto-reloading (Ready to fire).

                        // Check if User wants to fire (User is holding the key).
                        // Note: We check the key state. If the user is holding it, isDown() is true.
                        if (client.options.keyUse.isDown()) {
                            // The user is holding the key.
                            // To fire a loaded crossbow, we need a fresh "Press" (Use Item).
                            // Holding the key statically usually does nothing for a charged crossbow.
                            // We simulate a Rapid Fire by forcing the key UP (False) for this tick.

                            // Mechanism:
                            // Tick N: Input=True (User). We force SetDown(False). Game sees False.
                            // Tick N+1: Input=True (User). Game sees False->True transition. FIRE!
                            // Tick N+1 End: We force SetDown(False).
                            // Repeat.
                            client.options.keyUse.setDown(false);
                        }
                    }
                }
            } else {
                // Not holding crossbow, reset state
                if (autoReloading) {
                    client.options.keyUse.setDown(false);
                    autoReloading = false;
                }
            }
        });
    }
}
