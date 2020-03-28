package ru.sapeshkoas.j2.messager.chat.server.core;

import ru.sapeshkoas.j2.messager.chat.network.ServerSocketThread;
import ru.sapeshkoas.j2.messager.chat.network.ServerSocketThreadListener;
import ru.sapeshkoas.j2.messager.chat.network.SocketThread;
import ru.sapeshkoas.j2.messager.chat.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener {
    private int port;
    private ServerSocketThread serverSocketThread;
    private Vector<SocketThread> clients = new Vector<>();

    public void start(int port) {
        if (serverSocketThread == null || !serverSocketThread.isAlive()) {
            System.out.println("Server started on port 8189");
            serverSocketThread = new ServerSocketThread(this,"Server", port, 2000);
        } else {
            System.out.println("Server already started!");
        }

    }

    public void stop() {
        if (serverSocketThread.isAlive() && serverSocketThread != null) {
            serverSocketThread.interrupt();
            System.out.println("Server stopped");
        } else {
            System.out.println("Server is not running!");
        }

    }

    private void putLog(String str) {
        System.out.println(str);
    }

    /*
    * Server Socket Thread methods
    * */

    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("ServerSocketThread started");
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("ServerSocketThread stopped");
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket server) {
        putLog("ServerSocket created");
    }

    @Override
    public void onServerTimeout(ServerSocketThread thread, ServerSocket server) {

    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, ServerSocket server, Socket socket) {
        System.out.println("Socket Accepted");
        new SocketThread(this, "SocketThread " + socket.getInetAddress() + ":" + socket.getPort(), socket);
    }

    @Override
    public void onServerException(ServerSocketThread thread, Throwable exception) {
        exception.printStackTrace();
    }

    /*
    * Socket Thread methods
    * */

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("SocketThread " + thread.getName() + " started");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        putLog(thread.getName() + " stopped");
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog("SocketThread " + thread.getName() + " ready");
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        thread.sendMessage(msg);
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        exception.printStackTrace();
    }
}
