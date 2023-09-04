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
    private final Map<String, PermissionRules> permissionRuleMap = new ConcurrentHashMap<>();
    private final Logger logger = LogUtils.getLogger();

    public synchronized void loadFromRulesFile(File file) {
        permissionRuleFiles.add(PermissionRuleFile.readFromFile(file));
    }

    public boolean containsClass(String className) {
        return permissionRuleMap.containsKey(className);
    }

    public boolean isEnablePermissionCheckFor(String clazzName) {
        return permissionRuleMap.containsKey(clazzName) && permissionRuleMap.get(clazzName).getStatus();
    }

    public void init() {
        for (PermissionRuleFile ruleFile : permissionRuleFiles) {
            logger.info("Loading rule file %s".formatted(ruleFile.name));
            for (PermissionRule rule : ruleFile.permissionRules) {
                if (!permissionRuleMap.containsKey(rule.className)) {
                    permissionRuleMap.put(rule.className, new PermissionRules(new ArrayList<>(), true));
                }
                permissionRuleMap.get(rule.className).rules.add(rule);
            }
        }
        permissionRuleMap.forEach((s, a) -> {
            logger.info("Applying permission check patch to class %s".formatted(s));
            var result = PatchUtil.patchClass(s);
            permissionRuleMap.computeIfPresent(s, ((s1, permissionRules) -> {
                permissionRules.setStatus(result);
                return permissionRules;
            }));
        });
    }

    public boolean checkPermission(String className, ServerCommandSource commandSource) {
        if (commandSource.getEntity() == null) return true;
        if (permissionRuleMap.containsKey(className)) {
            return permissionRuleMap.get(className).rules.stream().allMatch(it -> switch (it.permissionType) {
                case PERMISSION_REQUIREMENT -> commandSource.hasPermissionLevel(it.permissionRequirement);
                case PLAYER_BLACKLIST -> commandSource.isExecutedByPlayer() &&
                        !it.playerAllowed.contains(commandSource.getPlayer().getGameProfile().getName());
                case PLAYER_WHITELIST -> commandSource.isExecutedByPlayer() &&
                        it.playerAllowed.contains(commandSource.getPlayer().getGameProfile().getName());
            });
        }
        return true;
    }
}
