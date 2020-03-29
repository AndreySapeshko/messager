package ru.sapeshkoas.j2.messager.chat.server.core;

import ru.sapeshkoas.j2.messager.chat.library.Library;
import ru.sapeshkoas.j2.messager.chat.network.SocketThread;
import ru.sapeshkoas.j2.messager.chat.network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {
    private String nickName;
    private boolean isAuthorized;

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public String getNickName() {
        return nickName;
    }

    public boolean getIsAuthorized() {
        return isAuthorized;
    }

    void authAccept(String nickName) {
        isAuthorized = true;
        this.nickName = nickName;
        sendMessage(Library.getAuthAccept(nickName));
    }

    void authFail() {
        sendMessage(Library.getAuthDenied());
        close();
    }

    void msgFormatError(String msg) {
        sendMessage(Library.getMsgFormatError(msg));
        close();
    }
}
