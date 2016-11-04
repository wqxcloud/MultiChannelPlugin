package com.tubb.multichannel

public class AppChannelExtension{
    /**
     * channel config file path
     */
    String channelFilePath
    /**
     * custom productFlavor
     */
    Closure buildProductFlavor
    /**
     * apk output dir
     */
    String outputDir
    /**
     * config apk file name
     */
    Closure buildOutputFileName
}