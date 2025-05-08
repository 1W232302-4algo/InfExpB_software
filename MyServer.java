import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class MyServer {
    public static void main(String[] args) {
        final int PORT = 50505;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("PORT:" + PORT);
            Socket clientSocket = serverSocket.accept();
            System.out.println("client: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

            MyGameBoard gameBoard = new MyGameBoard();
            MyGameBoard.MyPlayer player1 = gameBoard.new MyPlayer(1, 0, 0);
            System.out.println("game ready");

            Thread receiveThread = new Thread(() -> {//receive
                try {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("received: " + inputLine);
                        switch (inputLine) {
                            case "right":
                                player1.moveRight();
                                break;
                            case "left":
                                player1.moveLeft();
                                break;
                            case "up":
                                player1.moveUp();
                                break;
                            case "down":
                                player1.moveDown();
                                break;
                        }
                    }
                } catch (IOException e) {
                    System.err.println("error: " + e.getMessage());
                }
            });
            receiveThread.start();

        
            Timer sendTimer = new Timer();
            sendTimer.scheduleAtFixedRate(new TimerTask() {//send
                @Override
                public void run() {
                    out.println(gameBoard.BoardData());
                }
            }, 0, 100);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class MyGameBoard {
        private final int[][] board = new int[3][3];

        public MyGameBoard() {
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    board[i][j] = 0;
                }
            }
        }

        public String BoardData() {
            String data = "";
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    data += board[i][j] + ",";
                }
            }
            return data;
        }

        private class MyPlayer {
            private final int playerNumber;
            private int x, y;

            public MyPlayer(int playerNumber, int x, int y) {
                this.playerNumber = playerNumber;
                this.x = x;
                this.y = y;
                board[x][y] = playerNumber;
            }

            public void moveRight() {
                board[x][y] = 0;
                if (y != board.length - 1) {
                    y++;
                }
                board[x][y] = playerNumber;
            }

            public void moveLeft() {
                board[x][y] = 0;
                if (y != 0) {
                    y--;
                }
                board[x][y] = playerNumber;
            }

            public void moveUp() {
                board[x][y] = 0;
                if (x != 0) {
                    x--;
                }
                board[x][y] = playerNumber;
            }

            public void moveDown() {
                board[x][y] = 0;
                if (x != board.length - 1) {
                    x++;
                }
                board[x][y] = playerNumber;
            }
        }

    }
}
