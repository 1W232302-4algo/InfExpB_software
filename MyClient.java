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
    public static int N = 10;

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
            }, 0, 10);
            

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
        private final JLabel[][] cells = new JLabel[N][N + 1]; //edit
        private final Set<String> pressedKeys = new HashSet<>();

        public MyFrame() {
            setTitle("Game");
            setSize(400, 400);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new GridLayout(N, N + 1)); //edited

            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N + 1; j++) {//edited
                    cells[i][j] = new JLabel("0", SwingConstants.CENTER);
                    add(cells[i][j]);

                    cells[i][j].setOpaque(true);
                    if (j == N) {
                        cells[i][j].setBackground(java.awt.Color.LIGHT_GRAY);
                    }
                }
            }



            setVisible(true);

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_SPACE:
                            pressedKeys.add("bomb");
                            break;
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
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_SPACE:
                            pressedKeys.remove("bomb");
                            break;
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
                    }
                }
            });
        }

        public String getDirection() {
            if (pressedKeys.isEmpty()) return "stopped";
            if (pressedKeys.contains("bomb")) return "bomb";
            if (pressedKeys.contains("up")) return "up";
            if (pressedKeys.contains("down")) return "down";
            if (pressedKeys.contains("left")) return "left";
            if (pressedKeys.contains("right")) return "right";
            return "stopped";
        }

        public void showBoard(String boardData){
            String[][] board = new String[N][N];
            String[] data = boardData.split(",");
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    board[i][j] = data[i * board.length + j];
                    cells[i][j].setText(board[i][j]);
                    switch(Integer.parseInt(board[i][j])){
                        case 21:
                            cells[i][j].setForeground(java.awt.Color.GRAY);
                            cells[i][j].setBackground(java.awt.Color.YELLOW);
                            break;
                        case 22:
                            cells[i][j].setForeground(java.awt.Color.RED);
                            cells[i][j].setBackground(java.awt.Color.YELLOW);
                            break;
                        case 23:
                            cells[i][j].setForeground(java.awt.Color.BLUE);
                            cells[i][j].setBackground(java.awt.Color.YELLOW);
                            break;
                        case 91:
                            cells[i][j].setForeground(java.awt.Color.GRAY);
                            break;
                        case 92:
                            cells[i][j].setForeground(java.awt.Color.RED);
                            break;
                        case 93:
                            cells[i][j].setForeground(java.awt.Color.BLUE);
                            break;
                        default:
                            cells[i][j].setForeground(java.awt.Color.BLACK);
                            cells[i][j].setBackground(java.awt.Color.WHITE);
                            break;
                    }
                }
            }

            for (int i = 0; i < data.length - board.length * board.length; i++) {
                cells[i][N].setText(data[board.length * board.length + i]);
            }

        }
    }
}
