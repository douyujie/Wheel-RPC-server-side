package com.example.MyRPCFramework.compress;

import com.example.MyRPCFramework.extension.SPI;

@SPI
public interface Compress {

    byte[] compress(byte[] bytes);


    byte[] decompress(byte[] bytes);
}
