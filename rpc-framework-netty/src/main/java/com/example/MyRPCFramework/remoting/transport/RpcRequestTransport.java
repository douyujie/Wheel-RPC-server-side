package com.example.MyRPCFramework.remoting.transport;


import com.example.MyRPCFramework.extension.SPI;
import com.example.MyRPCFramework.remoting.dto.RpcRequest;

/**
 * send RpcRequest。
 */
@SPI
public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result
     *
     * @param rpcRequest message body
     * @return data from server
     */
    Object sendRpcRequest(RpcRequest rpcRequest);
}
