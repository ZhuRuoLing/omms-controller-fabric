package net.zhuruoling.omms.controller.fabric.gui;

import net.minecraft.server.network.ServerPlayerEntity;
import net.zhuruoling.omms.controller.fabric.util.MenuObject;

public class GuiUtil {
    public static void show(ServerPlayerEntity player) {
//        var builder = new SimpleGuiBuilder(ScreenHandlerType.GENERIC_9X2, false);
//        builder.setSlot(0, Items.DIAMOND.getDefaultStack().setCustomName(Text.of("Set SpawnPoint")), (index, type, action) -> {
//            var manager = Objects.requireNonNull(player.getServer()).getCommandManager();
//            manager.execute(manager.getDispatcher().parse("spawnpoint", player.getCommandSource()), "spawnpoint");
//        });
//        builder.setTitle(Text.of("WDNMD").copyContentOnly().setStyle(Style.EMPTY.withColor(Formatting.AQUA)));
//        var gui = builder.build(player);
//        gui.open();
    }

    public static void showServerMenu(ServerPlayerEntity player, MenuObject... menuObjects) {
//        int line = (menuObjects.length - (menuObjects.length % 9)) / 9;
//        var type = switch (line){
//            case 1 -> ScreenHandlerType.GENERIC_9X1;
//            case 2 -> ScreenHandlerType.GENERIC_9X2;
//            case 3 -> ScreenHandlerType.GENERIC_9X3;
//            case 4 -> ScreenHandlerType.GENERIC_9X4;
//            case 5 -> ScreenHandlerType.GENERIC_9X5;
//            case 6 -> ScreenHandlerType.GENERIC_9X6;
//            default -> throw new IllegalArgumentException("Too Much MenuObject");
//        };
//
//        var builder = new SimpleGuiBuilder(ScreenHandlerType.GENERIC_9X4, false);
//
//        builder.setSlot(0, Items.DIAMOND.getDefaultStack().setCustomName(Text.of("Set SpawnPoint")), (index, type, action) -> {
//            var manager = Objects.requireNonNull(player.getServer()).getCommandManager();
//            manager.execute(manager.getDispatcher().parse("spawnpoint", player.getCommandSource()), "spawnpoint");
//        });
//        builder.setTitle(Text.of("WDNMD").copyContentOnly().setStyle(Style.EMPTY.withColor(Formatting.AQUA)));
//        var gui = builder.build(player);
//        gui.open();
    }
}
