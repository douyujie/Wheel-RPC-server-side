package com.example;

import com.example.MyRPCFramework.annotation.RpcScan;
import com.example.MyRPCFramework.entity.RpcServiceProperties;
import com.example.MyRPCFramework.remoting.transport.server.NettyRpcServer;
import com.example.serviceImpl.HelloServiceImpl2;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;


/**
 * Server: Automatic registration service via @RpcService annotation
 */
@RpcScan(basePackage = {"com.example"})
public class NettyServerMain {
    public static void main(String[] args) {
        // Register service via annotation
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyRpcServer nettyRpcServer = (NettyRpcServer) applicationContext.getBean("nettyRpcServer");
        // Register service manually
        HelloService helloService2 = new HelloServiceImpl2();
        RpcServiceProperties rpcServiceProperties = RpcServiceProperties.builder()
                .group("test2").version("version2").build();
        nettyRpcServer.registerService(helloService2, rpcServiceProperties);
        nettyRpcServer.start();
    }
}
