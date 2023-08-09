package net.zhuruoling.omms.controller.fabric.permission;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Arrays;
import java.util.List;

public class PermissionRule {
    String className;
    String classQualifier;
    PermissionType permissionType;
    List<String> playerAllowed;
    int permissionRequirement;

    public PermissionRule(String className, PermissionType permissionType, List<String> playerAllowed) {
        this.className = className;
        this.permissionType = permissionType;
        this.playerAllowed = playerAllowed;
        this.classQualifier = "L" + className.replace(".","/") + ";";
        this.permissionRequirement = 0;
    }

    public PermissionRule(String className, PermissionType permissionType, int permissionRequirement) {
        this.className = className;
        this.permissionType = permissionType;
        this.permissionRequirement = permissionRequirement;
        classQualifier = "L" + className.replace(".","/") + ";";
    }
    public static PermissionRule fromString(String namespace, String content){
        var tokens = content.split(" ");
        String name = tokens[0];
        var pm = PermissionType.valueOf(tokens[1].toUpperCase());
        var mappingResolver = FabricLoader.getInstance().getMappingResolver();
        var remappedName = mappingResolver.unmapClassName(namespace, name);
        return switch (pm){
            case PLAYER_BLACKLIST, PLAYER_WHITELIST -> new PermissionRule(remappedName, pm, Arrays.stream(tokens[2].split(",")).toList());
            case PERMISSION_REQUIREMENT -> new PermissionRule(remappedName, pm, Integer.parseInt(tokens[2], 10));
        };
    }


}
