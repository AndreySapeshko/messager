package ru.sapeshkoas.j2.messager.chat.server.core;

import ru.sapeshkoas.j2.messager.chat.library.Library;
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
    private ChatServerListener listener;

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
    }

    public void start(int port) {
        if (serverSocketThread == null || !serverSocketThread.isAlive()) {
            listener.onChatServerMessage("Server started on port 8189");
            serverSocketThread = new ServerSocketThread(this,"Server", port, 2000);
        } else {
            listener.onChatServerMessage("Server already started!");
        }

    }

    public void stop() {
        if (serverSocketThread.isAlive() && serverSocketThread != null) {
            serverSocketThread.interrupt();
            listener.onChatServerMessage("Server stopped from ChatServer.stop()");
        } else {
            listener.onChatServerMessage("Server is not running!");
        }

    }

    private void putLog(String str) {
        listener.onChatServerMessage(str);
    }

    /*
    * Server Socket Thread methods
    * */

    @Override
    public void onServerStart(ServerSocketThread thread) {
        putLog("ServerSocketThread started");
        SqlClient.connect();
        putLog(SqlClient.getNickName("andre", "123"));
    }

    @Override
    public void onServerStop(ServerSocketThread thread) {
        putLog("ServerSocketThread stopped");
        SqlClient.disconnect();
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
        putLog("Socket Accepted");
        String threadName = "SocketThread " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, threadName, socket);
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
        putLog(thread.getName() + " started");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        putLog(thread.getName() + " stopped");
        clients.remove(thread);
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog(thread.getName() + " ready");
        clients.add(thread);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.getIsAuthorized()) {
            handleAuthorizedMessage(client, msg);
        } else {
            handleNonAuthorizedMessage(client, msg);
        }
    }

    private void handleNonAuthorizedMessage(ClientThread clientThread, String msg) {
        String[] partsOfMsg = msg.split(Library.DELIMITER);
        if (partsOfMsg.length != 3) {
            return;
        }
        String login = partsOfMsg[1];
        String password = partsOfMsg[2];
        String nickName = SqlClient.getNickName(login, password);
        if (nickName == null) {
            putLog("Invalid credentials for user " + login);
            clientThread.authFail();
        }
        clientThread.authAccept(nickName);
        sendToAllAuthorizedClients(Library.getBroadcast("Server", nickName + " connected"));
    }

    private void handleAuthorizedMessage(ClientThread clientThread, String msg) {
        sendToAllAuthorizedClients(Library.getBroadcast(clientThread.getNickName(), msg));
    }

    private void sendToAllAuthorizedClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.getIsAuthorized()) {
                continue;
            }
            client.sendMessage(msg);
        }
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        putLog(exception.getMessage());
        exception.printStackTrace();
    }
}
