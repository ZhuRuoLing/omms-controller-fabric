package net.zhuruoling.omms.controller.fabric.command;

import com.mojang.brigadier.CommandDispatcher;

abstract public class Command<T> {
    abstract public void register(CommandDispatcher<T> commandDispatcher);

}
