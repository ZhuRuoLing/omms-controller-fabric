package net.zhuruoling.omms.controller.fabric;

import com.mojang.brigadier.Message;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.zhuruoling.omms.controller.fabric.config.Config;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.server.command.CommandManager.literal;

public class OmmsControllerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Config.init();

        var handler = new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Texts.toText(() -> "YEE");
            }

            @Override
            public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                var handler = GenericContainerScreenHandler.createGeneric9x3(syncId, inv);
                return handler;
            }
        };

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            dispatcher.register(literal("menu").executes(context -> {
                var player = context.getSource().getPlayer();
                player.openHandledScreen(handler);
                return 1;
            }));
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {

        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ;
        });
    }
}
