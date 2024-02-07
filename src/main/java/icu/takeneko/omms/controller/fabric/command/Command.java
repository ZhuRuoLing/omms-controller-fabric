package icu.takeneko.omms.controller.fabric.command;

import com.mojang.brigadier.CommandDispatcher;

public interface Command<T> {
    void register(CommandDispatcher<T> commandDispatcher);
}
