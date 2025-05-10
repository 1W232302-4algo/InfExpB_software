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
    public static int N = 11;
    public static int PLAYERS = 5;
    public static void main(String[] args) {
        final int PORT = 50505;
        
        MyGameBoard gameBoard = new MyGameBoard();
        MyGameBoard.MyClock clock = gameBoard.new MyClock();
        clock.start();
        boolean running = true;

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("game ready");
            AtomicInteger playerNumber = new AtomicInteger(1000);

            while(running){
                System.out.println("PORT:" + PORT);
                Socket clientSocket = serverSocket.accept();

                Thread clientThread = new Thread(() -> {
                    try {
                        System.out.println("client: " + clientSocket.getInetAddress());

                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        MyGameBoard.MyPlayer player = gameBoard.new MyPlayer(playerNumber.incrementAndGet(), playerNumber.get()%100, 0);

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
        private MyPlayer[] players = new MyPlayer[PLAYERS];
        private final int[][] board = new int[N][N];
        private final int bombRange = 3;
        private final int bombDelay = 1000;//1000ms
        private final int bombTime = -50;//5000ms
        private final int waitTime = 5;//500ms

        public MyGameBoard() {
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    board[i][j] = 0;
                }
            }
        }

        public synchronized void setPlayers(MyPlayer player) {
            this.players[player.playerNumber%100] = player;
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
                board[x][y] = 99;
            }
        }

        public synchronized void setFire(int x, int y) {
            if(board[x][y] == 99) {
                board[x][y] = bombTime;
                for(int i=1; i<bombRange; i++) {
                    if(x+i < board.length && board[x+i][y] <= 0){
                        board[x+i][y] = bombTime;
                    }else{
                        if(x+i < board.length && board[x+i][y] > 1000){
                            board[x+i][y] += 100;
                            int playernum = players[board[x+i][y]%100].playerNumber;
                            players[board[x+i][y]%100].updatePlayerNumber(playernum + 100);
                            //bombed
                        }
                        break;
                    }
                }
                for(int i=1; i<bombRange; i++) {
                    if(x-i >= 0 && board[x-i][y] <= 0){
                        board[x-i][y] = bombTime;
                    }else{
                        if(x-i >= 0 && board[x-i][y] > 1000){
                            board[x-i][y] += 100;
                            int playernum = players[board[x-i][y]%100].playerNumber;
                            players[board[x-i][y]%100].updatePlayerNumber(playernum + 100);
                            //bombed
                        }
                        break;
                    }
                }
                for(int i=1; i<bombRange; i++) {
                    if(y+i < board.length && board[x][y+i] <= 0){
                        board[x][y+i] = bombTime;
                    }else{
                        if(y+i < board.length && board[x][y+i] > 1000){
                            board[x][y+i] += 100;
                            int playernum = players[board[x][y+i]%100].playerNumber;
                            players[board[x][y+i]%100].updatePlayerNumber(playernum + 100);
                            //bombed
                        }
                        break;
                    }
                }
                for(int i=1; i<bombRange; i++) {
                    if(y-i >= 0 && board[x][y-i] <= 0){
                        board[x][y-i] = bombTime;
                    }else{
                        if(y-i >= 0 && board[x][y-i] > 1000){
                            board[x][y-i] += 100;
                            int playernum = players[board[x][y-i]%100].playerNumber;
                            players[board[x][y-i]%100].updatePlayerNumber(playernum + 100);
                            //bombed
                        }
                        break;
                    }
                }
            }
        }

        public synchronized void clockFire(){
            for(int i=0; i<board.length; i++){
                for(int j=0; j<board.length; j++){
                    if(board[i][j] < 0){
                        board[i][j]++;
                    }
                }
            }
        }

        public synchronized void clockPlayerWaitTime(){
            for(int i=0; i<PLAYERS; i++){
                if(players[i] != null && players[i].playerWaitTime > 0){
                    players[i].playerWaitTime--;
                }
            }
        }

        private class MyPlayer {
            private int playerNumber;
            private int x, y;
            private String posture = "right";
            private int playerWaitTime = 0;
            private String playerCondition = "alive";

            public MyPlayer(int playerNumber, int x, int y) {
                this.playerNumber = playerNumber;
                this.x = x;
                this.y = y;
                setPlayers(this);
                updateBoard(playerNumber, x, y);
            }

            public void updatePlayerNumber(int playerNumber) {
                this.playerNumber = playerNumber;
            }

            public void moveRight() {
                if(playerWaitTime == 0 && playerCondition.equals("alive")){
                    if(y != board.length - 1) {
                        int newY = y + 1;
                        if(move(playerNumber, x, y, x, newY)){
                            y = newY;
                        }
                    }
                    posture = "right";

                    playerWaitTime = waitTime;
                }
            }

            public void moveLeft() {
                if(playerWaitTime == 0 && playerCondition.equals("alive")){
                    if(y != 0) {
                        int newY = y - 1;
                        if(move(playerNumber, x, y, x, newY)){
                            y = newY;
                        }
                    }
                    posture = "left";

                    playerWaitTime = waitTime;
                }
            }

            public void moveUp() {
                if(playerWaitTime == 0 && playerCondition.equals("alive")){
                    if(x != 0) {
                        int newX = x - 1;
                        if(move(playerNumber, x, y, newX, y)){
                            x = newX;
                        }
                    }
                    posture = "up";

                    playerWaitTime = waitTime;
                }
            }

            public void moveDown() {
                if(playerWaitTime == 0 && playerCondition.equals("alive")){
                    if(x != board.length - 1) {
                        int newX = x + 1;
                        if(move(playerNumber, x, y, newX, y)){
                            x = newX;
                        }
                    }
                    posture = "down";

                    playerWaitTime = waitTime;
                }
            }

            public void playerSetBomb(){
                if(playerCondition.equals("alive")){
                    int bombX, bombY;
                    switch(posture){
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
                        new MyBomb(bombX, bombY).start();
                    }

                    playerWaitTime = waitTime;
                }                
            }
        }

        private class MyBomb{
            private int x, y;

            public MyBomb(int x, int y) {
                this.x = x;
                this.y = y;
            }

            public synchronized void start() {
                Thread bombThread = new Thread(() -> {
                    try {
                        setBomb(x, y);
                        Thread.sleep(bombDelay);
                        setFire(x, y);
                    } catch (InterruptedException e) {
                        System.err.println("error: " + e.getMessage());
                    }
                });
                bombThread.start();
            }

        }

        private class MyClock{
            public synchronized void start() {
                Thread clockThread = new Thread(() -> {
                    try {
                        while(true){
                            clockFire();
                            clockPlayerWaitTime();
                            Thread.sleep(100);
                        }
                    } catch (InterruptedException e) {
                        System.err.println("error: " + e.getMessage());
                    }
                });
                clockThread.start();
            }
        }

    }
}
