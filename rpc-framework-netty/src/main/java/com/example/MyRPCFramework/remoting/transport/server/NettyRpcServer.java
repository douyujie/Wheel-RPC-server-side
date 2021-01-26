package com.example.MyRPCFramework.remoting.transport.server;
import com.example.MyRPCFramework.factory.SingletonFactory;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.example.MyRPCFramework.entity.RpcServiceProperties;
import com.example.MyRPCFramework.remoting.transport.codec.RpcMessageDecoder;
import com.example.MyRPCFramework.remoting.transport.codec.RpcMessageEncoder;
import com.example.MyRPCFramework.provider.ServiceProvider;
import com.example.MyRPCFramework.provider.ServiceProviderImpl;
import com.example.MyRPCFramework.concurrent.RuntimeUtil;
import com.example.MyRPCFramework.concurrent.threadpool.ThreadPoolFactoryUtils;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NettyRpcServer {

    public static final int PORT = 9998;

    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);

    public void registerService(Object service, RpcServiceProperties rpcServiceProperties) {
        serviceProvider.publishService(service, rpcServiceProperties);
    }

    @SneakyThrows
    public void start() {
        //todo
        //CustomShutdownHook.getCustomShutdownHook().clearAll();
        String host = InetAddress.getLocalHost().getHostAddress();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtils.createThreadFactory("service-remoting.handler-group", false)
        );
        try {
            // 步骤1: 创建 ServerBootstrap 实例
            ServerBootstrap b = new ServerBootstrap();
            // 步骤2: 设置并绑定Reactor线程池
            b.group(bossGroup, workerGroup)
                    // 步骤3: 设置并绑定服务端 Channel
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // childOption()方法用于给服务端 ServerSocketChannel接收到的 SocketChannel 添加配置
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // option()方法用于给服务端的 ServerSocketChannel 添加配置
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度,
                    // 当客户端连接请求速率大于 NioServerSocketChannel 接收速率的时候，
                    // 会使用该队列做缓冲。如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // handler()方法用于给 BossGroup 设置业务处理器
                    // childHandler()方法用于给 WorkerGroup 设置业务处理器
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 步骤4: 链路建立的时候创建并初始化 ChannelPipeline
                            ChannelPipeline p = ch.pipeline();
                            // 步骤5: 初始化 ChannelPipeline 完成之后，添加并设置 ChannelHandler
                            // 30 秒之内没有收到客户端请求的话就关闭连接
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(new RpcMessageEncoder());
                            p.addLast(new RpcMessageDecoder());
                            p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });
            // 步骤6: 绑定端口，启动服务器，生成一个 channelFuture 对象，
            // ChannelFuture 涉及到 Netty 的异步模型
            ChannelFuture f = b.bind(host, PORT).sync();
            // 对通道关闭进行监听
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("occur com.example.MyRPCFramework.exception when start remoting.transport.server:", e);
        } finally {
            log.error("shutdown bossGroup and workerGroup");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }
}
