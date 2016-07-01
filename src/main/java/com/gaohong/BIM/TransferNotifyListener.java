package com.gaohong.BIM;

/**
 * @version 1.00.00
 * @description: 传输监听器，主要用于与服务的通信过程中通知进度
 * @author: archko 12-6-4
 */
public interface TransferNotifyListener {

    public void started();

    /**
     * 开始传输，
     *
     * @param totalSize 总大小
     */
    public void started(long totalSize);

    public void transferred(int length);

    /**
     * 传输通知
     *
     * @param length 传输了总大小
     */
    public void transferred(long length);

    public void completed();

    public void aborted();

    public void failed(String msg);
}
