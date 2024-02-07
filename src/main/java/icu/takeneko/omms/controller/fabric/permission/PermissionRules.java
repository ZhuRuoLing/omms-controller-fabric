package icu.takeneko.omms.controller.fabric.permission;

import icu.takeneko.omms.controller.fabric.util.Util;

import java.util.List;

public class PermissionRules {
    List<PermissionRule> rules;
    boolean status;
    String sessionId;
    public PermissionRules(List<PermissionRule> rules, boolean status) {
        this.rules = rules;
        this.status = status;
        sessionId = Util.randomStringGen(8);
    }

    public List<PermissionRule> getRules() {
        return rules;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

}
