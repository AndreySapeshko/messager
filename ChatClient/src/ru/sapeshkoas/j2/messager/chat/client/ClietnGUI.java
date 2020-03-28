package ru.sapeshkoas.j2.messager.chat.client;

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
import java.util.ArrayList;

public class ClietnGUI extends JFrame implements ActionListener, SocketThreadListener {
    private static final int HEAGHT = 300;
    private static final int WIDTH = 400;
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
        setTitle("Chat client");
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
                socketThread = new SocketThread(this, socket.getInetAddress() + ":" + socket.getPort(), socket);

            } catch (IOException e) {
                showException(Thread.currentThread(), e);
            }
        } else {
            System.out.println("Connect already started");
        }
    }

    private void sendMessage() {
        String msg = tfMessage.getText();
        String userName = tfName.getText();
        if ("".equals(msg)) {
            return;
        }
//        putLog(userName, msg);
        tfMessage.setText(null);
        tfMessage.grabFocus();
        socketThread.sendMessage(userName + ": " + msg);
        writeLogFile(userName, msg);
    }

    private void putLog(String userName, String msg) {
        if ("".equals(msg)) {
            return;
        }
        taLog.append(userName + ": " + msg + "\n");
        taLog.setCaretPosition(taLog.getDocument().getLength());
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
                    " " + exception.getMessage() + "\n\t" + ste[0];
        }
        JOptionPane.showMessageDialog(null, msg, "Exception", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void onSocketStart(SocketThread thread, Socket socket) {
        putLog(tfName.getText(), "SocketThread started");
    }

    @Override
    public void onSocketStop(SocketThread thread) {
        putLog(tfName.getText(), "SocketThread stopped");
        panelTop.setVisible(true);
        panelBottom.setVisible(false);
    }

    @Override
    public void onSocketReady(SocketThread thread, Socket socket) {
        putLog(tfName.getText(), "SocketThread ready");
        panelBottom.setVisible(true);
        panelTop.setVisible(false);
    }

    @Override
    public void onReceiveString(SocketThread thread, Socket socket, String msg) {
        putLog(thread.getName(), msg);
    }

    @Override
    public void onSocketException(SocketThread thread, Exception exception) {
        showException(thread, exception);
    }
}
