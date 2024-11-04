package net.azisaba.lifepvelevel.network;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.azisaba.lifepvelevel.SpigotPlugin;
import net.azisaba.lifepvelevel.util.LoggedPrintStream;
import net.minecraft.network.protocol.Packet;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChannelHandler extends ChannelDuplexHandler {
    private final Player player;

    public ChannelHandler(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Packet<?>) {
            try {
                for (Object p : PacketRewriter.processIncomingPacket(new PacketData(player, msg))) {
                    super.channelRead(ctx, p);
                }
            } catch (Throwable e) {
                if (e instanceof VirtualMachineError) {
                    throw e;
                }
                SpigotPlugin.getInstance().getLogger().severe("Exception while processing packet from " + player.getName() + ", proceeding with original packet");
                e.printStackTrace(new LoggedPrintStream(SpigotPlugin.getInstance().getLogger(), System.err));
                super.channelRead(ctx, msg);
            }
            return;
        }
        super.channelRead(ctx, msg);
    }
}
