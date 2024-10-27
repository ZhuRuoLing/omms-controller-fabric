package icu.takeneko.omms.controller.fabric.network.http;

import icu.takeneko.omms.controller.fabric.permission.PermissionRule;

public class PermissionModificationData {
    private final Type type;
    private final String className;
    private final int removeAt;
    private final PermissionRule rule;

    public PermissionModificationData(Type type, String className, int removeAt, PermissionRule rule) {
        this.type = type;
        this.className = className;
        this.removeAt = removeAt;
        this.rule = rule;
    }

    public enum Type {
        ENABLE, REMOVE, ADD_RULE, REMOVE_RULE
    }

    public Type getType() {
        return type;
    }

    public String getClassName() {
        return className;
    }

    public int getRemoveAt() {
        return removeAt;
    }

    public PermissionRule getRule() {
        return rule;
    }
}
