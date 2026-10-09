package io.netty.channel.nio;

import io.netty.channel.local.LocalEventLoopGroup;
import java.util.concurrent.ThreadFactory;

/**
 * Replacement for Netty's NIO event loop group. NIO needs selectors and sockets, which a browser does not have;
 * Minecraft's integrated server talks to its client over Netty's in-memory local channels, which only need event
 * loops. This group provides those under the NIO group's name (Minecraft creates one in NetworkSystem's static
 * initializer).
 */
public class NioEventLoopGroup extends LocalEventLoopGroup {
    public NioEventLoopGroup() {
        super(0);
    }

    public NioEventLoopGroup(int nThreads) {
        super(nThreads);
    }

    public NioEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        super(nThreads, threadFactory);
    }

    public void setIoRatio(int ioRatio) {
    }

    public void rebuildSelectors() {
    }
}
