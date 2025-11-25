import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * WhatsApp-like ChatClientGUI (Swing)
 * Usage: java -cp out ChatClientGUI [serverIP] [port]
 *
 * Replace your src/ChatClientGUI.java with this file.
 */
public class ChatClientGUI extends JFrame {
    private final JPanel messagesPanel;           // panel that holds message bubbles (vertical)
    private final JScrollPane messagesScroll;     // scroll container
    private final JTextField inputField;
    private final JButton sendButton;
    private PrintWriter out;
    private BufferedReader in;
    private String name;
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm");

    public ChatClientGUI(String serverIP, int port) {
        super("Java Chat — WhatsApp Style");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 700);
        setLocationRelativeTo(null);

        // Top header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(7, 94, 84)); // WhatsApp-ish green
        header.setBorder(new EmptyBorder(10, 12, 10, 12));
        JLabel title = new JLabel("JavaChat");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);

        JLabel subtitle = new JLabel("Connected");
        subtitle.setForeground(new Color(200, 255, 240));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        header.add(subtitle, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Messages area (center) — with vertical BoxLayout inside a scroll pane
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(new Color(238, 238, 238));
        messagesPanel.setBorder(new EmptyBorder(10,10,10,10));

        messagesScroll = new JScrollPane(messagesPanel);
        messagesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        messagesScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(messagesScroll, BorderLayout.CENTER);

        // Bottom input area
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(new EmptyBorder(10,10,10,10));
        inputField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.setPreferredSize(new Dimension(100, 36));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Networking: connect to server
        try {
            Socket socket = new Socket(serverIP, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            name = JOptionPane.showInputDialog(this, "Enter your name:", "Name", JOptionPane.PLAIN_MESSAGE);
            if (name == null || name.trim().isEmpty()) name = "User" + ((int)(Math.random() * 1000));
            out.println(name); // send name to server as first message
            addSystemMessage("Connected as " + name);
            subtitle.setText("Logged in as " + name);

            // Reader thread: handle messages from server
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        // server broadcasts: "Name: message" or join/leave messages like "Alice has joined the chat."
                        if (line.endsWith("has joined the chat.") || line.endsWith("has left the chat.")) {
                            addSystemMessage(line);
                        } else {
                            int colon = line.indexOf(":");
                            if (colon > 0) {
                                String sender = line.substring(0, colon).trim();
                                String text = line.substring(colon + 1).trim();
                                boolean isMe = sender.equals(name); // server currently does not broadcast back to sender, but just in case
                                addMessageBubble(sender, text, isMe);
                            } else {
                                addSystemMessage(line);
                            }
                        }
                    }
                } catch (IOException ex) {
                    addSystemMessage("Disconnected from server.");
                }
            }).start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to connect: " + ex.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        // Send action
        ActionListener sendAction = e -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;
            // show as own message (right bubble)
            addMessageBubble("Me", text, true);
            out.println(text);
            inputField.setText("");
            inputField.requestFocusInWindow();
        };

        sendButton.addActionListener(sendAction);
        inputField.addActionListener(sendAction);

        setVisible(true);
        inputField.requestFocusInWindow();
    }

    // Adds a small centered system message (join/leave/errors)
    private void addSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
            p.setOpaque(false);
            JLabel label = new JLabel("<html><i>" + escapeHtml(text) + "</i></html>");
            label.setForeground(Color.DARK_GRAY);
            label.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            p.add(label);
            messagesPanel.add(p);
            messagesPanel.add(Box.createVerticalStrut(6));
            revalidateAndScroll();
        });
    }

    // Adds a message bubble. If isMe=true -> right aligned green bubble, else left aligned gray bubble.
    private void addMessageBubble(String sender, String text, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            // wrapper to control left/right alignment
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);

            // bubble panel holds sender (optional) and message
            JPanel bubble = new JPanel();
            bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
            bubble.setBorder(new EmptyBorder(6,10,6,10));
            JTextArea msgArea = new JTextArea(text);
            msgArea.setLineWrap(true);
            msgArea.setWrapStyleWord(true);
            msgArea.setEditable(false);
            msgArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            msgArea.setOpaque(false);

            JLabel senderLabel = new JLabel(sender.equals("Me") ? "You" : sender);
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            senderLabel.setBorder(new EmptyBorder(0,0,4,0));
            senderLabel.setForeground(new Color(60,60,60));

            // bubble styling
            JPanel bubbleBg = new JPanel(new BorderLayout());
            bubbleBg.setBorder(new EmptyBorder(8,8,8,8));
            bubbleBg.setLayout(new BorderLayout());
            bubbleBg.setOpaque(true);

            // background colors
            if (isMe) {
                bubbleBg.setBackground(new Color(37,211,102)); // green
                msgArea.setForeground(Color.BLACK);
                senderLabel.setForeground(new Color(0,30,0));
            } else {
                bubbleBg.setBackground(new Color(240,240,240)); // light gray
                msgArea.setForeground(Color.BLACK);
                senderLabel.setForeground(new Color(40,40,40));
            }

            // create content in bubble
            bubble.add(senderLabel);
            bubble.add(msgArea);
            bubbleBg.add(bubble, BorderLayout.CENTER);

            // timestamp
            JLabel timeLabel = new JLabel(timeFmt.format(new Date()));
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            timeLabel.setBorder(new EmptyBorder(4,6,0,6));
            timeLabel.setForeground(new Color(60,60,60));

            // place bubble left or right
            if (isMe) {
                JPanel rightWrap = new JPanel(new BorderLayout());
                rightWrap.setOpaque(false);
                rightWrap.add(bubbleBg, BorderLayout.EAST);
                rightWrap.add(timeLabel, BorderLayout.SOUTH);
                wrapper.add(rightWrap, BorderLayout.EAST);
            } else {
                JPanel leftWrap = new JPanel(new BorderLayout());
                leftWrap.setOpaque(false);
                leftWrap.add(bubbleBg, BorderLayout.WEST);
                leftWrap.add(timeLabel, BorderLayout.SOUTH);
                wrapper.add(leftWrap, BorderLayout.WEST);
            }

            messagesPanel.add(wrapper);
            messagesPanel.add(Box.createVerticalStrut(8));
            revalidateAndScroll();
        });
    }

    // ensure the scroll follows the newest message
    private void revalidateAndScroll() {
        messagesPanel.revalidate();
        messagesPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar v = messagesScroll.getVerticalScrollBar();
            v.setValue(v.getMaximum());
        });
    }

    // small HTML escape for system messages
    private String escapeHtml(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    public static void main(String[] args) {
        String server = (args.length >= 1) ? args[0] : "127.0.0.1";
        int port = (args.length >= 2) ? Integer.parseInt(args[1]) : 1234;
        SwingUtilities.invokeLater(() -> new ChatClientGUI(server, port));
    }
}

// ./scripts/build.sh --> in the terminal first

// ./scripts/run_server.sh --> this starts server

// ./scripts/run_client_console.sh --> add new clents 
