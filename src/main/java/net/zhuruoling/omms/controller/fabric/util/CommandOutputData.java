package net.zhuruoling.omms.controller.fabric.util;

public class CommandOutputData {
    String controllerId;
    String command;
    String output;

    public CommandOutputData(String controllerId, String command, String output) {
        this.controllerId = controllerId;
        this.command = command;
        this.output = output;
    }

    public String getControllerId() {
        return controllerId;
    }

    public String getCommand() {
        return command;
    }

    public String getOutput() {
        return output;
    }
}
