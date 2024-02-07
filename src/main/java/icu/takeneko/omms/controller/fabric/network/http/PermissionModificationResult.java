package icu.takeneko.omms.controller.fabric.network.http;

public class PermissionModificationResult {
    boolean status;
    String message;

    Object object;

    public PermissionModificationResult(boolean status, String message, Object object) {
        this.status = status;
        this.message = message;
        this.object = object;
    }
}
