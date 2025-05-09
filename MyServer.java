import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class MyServer {
    public static int N = 4;
    public static int PLAYERS = 4;
    public static void main(String[] args) {
        final int PORT = 50505;
        
        MyGameBoard gameBoard = new MyGameBoard();
        boolean running = true;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("game ready");
            AtomicInteger playerNumber = new AtomicInteger(0);

            while(running){
                System.out.println("PORT:" + PORT);
                Socket clientSocket = serverSocket.accept();

                Thread clientThread = new Thread(() -> {
                    try {
                        System.out.println("client: " + clientSocket.getInetAddress());

                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        MyGameBoard.MyPlayer player = gameBoard.new MyPlayer(playerNumber.incrementAndGet(), playerNumber.get(), 0);

                        Thread receiveThread = new Thread(() -> {//receive
                            try {
                                String inputLine;
                                while ((inputLine = in.readLine()) != null) {
                                    System.out.println("received: " + inputLine);
                                    switch (inputLine) {
                                        case "right":
                                            player.moveRight();
                                            break;
                                        case "left":
                                            player.moveLeft();
                                            break;
                                        case "up":
                                            player.moveUp();
                                            break;
                                        case "down":
                                            player.moveDown();
                                            break;
                                        case "bomb":
                                            player.playerSetBomb();
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
                    } catch (Exception e) {
                    }
                });
                clientThread.start();
            }

        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    private static class MyGameBoard {
        private final int[][] board = new int[N][N];
        private final String posture[] = new String[PLAYERS];

        public MyGameBoard() {
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    board[i][j] = 0;
                }
            }
        }

        public synchronized String BoardData() {
            String data = "";
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    data += board[i][j] + ",";
                }
            }
            return data;
        }

        public synchronized void updateBoard(int playerNumber, int x, int y) {
            board[x][y] = playerNumber;
        }

        public synchronized void updatePosture(int playerNumber, String posture) {
            this.posture[playerNumber] = posture;
        }

        public synchronized String getPosture(int playerNumber) {
            return posture[playerNumber];
        }

        public synchronized boolean move(int playerNumber, int x_before, int y_before, int x_after, int y_after) {
            if(board[x_after][y_after] == 0) {
                board[x_after][y_after] = playerNumber;
                board[x_before][y_before] = 0;
                return true;
            }
            return false;
        }

        public synchronized void setBomb(int x, int y) {
            if(board[x][y] == 0) {
                board[x][y] = -1;
            }
        }

        private class MyPlayer {
            private int playerNumber, x, y;

            public MyPlayer(int playerNumber, int x, int y) {
                this.playerNumber = playerNumber;
                this.x = x;
                this.y = y;
                updateBoard(playerNumber, x, y);
            }

            public void moveRight() {
                if(y != board.length - 1) {
                    int newY = y + 1;
                    if(move(playerNumber, x, y, x, newY)){
                        y = newY;
                    }
                }
                updatePosture(playerNumber, "right");
            }

            public void moveLeft() {
                if(y != 0) {
                    int newY = y - 1;
                    if(move(playerNumber, x, y, x, newY)){
                        y = newY;
                    }
                }
                updatePosture(playerNumber, "left");
            }

            public void moveUp() {
                if(x != 0) {
                    int newX = x - 1;
                    if(move(playerNumber, x, y, newX, y)){
                        x = newX;
                    }
                }
                updatePosture(playerNumber, "up");
            }

            public void moveDown() {
                if(x != board.length - 1) {
                    int newX = x + 1;
                    if(move(playerNumber, x, y, newX, y)){
                        x = newX;
                    }
                }
                updatePosture(playerNumber, "down");
            }

            public void playerSetBomb(){
                int bombX, bombY;
                switch(getPosture(playerNumber)){
                    case "right":
                        bombX = x;
                        bombY = y + 1;
                        break;
                    case "left":
                        bombX = x;
                        bombY = y - 1;
                        break;
                    case "up":
                        bombX = x - 1;
                        bombY = y;
                        break;
                    case "down":
                        bombX = x + 1;
                        bombY = y;
                        break;
                    default:
                        bombX = x;
                        bombY = y;
                        break;
                }

                if(0<= bombX && bombX < board.length && 0<= bombY && bombY < board.length) {
                    setBomb(bombX, bombY);
                }
                
            }
        }

    }
}
