package net.zhuruoling.omms.controller.fabric.network.http;

import net.zhuruoling.omms.controller.fabric.permission.PermissionRule;

public class PermissionModificationData {
    enum Type {
        ENABLE, REMOVE, ADD_RULE, REMOVE_RULE
    }

    Type type;
    String className;
    int removeAt;
    PermissionRule rule;
}
