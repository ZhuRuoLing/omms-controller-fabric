package net.zhuruoling.omms.controller.fabric.permission;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Objects;

public class MappedNames {
    public static String nameOfClassServerCommandSource = "net.minecraft.server.command.ServerCommandSource";
    public static String nameOfMethodHasPermissionLevel = "hasPermissionLevel";

    public static void mapNames(){
        var mr = FabricLoader.getInstance().getMappingResolver();
        nameOfClassServerCommandSource = mr.unmapClassName(mr.getCurrentRuntimeNamespace(), nameOfClassServerCommandSource);
        nameOfMethodHasPermissionLevel = Objects.equals(mr.getCurrentRuntimeNamespace(), "named") ? nameOfMethodHasPermissionLevel : "method_9259";
    }

}
