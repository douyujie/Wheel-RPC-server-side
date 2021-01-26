# Wheel-RPC-server-side

## Preface

Although the principle of RPC is not difficult, I encountered many problems in the process of implementation. Wheel-RPC implements only the most basic features of the RPC framework, and some of the optimizations are mentioned below for those interested.



## Introduction

Wheel-RPC is an RPC framework based on Netty+Protostuff/Kyro+Zookeeper. 

The RPC framework we mentioned here refers to a framework that allows clients to directly call server-side methods as simple as calling local methods, similar to the Dubbo, Motan, and gRPC. 

A schematic diagram of the simplest RPC framework usage is shown in the figure below, which is also the current architecture of Wheel-RPC

![rpc-architure](https://github.com/douyujie/Wheel-RPC-server-side/blob/main/pic/rpc-architure.png)

The service provider Server registers the service with the registry, and the service consumer Client gets the service-related information through the registry, and then requests the service provider Server through the network.

As a leader in the field of RPC framework [Dubbo](https://github.com/apache/dubbo), the architecture is shown in the figure below, which is roughly the same as what we drew above.

![dubbo-architure](https://github.com/douyujie/Wheel-RPC-client-side/blob/main/pic/dubbo-architure.png)

**Under normal circumstances, the RPC framework must not only provide service discovery functions, but also provide load balancing, fault tolerance and other functions. Such an RPC framework is truly qualified. **

**Please let me simply talk about the idea of designing a most basic RPC framework:**

![rpc-architure-detail](https://github.com/douyujie/Wheel-RPC-client-side/blob/main/pic/rpc-architure-detail.png)

1. **Registration Center**: The registration center is required first, and Zookeeper is recommended. The registration center is responsible for the registration and search of service addresses, which is equivalent to a directory service. When the server starts, the service name and its corresponding address (ip+port) are registered in the registry, and the service consumer finds the corresponding service address according to the service name. With the service address, the service consumer can request the server through the network.
2. **Network Transmission**: Since you want to call a remote method, you must send a request. The request must at least include the class name, method name, and related parameters you call! Recommend the Netty framework based on NIO.
3. **Serialization**: Since network transmission is involved, serialization must be involved. You can't directly use the serialization that comes with JDK! The serialization that comes with the JDK is inefficient and has security vulnerabilities. Therefore, you have to consider which serialization protocol to use. The more commonly used ones are hession2, kyro, and protostuff.
4. **Dynamic Proxy**: In addition, a dynamic proxy is also required. Because the main purpose of RPC is to allow us to call remote methods as easy as calling local methods, the use of dynamic proxy can shield the details of remote method calls such as network transmission. That is to say, when you call a remote method, the network request will actually be transmitted through the proxy object. Otherwise, how could it be possible to call the remote method directly?
5. **Load Balancing**: Load balancing is also required. Why? For example, a certain service in our system has very high traffic. We deploy this service on multiple servers. When a client initiates a request, multiple servers can handle the request. Then, how to correctly select the server that processes the request is critical. If you need one server to handle requests for the service, the meaning of deploying the service on multiple servers no longer exists. Load balancing is to avoid a single server responding to the same request, which is likely to cause server downtime, crashes and other problems. We can clearly feel its meaning from the two words of load balancing.



## Run the project

### Download and run zookeeper

Docker is used here to download and install.

download:

```
docker pull zookeeper:3.5.8
```

Run：

```
docker run -d --name zookeeper -p 2181:2181 zookeeper:3.5.8
```



## Use

### Server(service provider)

Implementing the interface：

```
@Slf4j
@RpcService(group = "test1", version = "version1")
public class HelloServiceImpl implements HelloService {
    static {
        System.out.println("HelloServiceImpl is created");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl received: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl returned: {}.", result);
        return result;
    }
}
	
@Slf4j
public class HelloServiceImpl2 implements HelloService {

    static {
        System.out.println("HelloServiceImpl2 is created");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl2 received: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl2 returned: {}.", result);
        return result;
    }
}
```

Publish services (transport using Netty) :

```
@RpcScan(basePackage = {"com.example"})
public class NettyServerMain {
    public static void main(String[] args) {
        // Register service via annotation
        new AnnotationConfigApplicationContext(NettyServerMain.class);
        NettyServer nettyServer = new NettyServer();
        // Register service manually
        HelloService helloService2 = new HelloServiceImpl2();
        RpcServiceProperties rpcServiceProperties = RpcServiceProperties.builder()
                .group("test2").version("version2").build();
        nettyServer.registerService(helloService2, rpcServiceProperties);
        nettyServer.start();
    }
}
```

### Client(service consumer)

```
@Component
public class HelloController {

    @RpcReference(version = "version1", group = "test1")
    private HelloService helloService;

    public void test() throws InterruptedException {
        String hello = this.helloService.hello(new Hello("111", "222"));
        assert "Hello description is 222".equals(hello);
        Thread.sleep(12000);
        for (int i = 0; i < 10; i++) {
            System.out.println(helloService.hello(new Hello("111", "222")));
        }
    }
}
ClientTransport rpcRequestTransport = new SocketRpcClient();
RpcServiceProperties rpcServiceProperties = RpcServiceProperties.builder()
        .group("test2").version("version2").build();
RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcRequestTransport, rpcServiceProperties);
HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
String hello = helloService.hello(new Hello("111", "222"));
System.out.println(hello);
```
# Wheel-RPC-server-side
