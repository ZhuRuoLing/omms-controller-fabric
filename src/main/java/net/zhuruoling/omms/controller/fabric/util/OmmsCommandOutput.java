package net.zhuruoling.omms.controller.fabric.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public class OmmsCommandOutput implements CommandOutput {
    private static final String RCON_NAME = "OMMS";
    private static final Text RCON_NAME_TEXT = Text.literal("OMMS");
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

    public ServerCommandSource createOmmsCommandSource() {
        ServerWorld serverWorld = this.server.getOverworld();
        return new ServerCommandSource(this, Vec3d.of(serverWorld.getSpawnPos()), Vec2f.ZERO, serverWorld, 4, RCON_NAME, RCON_NAME_TEXT, this.server, (Entity)null);
    }

    public void sendMessage(Text message) {
        this.buffer.append(message.getString()).append("\n");
    }

    public boolean shouldReceiveFeedback() {
        return true;
    }

    public boolean shouldTrackOutput() {
        return true;
    }

    public boolean shouldBroadcastConsoleToOps() {
        return this.server.shouldBroadcastRconToOps();
    }
}
