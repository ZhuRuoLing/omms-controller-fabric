package net.zhuruoling.omms.controller.fabric.permission;

import com.mojang.logging.LogUtils;
import net.minecraft.server.command.ServerCommandSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PermissionRuleManager {
    @SuppressWarnings("all")
    public static PermissionRuleManager INSTANCE = new PermissionRuleManager();
    private PermissionRuleFile permissionRuleFile;
    private final Map<String, PermissionRules> permissionRuleMap = new ConcurrentHashMap<>();
    private final Logger logger = LogUtils.getLogger();

    public synchronized void loadFromRulesFile(File file) {
        permissionRuleFile = PermissionRuleFile.readFromFile(file);
    }

    public boolean containsClass(String className) {
        return permissionRuleMap.containsKey(className);
    }

    public boolean isEnablePermissionCheckFor(String clazzName) {
        return permissionRuleMap.containsKey(clazzName) && permissionRuleMap.get(clazzName).getStatus() && !permissionRuleMap.get(clazzName).rules.isEmpty();
    }

    public void init() {
        logger.info("Loading rule file %s".formatted(permissionRuleFile.name));
        for (PermissionRule rule : permissionRuleFile.permissionRules) {
            if (!permissionRuleMap.containsKey(rule.className)) {
                permissionRuleMap.put(rule.className, new PermissionRules(new ArrayList<>(), true));
            }
            permissionRuleMap.get(rule.className).rules.add(rule);
        }
        permissionRuleMap.forEach((s, a) -> applyPermissionRule(s));
    }

    private void applyPermissionRule(String className) {
        logger.info("Applying permission check patch to class %s".formatted(className));
        var result = PatchUtil.patchClass(className);
        permissionRuleMap.computeIfPresent(className, ((s1, permissionRules) -> {
            permissionRules.setStatus(result);
            return permissionRules;
        }));
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

    public Map<String, PermissionRules> getPermissionRuleMap() {
        return permissionRuleMap;
    }

    public void createNewRule(@NotNull String clazz) {
        permissionRuleMap.put(clazz, new PermissionRules(new ArrayList<>(), true));
        applyPermissionRule(clazz);
    }

    public void enableCheckFor(String clazz){
        if (permissionRuleMap.containsKey(clazz)){
            permissionRuleMap.get(clazz).setStatus(true);
        }else {
            createNewRule(clazz);
        }
    }

    public void addRule(String clazz, PermissionRule rule){
        synchronized (permissionRuleMap){
            permissionRuleMap.get(clazz).rules.add(rule);
        }
    }

    public void removeRule(String clazz, int at){
        synchronized (permissionRuleMap){
            permissionRuleMap.get(clazz).rules.remove(at);
        }
    }

    public void save() {
        this.permissionRuleFile.save();
    }

    public void disableCheckFor(String clazz) {
        if (permissionRuleMap.containsKey(clazz)){
            permissionRuleMap.get(clazz).setStatus(false);
        }else {
            createNewRule(clazz);
        }
    }
}
