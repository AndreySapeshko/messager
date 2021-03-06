package ru.sapeshkoas.j2.messager.chat.client;

import ru.sapeshkoas.j2.messager.chat.library.Library;
import ru.sapeshkoas.j2.messager.chat.network.SocketThread;
import ru.sapeshkoas.j2.messager.chat.network.SocketThreadListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class ClietnGUI extends JFrame implements ActionListener, SocketThreadListener {
    private static final int HEAGHT = 300;
    private static final int WIDTH = 400;
    private static final String WINDOW_TITLE = "Chat client";
    private final JPanel panelTop = new JPanel(new GridLayout(2, 3));
    private final JTextField tfAddress = new JTextField("127.0.0.1");
    private final JTextField tfPort = new JTextField("8189");
    private final JTextField tfName = new JTextField("Ivan");
    private final JPasswordField pfPassword = new JPasswordField("1235");
    private final JButton btnLogin = new JButton("Login");
    private final JCheckBox cbAlwaysOnTop = new JCheckBox("Always on top");
    private final JTextArea taLog = new JTextArea();
    private final JList<String> usersList = new JList<>();
    private final JPanel panelBottom = new JPanel(new BorderLayout());
    private final JButton btnDisconnect = new JButton("<html><b>Disconnect</b></html>");
    private final JTextField tfMessage = new JTextField();
    private final JButton btnSend = new JButton("Send");
    private SocketThread socketThread;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClietnGUI();
            }
        });
    }

    public ClietnGUI() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle(WINDOW_TITLE);
        setResizable(false);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setSize(new Dimension(WIDTH, HEAGHT));
        JScrollPane scrollLog = new JScrollPane(taLog);
        taLog.setEditable(false);
        String[] users = new String[]{"user1", "user2", "user3", "user4", "user5", "user_with_an_exceptionally_long_name_in_this_chat"};
        usersList.setListData(users);
        JScrollPane scrollUsers = new JScrollPane(usersList);
        scrollUsers.setPreferredSize(new Dimension(100, 0));
        cbAlwaysOnTop.addActionListener(this);
        tfMessage.addActionListener(this);
        btnSend.addActionListener(this);
        btnLogin.addActionListener(this);
        btnDisconnect.addActionListener(this);

        panelTop.setVisible(true);
        panelTop.add(tfAddress);
        panelTop.add(tfPort);
        panelTop.add(cbAlwaysOnTop);
        panelTop.add(pfPassword);
        panelTop.add(tfName);
        panelTop.add(btnLogin);

        panelBottom.setVisible(false);
        panelBottom.add(btnDisconnect, BorderLayout.WEST);
        panelBottom.add(tfMessage, BorderLayout.CENTER);
        panelBottom.add(btnSend, BorderLayout.EAST);

        add(panelBottom, BorderLayout.SOUTH);
        add(panelTop, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        add(scrollUsers, BorderLayout.EAST);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cbAlwaysOnTop) {
            setAlwaysOnTop(this.cbAlwaysOnTop.isSelected());
        } else if (src == tfMessage || src == btnSend) {
            sendMessage();
        } else if (src == btnLogin) {
            connect();
        } else if (src == btnDisconnect) {
            socketThread.close();
        }
        else {
            throw new RuntimeException("Unknown sources: " + src);
        }
    }

    private void connect() {
        if (socketThread == null || !socketThread.isAlive()) {
            try {
                Socket socket = new Socket(tfAddress.getText(), Integer.parseInt(tfPort.getText()));
                socketThread = new SocketThread(this, tfName.getText(), socket);

            } catch (IOException e) {
                showException(Thread.currentThread(), e);
            }
        } else {
            System.out.println("Connect already started");
        }
    }

    private void sendMessage() {
        String msg = tfMessage.getText();
//        String userName = tfName.getText();
        if ("".equals(msg)) {
            return;
        }
//        putLog(userName, msg);
        tfMessage.setText(null);
        tfMessage.grabFocus();
        socketThread.sendMessage(Library.getTypeBcastClient(msg));
//        writeLogFile(userName, msg);
    }

    private void putLog(String msg) {
        if ("".equals(msg)) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                taLog.append(msg + "\n");
                taLog.setCaretPosition(taLog.getDocument().getLength());
            }
        });
    }

    private void writeLogFile(String userName, String msg) {
        try (FileWriter fw = new FileWriter("log.txt", true)) {
            fw.write(userName + ": " + msg + "\n");
        } catch (IOException e) {
            showException(Thread.currentThread(), e);
        }
    }

    private void showException(Thread t, Throwable exception) {
        String msg = "";
        StackTraceElement[] ste = exception.getStackTrace();
        if (ste.length == 0) {
            msg = "Stack Trace is empty";
        } else {
            msg = "Exception in " + t.getName() + " " + exception.getClass().getCanonicalName() +
                    " " + exception.getMessage() + "\n\t" + ste[0] + "\n\t" + ste[ste.length-1];
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog("SocketThread started");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        putLog("SocketThread stopped");
        panelTop.setVisible(true);
        panelBottom.setVisible(false);
        setTitle(WINDOW_TITLE);
        usersList.setListData(new String[0]);
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog("SocketThread ready");
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
        String login = tfName.getText();
        String password = new String(pfPassword.getPassword());
        thread.sendMessage(Library.getAuthRequest(login, password));
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {

        String[] partsOfMsg = msg.split(Library.DELIMITER);
        if (partsOfMsg[0].equals(Library.AUTH_ACCEPT)) {
            setTitle(WINDOW_TITLE + " entered with nickname: " + partsOfMsg[1]);
            putLog(partsOfMsg[1] + ", доступ к чату окрыт!");

        } else if (partsOfMsg[0].equals(Library.AUTH_DENIED)) {
            putLog("В доступe отказано!");

        } else if (partsOfMsg[0].equals(Library.MSG_FORMAT_ERROR)) {
            putLog(partsOfMsg[0].replaceAll("[/_]", " ") + ": " + msg);

        } else if (partsOfMsg[0].equals(Library.TYPE_BROADCAST)) {
            Date timeMsg = new Date(Long.parseLong(partsOfMsg[1]));
            SimpleDateFormat df = new SimpleDateFormat("hh:mm");
            putLog(df.format(timeMsg) + " " + partsOfMsg[2] + ": " + partsOfMsg[3]);

        } else if (partsOfMsg[0].equals(Library.USER_LIST)) {
            String users = msg.substring(Library.USER_LIST.length() + Library.DELIMITER.length());
            String[] usersArr = users.split(Library.DELIMITER);
            Arrays.sort(usersArr);
            usersList.setListData(usersArr);

        } else {
            throw new RuntimeException("Msg format error: " + msg);
        }

    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        showException(thread, exception);
    }
}
