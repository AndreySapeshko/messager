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
        ClientThread client = (ClientThread) thread;
        if (client.getIsAuthorized() && !client.getIsReconnected()) {
            sendToAllAuthorizedClients(Library.getBroadcast("Server", client.getNickName() + " disconnect"));
        }
        clients.remove(thread);
        sendToAllAuthorizedClients(Library.getUserList(getUsers()));
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog(thread.getName() + " ready");
        clients.add(thread);
        ClientThread client = (ClientThread) thread;
        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(120000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!client.getIsAuthorized()) {
                    listener.onChatServerMessage("authorization time expired: " + client.getName());
                    client.sendMessage(Library.getBroadcast("Server", "authorization time expired"));
                    client.close();
                }
            }
        }.start();
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
        } else {

            ClientThread oldClient = findClientByNickname(nickName);
            clientThread.authAccept(nickName);
            if (oldClient == null) {
                sendToAllAuthorizedClients(Library.getBroadcast("Server", nickName + " connected"));
            } else {
                oldClient.reconnected();
                clients.remove(oldClient);
            }
        }


        sendToAllAuthorizedClients(Library.getUserList(getUsers()));
    }

    private void handleAuthorizedMessage(ClientThread clientThread, String msg) {
        String[] partsOfMsg = msg.split(Library.DELIMITER);
        if (partsOfMsg[0].equals(Library.TYPE_BCAST_CLIENT)) {
            sendToAllAuthorizedClients(Library.getBroadcast(clientThread.getNickName(), partsOfMsg[1]));
        } else {
            throw new RuntimeException("Msg format error: " + msg);
        }
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

    private String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.getIsAuthorized()) {
                continue;
            } else {
                sb.append(client.getNickName()).append(Library.DELIMITER);
            }
        }
        return sb.toString();
    }

    private ClientThread findClientByNickname(String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.getIsAuthorized()) {
                continue;
            }
            if (client.getNickName().equals(nickname)) {
                return client;
            }
        }
        return null;
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        putLog(exception.getMessage());
        exception.printStackTrace();
    }
}
