package net.hbcpen.mechanicalcrossbow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.util.Hand;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class MechanicalCrossbowClient implements ClientModInitializer {
    private boolean autoReloading = false;

    @Override
    public void onInitializeClient() {
        // Use START_CLIENT_TICK to override input before game logic runs
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null)
                return;

            ItemStack mainHandStack = client.player.getMainHandStack();
            ItemStack offHandStack = client.player.getOffHandStack();

            ItemStack crossbowStack = null;
            Hand hand = null;
            if (mainHandStack.getItem() instanceof CrossbowItem) {
                crossbowStack = mainHandStack;
                hand = Hand.MAIN_HAND;
            } else if (offHandStack.getItem() instanceof CrossbowItem) {
                crossbowStack = offHandStack;
                hand = Hand.OFF_HAND;
            }

            if (crossbowStack != null) {
                // Prevent operation if a GUI is open
                if (client.currentScreen != null) {
                    // If we were auto-reloading, release the key press
                    if (autoReloading) {
                        client.options.useKey.setPressed(false);
                        autoReloading = false;
                    }
                    return;
                }

                boolean isCharged = CrossbowItem.isCharged(crossbowStack);

                if (!isCharged) {
                    // AUTO-RELOAD
                    // Force key down regardless of user input.
                    client.options.useKey.setPressed(true);
                    autoReloading = true;
                } else {
                    // CHARGED
                    // Check Hardware Input to see if user wants to fire.
                    // We cannot trust keyUse.isDown() because we modify it.

                    boolean userWantToFire = false;
                    long windowHandle = client.getWindow().getHandle();
                    InputUtil.Key boundKey = KeyBindingHelper.getBoundKeyOf(client.options.useKey);

                    if (boundKey.getCategory() == InputUtil.Type.MOUSE) {
                        userWantToFire = GLFW.glfwGetMouseButton(windowHandle, boundKey.getCode()) == GLFW.GLFW_PRESS;
                    } else if (boundKey.getCategory() == InputUtil.Type.KEYSYM) {
                        userWantToFire = GLFW.glfwGetKey(windowHandle, boundKey.getCode()) == GLFW.GLFW_PRESS;
                    }

                    if (userWantToFire) {
                        // User is holding the physical button. Fire!
                        // Only fire if not on cooldown
                        if (client.interactionManager != null
                                && !client.player.getItemCooldownManager().isCoolingDown(crossbowStack)) {
                            client.interactionManager.interactItem(client.player, hand);
                        }
                    } else {
                        // User released button.
                        // We must release our hold.
                        client.options.useKey.setPressed(false);
                        autoReloading = false;
                    }
                }
            } else {
                // Not holding crossbow
                if (autoReloading) {
                    client.options.useKey.setPressed(false);
                    autoReloading = false;
                }
            }
        });
    }
}
