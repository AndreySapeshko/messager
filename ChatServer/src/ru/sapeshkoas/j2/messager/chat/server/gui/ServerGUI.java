package ru.sapeshkoas.j2.messager.chat.server.gui;

import ru.sapeshkoas.j2.messager.chat.server.core.ChatServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerGUI extends JFrame implements ActionListener {
    private ChatServer chatServer = new ChatServer();
    private static final int POS_X = 100;
    private static final int POS_Y = 100;
    private static final int WIGTH = 300;
    private static final int HEIGHT = 150;
    private JButton btnStart = new JButton("Start");
    private JButton btnStop = new JButton("Stop");
    private JTextArea taLog = new JTextArea();
    private JPanel panelTop = new JPanel();

    public ServerGUI() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(POS_X, POS_Y,WIGTH, HEIGHT);
        setResizable(false);
        setTitle("Chat Server");
        setLayout(new BorderLayout());
        btnStart.addActionListener(this);
        btnStop.addActionListener(this);
        JScrollPane scrollLog = new JScrollPane(taLog);
        panelTop.add(btnStart, LEFT_ALIGNMENT);
        panelTop.add(btnStop, RIGHT_ALIGNMENT);

        add(panelTop, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ServerGUI();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnStart) {
            chatServer.start(8189);
        } else if (src == btnStop) {
            chatServer.stop();
        }
    }
}
