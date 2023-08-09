package net.zhuruoling.omms.controller.fabric.permission;

import com.mojang.logging.LogUtils;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionRuleManager {
    @SuppressWarnings("all")
    public static PermissionRuleManager INSTANCE = new PermissionRuleManager();
    private final List<PermissionRuleFile> permissionRuleFiles = new ArrayList<>();
    private final Map<String, List<PermissionRule>> permissionRules = new ConcurrentHashMap<>();
    private final Logger logger = LogUtils.getLogger();

    public synchronized void loadFromRulesFile(File file) {
        permissionRuleFiles.add(PermissionRuleFile.readFromFile(file));
    }

    public boolean containsClass(String className){
        return permissionRules.containsKey(className);
    }

    public void init() {
        for (PermissionRuleFile ruleFile : permissionRuleFiles) {
            logger.info("Loading rule file %s".formatted(ruleFile.name));
            for (PermissionRule rule : ruleFile.permissionRules) {
                if (!permissionRules.containsKey(rule.className)) {
                    permissionRules.put(rule.className, new ArrayList<>());
                }
                permissionRules.get(rule.className).add(rule);
            }
        }
        permissionRules.forEach((s, a) -> {
            logger.info("Applying permission check patch to class %s".formatted(s));
            PatchUtil.patchClass(s);
        });
    }

    private boolean checkPermission0(String className, ServerCommandSource commandSource) {
        if (commandSource.getEntity() == null)return true;
        if (permissionRules.containsKey(className)) {
            return permissionRules.get(className).stream().allMatch(it -> switch (it.permissionType) {
                case PERMISSION_REQUIREMENT -> commandSource.hasPermissionLevel(it.permissionRequirement);
                case PLAYER_BLACKLIST -> commandSource.isExecutedByPlayer() &&
                        !it.playerAllowed.contains(commandSource.getPlayer().getGameProfile().getName());
                case PLAYER_WHITELIST -> commandSource.isExecutedByPlayer() &&
                        it.playerAllowed.contains(commandSource.getPlayer().getGameProfile().getName());
            });
        }
        return true;
    }

    public static boolean checkPermission(String className, ServerCommandSource src) {
        return INSTANCE.checkPermission0(className, src);
    }
}
