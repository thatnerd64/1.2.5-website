package io.netty.channel.socket.nio;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import retro.net.WebSocketConnection;

/**
 * Replacement for Netty's NIO socket channel: a TCP connection carried over a WebSocket to a relay
 * (tools/ws-proxy.js), as java.net.Socket is in the 1.2.5 build. Minecraft connects to servers (and pings them for
 * the server list) through {@code new Bootstrap().channel(NioSocketChannel.class).connect(address, port)}; the
 * address is either {@code host} (reached through the launcher's relay as {@code <relay>?host=..&port=..}) or a
 * {@code ws://} / {@code wss://} URL typed as the server address (see retro.net.ServerAddress).
 *
 * <p>A reader thread blocks on the WebSocket and hands each chunk to the event loop; writes go straight out (a
 * WebSocket send does not block).
 */
public class NioSocketChannel extends AbstractChannel {
    private static final ChannelMetadata METADATA = new ChannelMetadata(false);
    private static final int OPEN = 0;
    private static final int ACTIVE = 1;
    private static final int CLOSED = 2;

    private final ChannelConfig config = new DefaultChannelConfig(this);
    private volatile int state = OPEN;
    private volatile WebSocketConnection connection;
    private volatile OutputStream out;
    private volatile InetSocketAddress remote;

    public NioSocketChannel() {
        super(null);
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new WebSocketUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    protected SocketAddress localAddress0() {
        return new InetSocketAddress("localhost", 0);
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return remote;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException("Cannot bind a socket in a browser");
    }

    @Override
    protected void doDisconnect() throws Exception {
        doClose();
    }

    @Override
    protected void doClose() throws Exception {
        state = CLOSED;
        WebSocketConnection c = connection;
        if (c != null) {
            c.close();
        }
    }

    @Override
    protected void doBeginRead() throws Exception {
        // the reader thread delivers data as it arrives
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        for (;;) {
            Object msg = in.current();
            if (msg == null) {
                break;
            }
            if (msg instanceof ByteBuf) {
                ByteBuf buf = (ByteBuf) msg;
                int n = buf.readableBytes();
                if (n > 0) {
                    byte[] bytes = new byte[n];
                    buf.getBytes(buf.readerIndex(), bytes);
                    OutputStream o = out;
                    if (o == null) {
                        throw new IOException("Not connected");
                    }
                    o.write(bytes, 0, n);
                    in.progress(n);
                }
                in.remove();
            } else {
                in.remove(new UnsupportedOperationException("Unsupported message type: " + msg.getClass().getName()));
            }
        }
    }

    @Override
    public ChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return state != CLOSED;
    }

    @Override
    public boolean isActive() {
        return state == ACTIVE;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    private void startReader(final WebSocketConnection c) {
        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream in = c.inputStream();
                byte[] buf = new byte[16384];
                try {
                    for (;;) {
                        int n = in.read(buf, 0, buf.length);
                        if (n < 0) {
                            break;
                        }
                        final byte[] chunk = Arrays.copyOf(buf, n);
                        eventLoop().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (isActive()) {
                                    pipeline().fireChannelRead(Unpooled.wrappedBuffer(chunk));
                                    pipeline().fireChannelReadComplete();
                                }
                            }
                        });
                    }
                } catch (final IOException e) {
                    if (state != CLOSED) {
                        eventLoop().execute(new Runnable() {
                            @Override
                            public void run() {
                                pipeline().fireExceptionCaught(e);
                            }
                        });
                    }
                }
                eventLoop().execute(new Runnable() {
                    @Override
                    public void run() {
                        unsafe().close(unsafe().voidPromise());
                    }
                });
            }
        }, "WebSocket reader");
        reader.setDaemon(true);
        reader.start();
    }

    private final class WebSocketUnsafe extends AbstractUnsafe {
        @Override
        public void connect(final SocketAddress remoteAddress, SocketAddress localAddress, final ChannelPromise promise) {
            if (!ensureOpen(promise)) {
                return;
            }
            if (state == ACTIVE || connection != null) {
                promise.setFailure(new IllegalStateException("Already connected"));
                return;
            }
            final InetSocketAddress target = (InetSocketAddress) remoteAddress;
            final int timeout = config.getConnectTimeoutMillis();
            Thread connector = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final WebSocketConnection c = WebSocketConnection.open(target.getHostString(), target.getPort(),
                                timeout);
                        eventLoop().execute(new Runnable() {
                            @Override
                            public void run() {
                                if (state == CLOSED || !promise.setUncancellable()) {
                                    c.close();
                                    return;
                                }
                                connection = c;
                                out = c.outputStream();
                                remote = target;
                                state = ACTIVE;
                                promise.trySuccess();
                                pipeline().fireChannelActive();
                                startReader(c);
                            }
                        });
                    } catch (final IOException e) {
                        eventLoop().execute(new Runnable() {
                            @Override
                            public void run() {
                                promise.tryFailure(e);
                                closeIfClosed();
                                if (state != CLOSED) {
                                    close(voidPromise());
                                }
                            }
                        });
                    }
                }
            }, "WebSocket connect");
            connector.setDaemon(true);
            connector.start();
        }
    }
}
