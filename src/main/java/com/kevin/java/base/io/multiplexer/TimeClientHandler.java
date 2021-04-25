package com.kevin.java.base.io.multiplexer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;

public class TimeClientHandler implements Runnable {

    private String host;
    private int port;
    private int no;
    private Selector selector;
    private SocketChannel socketChannel;
    private volatile boolean stop = false;

    public TimeClientHandler(String host, int port, int no) {
        this.host = host;
        this.port = port;
        this.no = no;
        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            doConnection();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        System.out.println("after connect");
        while (!stop) {
            try {
                selector.select(100);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                SelectionKey key;
                while (iterator.hasNext()) {
                    key = iterator.next();
//                    iterator.remove();
                    try {
                        handInput(key);
                    } catch (Exception e) {
                        key.cancel();
                        if (key.channel() != null) {
                            key.channel().close();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }

        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            SocketChannel channel = (SocketChannel) key.channel();
            //判断是否连接成功
            if (key.isConnectable()) {
                //完成了连接
                if (channel.finishConnect()) {
                    channel.register(selector, SelectionKey.OP_READ);
                    doWrite(socketChannel);
                } else {
                    System.exit(-1);
                }
            }

            //判断是否可读状态
            if (key.isReadable()) {
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                int readbytes = channel.read(readBuffer);
                if (readbytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String msg = new String(bytes, "UTF-8");
                    System.out.println(" client get msg:" + msg);
                    this.stop = true;
                } else if (readbytes < 0) {
                    key.cancel();
                    channel.close();
                }
            }
        }
    }

    private void stop() {
        this.stop = true;
    }

    private void doConnection() throws IOException {
        //如果连接成功，则注册到多路复用器上,监听读事件
        if (socketChannel.connect(new InetSocketAddress(host, port))) {
            socketChannel.register(selector, SelectionKey.OP_READ);
            doWrite(socketChannel);
        } else {
            //注册到多路复用器上，监听连接事件
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        }
    }

    private void doWrite(SocketChannel socketChannel) throws IOException {
        byte[] bytes = ("client " + no + " query for time " + LocalDateTime.now().toString()).getBytes();
        ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
        writeBuffer.put(bytes);
        writeBuffer.flip();
        socketChannel.write(writeBuffer);
        if (!writeBuffer.hasRemaining()) {
            System.out.println("client send msg succ");
        }
    }

    public static void main(String[] args) throws IOException {
        new Thread(new TimeClientHandler("127.0.0.1", 8088, 1)).start();
    }
}
