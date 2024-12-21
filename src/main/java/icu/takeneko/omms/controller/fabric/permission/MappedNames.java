package icu.takeneko.omms.controller.fabric.permission;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import java.util.Objects;

public class MappedNames {
    public static String nameOfClassCommandSourceStack = "net.minecraft.server.command.CommandSourceStack";
    public static String nameOfMethodHasPermissionLevel = "hasPermission";

    public static void mapNames(){
        var mr = FabricLoader.getInstance().getMappingResolver();
        nameOfClassCommandSourceStack = mr.unmapClassName(mr.getCurrentRuntimeNamespace(), nameOfClassCommandSourceStack);
        nameOfMethodHasPermissionLevel = Objects.equals(mr.getCurrentRuntimeNamespace(), "named") ? nameOfMethodHasPermissionLevel : "method_9259";
    }

}
