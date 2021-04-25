package com.kevin.java.base.io.multiplexer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {


    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean stop = false;

    /**
     * @Desc 初始化多路复用器，绑定端口
     * @Author HeRong
     * @Date 2020/5/3
     */
    public MultiplexerTimeServer(int port) {
        try {
            //创建Reactor线程多路复用器
            selector = Selector.open();
            //创建socket 通道
            serverChannel = ServerSocketChannel.open();
            //设置通道为非阻塞
            serverChannel.configureBlocking(false);
            //绑定ip端口，设置最大的请求连接数为1024
            serverChannel.socket().bind(new InetSocketAddress(port), 1024);
            //注册到多路复用器上，监听accpet事件
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("time server is start on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void stop() {
        this.stop = true;
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                //设置获取就绪key的休眠时间,每间隔100ms唤醒一次
                selector.select(100);
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                SelectionKey key;
                while (iterator.hasNext()) {
                    key = iterator.next();
                    iterator.remove();
                    try {
                        handlerKey(key);
                    } catch (Exception e) {
                        key.cancel();
                        if (key.channel() != null) {
                            key.channel().close();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
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

    private void handlerKey(SelectionKey key) throws IOException {
        if (key.isValid()) {
            //查看是否是accpet事件
            if (key.isAcceptable()) {
                ServerSocketChannel channel = (ServerSocketChannel) key.channel();
                //接受客户端请求，三次握手结束，建立物理连接
                SocketChannel accept = channel.accept();
                //设置非阻塞模式
                accept.configureBlocking(false);
                //注册监听读取事件
                accept.register(selector, SelectionKey.OP_READ);
            }

            //查看是read事件
            if (key.isReadable()) {
                //开辟缓存空间
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                SocketChannel socketChannel = (SocketChannel) key.channel();
                //读取数据
                int readBytes = socketChannel.read(readBuffer);
                //大于0，读取到了数据
                if (readBytes > 0) {
                    readBuffer.flip();
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes);
                    String msg = new String(bytes, "UTF-8");
                    System.out.println("receive msg:" + msg);
                    doWrite(socketChannel, System.currentTimeMillis() + "");
                } else if (readBytes < 0) {
                    //等于-1 ，链路已经关闭，需要释放资源
                    key.cancel();
                    socketChannel.close();
                } else {
                    //等于0，没有可读取数据，忽略
                }

            }
        }

    }

    private void doWrite(SocketChannel socketChannel, String response) throws IOException {

        byte[] bytes = response.getBytes();
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        socketChannel.write(byteBuffer);

    }

    public static void main(String[] args) throws IOException {
        MultiplexerTimeServer server = new MultiplexerTimeServer(8088);
        new Thread(server, "timerServer-001").start();
    }
}
