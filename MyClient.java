import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;


import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class MyClient {
    public static int N = 11;

    public static void main(String[] args) {
        final String SERVER_IP = "localhost";
        final int PORT = 50505;

        try {
            Socket socket = new Socket(SERVER_IP, PORT);
            System.out.println("loading...");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            MyFrame frame = new MyFrame();
            frame.setVisible(true);
            frame.requestFocus();

            java.util.Timer sendTimer = new java.util.Timer();
            sendTimer.scheduleAtFixedRate(new TimerTask() {//send
                @Override
                public void run() {
                    String dir = frame.getDirection();
                    out.println(dir);
                }
            }, 0, 100);

            Thread receiveThread = new Thread(() -> {//receive 
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println("received: " + line);
                        frame.showBoard(line);
                    }
                } catch (IOException e) {
                    System.err.println("error: " + e.getMessage());
                }
            });
            receiveThread.start();
            try {
                receiveThread.join();
                socket.close();
            } catch (InterruptedException e) {
            }

        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        } 
    }

    private static class MyFrame extends JFrame {
        private final JLabel[][] cells = new JLabel[N][N];
        private final Set<String> pressedKeys = new HashSet<>();

        public MyFrame() {
            setTitle("Game");
            setSize(300, 300);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new GridLayout(N, N));

            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    cells[i][j] = new JLabel("0", SwingConstants.CENTER);
                    add(cells[i][j]);
                }
            }

        setVisible(true);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_RIGHT:
                            pressedKeys.add("right");
                            break;
                        case KeyEvent.VK_LEFT:
                            pressedKeys.add("left");
                            break;
                        case KeyEvent.VK_UP:
                            pressedKeys.add("up");
                            break;
                        case KeyEvent.VK_DOWN:
                            pressedKeys.add("down");
                            break;
                        case KeyEvent.VK_SPACE:
                            pressedKeys.add("bomb");
                            break;
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_RIGHT:
                            pressedKeys.remove("right");
                            break;
                        case KeyEvent.VK_LEFT:
                            pressedKeys.remove("left");
                            break;
                        case KeyEvent.VK_UP:
                            pressedKeys.remove("up");
                            break;
                        case KeyEvent.VK_DOWN:
                            pressedKeys.remove("down");
                            break;
                        case KeyEvent.VK_SPACE:
                            pressedKeys.remove("bomb");
                            break;
                    }
                }
            });
        }

        public String getDirection() {
            if (pressedKeys.isEmpty()) return "stopped";
            if (pressedKeys.contains("up")) return "up";
            if (pressedKeys.contains("down")) return "down";
            if (pressedKeys.contains("left")) return "left";
            if (pressedKeys.contains("right")) return "right";
            if (pressedKeys.contains("bomb")) return "bomb";
            return "stopped";
        }

        public void showBoard(String boardData){
            String[][] board = new String[N][N];
            String[] data = boardData.split(",");
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    board[i][j] = data[i * board.length + j];
                    cells[i][j].setText(board[i][j]);
                }
            }

        }
    }
}
