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
                    // Check if we are already pressing it via automation
                    if (!client.options.keyUse.isDown()) {
                        client.options.keyUse.setDown(true);
                        autoReloading = true;
                    }
                } else {
                    // Charged
                    if (autoReloading) {
                        // We finished loading, release the key to reset state
                        client.options.keyUse.setDown(false);
                        autoReloading = false;
                    } else {
                        // If we are NOT auto-reloading (user control or idle),
                        // enable Rapid Fire if user is holding the key.

                        // If user is holding the key, options.keyUse.isDown() should be true (if
                        // updated by game).
                        if (client.options.keyUse.isDown()) {
                            // The user is holding the key, but it's already charged.
                            // To fire, we need to register a "Use" click.
                            // If the game considers "Holding" as "Pressed", it might not trigger "Use" if
                            // it requires a fresh click.
                            // We toggle it off then on to simulate a click?

                            // Strategy: Release for 1 tick, then Press?
                            // Or just Press?
                            // Standard Crossbow fires on release? No, fires on Use.
                            // Let's try to release it now, so next tick the user's physical hold
                            // re-triggers a Press?
                            // Or we force a Press ourselves.

                            // Let's try: Force Release simply.
                            // If we force release this tick, next tick the input system will read "Held"
                            // and set "Pressed".
                            // This might create a rapid pulse: Press(Tick1) -> Fire -> Charged=False ->
                            // Reload loop.

                            client.options.keyUse.setDown(false);

                            // Wait, if we release it, it won't fire?
                            // Firing happens when you USE the item.
                            // If you hold it, does it use it?
                            // If I hold right click with a charged crossbow, it fires immediately?
                            // No. You have to click.
                            // So if keys is Pressed, and we do NOTHING, nothing happens.
                            // We need to simulate a "Release -> Press" or "Press" event.

                            // If we setPressed(false) this tick...
                            // Then next tick, if user holds, it becomes Pressed.
                            // This 0 -> 1 transition might trigger the use.
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
