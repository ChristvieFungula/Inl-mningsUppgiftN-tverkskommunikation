package InlämningsuppgiftNätverkProgramering;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.HashSet;

public class MultiChatAssignment extends JFrame {
    private JTextArea messageField;
    private JTextArea chatArea;
    private JButton sendButton;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private final String chatIp = "224.0.0.1";
    private final int chatPort = 55555;
    private MulticastSocket socket;
    private DatagramPacket packet;
    private InetAddress group;
    private final String userName;
    private final HashSet<String> users = new HashSet<>();

    // Protocol constants
    private static final String JOINED_SUFFIX = " has joined the chat.";
    private static final String LEFT_SUFFIX = " has left the chat.";
    private static final String WHO_IS_HERE = "Online in the chat: ";
    private static final String I_AM = " ";

    public MultiChatAssignment(String userName) {
        this.userName = userName;

        setTitle("Messenger: " + userName);
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // GUI panel
        JPanel menuPanel = new JPanel(new BorderLayout());
        setContentPane(menuPanel);

        // Chat area
        chatArea = new JTextArea(22, 22);
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.EAST);

        // Message input panel
        JPanel userInputPanel = new JPanel(new FlowLayout());
        JLabel messageLabel = new JLabel("Message: ");
        messageField = new JTextArea(3, 23);
        messageField.setEditable(true);

        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> {
            String userInput = messageField.getText().trim();
            if (!userInput.isEmpty()) {
                String message = userName + ": " + userInput;
                sendMessage(message);
                messageField.setText("");
            }
        });

        add(userInputPanel, BorderLayout.SOUTH);
        userInputPanel.add(messageLabel);
        userInputPanel.add(new JScrollPane(messageField));
        userInputPanel.add(sendButton);

        // User list
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.add(new JLabel("Online now:"), BorderLayout.NORTH);
        userListModel = new DefaultListModel<>();
        userListModel.addElement(userName);
        users.add(userName);  // Add self
        userList = new JList<>(userListModel);
        userListPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        add(userListPanel, BorderLayout.WEST);

        // Disconnect button
        JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnect());
        userListPanel.add(disconnectButton, BorderLayout.SOUTH);

        try {
            socket = new MulticastSocket(chatPort);
            socket.setLoopbackMode(false);
            socket.setNetworkInterface(NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
            group = InetAddress.getByName(chatIp);
            socket.joinGroup(group);

            // Announce presence
            sendMessage(userName + JOINED_SUFFIX);

            // Ask others who is online
            sendMessage(WHO_IS_HERE);

        } catch (Exception e) {
            e.printStackTrace();
        }

        setVisible(true);

        // Start listener in a new thread
        new Thread(this::receiveMessages).start();
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                sendMessage(userName + LEFT_SUFFIX);
                socket.leaveGroup(group);
                socket.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            JOptionPane.showMessageDialog(this, "You have disconnected.", "Disconnected", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
    }

    private void sendMessage(String message) {
        try {
            byte[] buf = message.getBytes();
            packet = new DatagramPacket(buf, buf.length, group, chatPort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        while (true) {
            byte[] buf = new byte[1024];
            try {
                packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                chatArea.append(message + "\n");

                if (message.equals(WHO_IS_HERE)) {
                    sendMessage(I_AM + userName);
                } else if (message.startsWith(I_AM)) {
                    String otherUser = message.substring(I_AM.length());
                    if (!users.contains(otherUser)) {
                        users.add(otherUser);
                        userListModel.addElement(otherUser);
                    }
                } else if (message.endsWith(JOINED_SUFFIX)) {
                    String newUser = message.substring(0, message.indexOf(JOINED_SUFFIX));
                    if (!users.contains(newUser)) {
                        users.add(newUser);
                        userListModel.addElement(newUser);
                    }
                } else if (message.endsWith(LEFT_SUFFIX)) {
                    String leftUser = message.substring(0, message.indexOf(LEFT_SUFFIX));
                    users.remove(leftUser);
                    userListModel.removeElement(leftUser);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Enter your username:");
        if (name == null || name.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Please enter your username.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else {
            new MultiChatAssignment(name);
        }
    }
}