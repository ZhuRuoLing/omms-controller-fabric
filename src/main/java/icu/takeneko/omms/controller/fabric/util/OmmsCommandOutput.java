package icu.takeneko.omms.controller.fabric.util;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class OmmsCommandOutput implements CommandSource {
    private static final String NAME = "OMMS";
    private static final Component NAME_TEXT = Component.literal("OMMS");
    private final StringBuffer buffer = new StringBuffer();
    private final MinecraftServer server;

    public OmmsCommandOutput(MinecraftServer server) {
        this.server = server;
    }

    public void clear() {
        this.buffer.setLength(0);
    }

    public String asString() {
        return this.buffer.toString();
    }

    public CommandSourceStack createOmmsCommandSource() {
        ServerLevel serverWorld = this.server.getLevel(Level.OVERWORLD);
        return new CommandSourceStack(this,
            Vec3.ZERO,
            Vec2.ZERO,
            serverWorld,
            4,
            NAME,
            NAME_TEXT,
            this.server,
            null
        );
    }

    @Override
    public void sendSystemMessage(Component component) {
        this.buffer.append(component.getString()).append("\n");
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return this.server.shouldRconBroadcast();
    }
}
