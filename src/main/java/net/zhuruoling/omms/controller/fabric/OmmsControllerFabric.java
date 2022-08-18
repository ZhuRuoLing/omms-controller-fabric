package net.zhuruoling.omms.controller.fabric;

import com.google.gson.GsonBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.zhuruoling.omms.controller.fabric.config.ConstantStorage;
import net.zhuruoling.omms.controller.fabric.menu.MenuBlock;
import net.zhuruoling.omms.controller.fabric.menu.MenuBlockEntity;
import net.zhuruoling.omms.controller.fabric.util.UdpBroadcastReceiver;
import net.zhuruoling.omms.controller.fabric.util.UdpBroadcastSender;
import net.zhuruoling.omms.controller.fabric.util.Util;

import java.util.ArrayList;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class OmmsControllerFabric implements DedicatedServerModInitializer {
    public static final BlockEntityType<MenuBlockEntity> MENU_BLOCK_ENTITY;
    public static final Block MENU_BLOCK;
    public static final Identifier MENU = new Identifier("omms", "menu");

    static {
        MENU_BLOCK = Registry.register(Registry.BLOCK, MENU, new MenuBlock(FabricBlockSettings.copyOf(Blocks.CHEST)));
        MENU_BLOCK_ENTITY = Registry.register(Registry.BLOCK_ENTITY_TYPE, MENU, FabricBlockEntityTypeBuilder.create(MenuBlockEntity::new, MENU_BLOCK).build(null));
    }

    @Override
    public void onInitializeServer() {
        ConstantStorage.init();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> dispatcher.register(literal("menu")
                .then(argument("data", NbtCompoundArgumentType.nbtCompound()).requires(source -> source.hasPermissionLevel(0)).executes(context -> {
                    try {
                        NbtCompound compound = NbtCompoundArgumentType.getNbtCompound(context, "data");
                        if (!compound.contains("servers")) {
                            context.getSource().sendError(Text.of("Wrong server data."));
                            return -1;
                        }
                        String data = compound.getString("servers");
                        String[] servers = new GsonBuilder().serializeNulls().create().fromJson(data, String[].class);
                        ArrayList<Text> serverEntries = new ArrayList<>();
                        String currentServer = ConstantStorage.getWhitelistName();
                        for (String server : servers) {
                            boolean isCurrentServer = Objects.equals(currentServer, server);
                            ConstantStorage.ServerMapping mapping = ConstantStorage.getServerMappings().get(server);
                            if (Objects.isNull(mapping)) {
                                serverEntries.add(Util.fromServerString(server, null, false, true));
                                continue;
                            }
                            serverEntries.add(Util.fromServerString(mapping.getDisplayName(), mapping.getProxyName(), isCurrentServer, false));
                        }
                        Text serverText = Texts.join(serverEntries, Util.SPACE);
                        context.getSource().sendFeedback(Text.of("----------Welcome to %s server!----------".formatted(ConstantStorage.getControllerName())), false);
                        context.getSource().sendFeedback(Text.of("    "), false);
                        context.getSource().sendFeedback(serverText, false);
                        return 1;

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return 1;
                }))));


        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var reciever = new UdpBroadcastReceiver(server);
            var sender = new UdpBroadcastSender();
            reciever.setDaemon(true);
            sender.setDaemon(true);
            sender.start();
            reciever.start();
            ConstantStorage.setSender(sender);
            ConstantStorage.setReceiver(reciever);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            ConstantStorage.getSender().setStopped(true);
            ConstantStorage.getReceiver().interrupt();

        });

        ServerLifecycleEvents.SERVER_STOPPED.register( server -> {
            ConstantStorage.getSender().setStopped(true);
            ConstantStorage.getReceiver().interrupt();
        });
    }
}
