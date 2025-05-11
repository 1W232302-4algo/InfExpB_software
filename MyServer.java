import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MyServer {
    public static final int N = 10;
    public static final int PLAYERS = 5;//four players(5 - 1 = 4)
    public static final int[][] startCoordinates = {{0, 0}, {N - 1, 0}, {0, N - 1}, {N - 1, N - 1}}; 
    public static void main(String[] args) {
        final int PORT = 50505;

        Map<Integer, PrintWriter> playerOutputs = new ConcurrentHashMap<>();

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

                        MyGameBoard.MyPlayer player = gameBoard.new MyPlayer(playerNumber.incrementAndGet(), startCoordinates[playerNumber.get() % 100 -1][0], startCoordinates[playerNumber.get() % 1000 -1][1]);

                        playerOutputs.put(playerNumber.get() % 100, out);

                        Thread receiveThread = new Thread(() -> {//receive
                            try {
                                String inputLine;
                                while ((inputLine = in.readLine()) != null) {
                                    System.out.println("received: " + inputLine);
                                    switch (inputLine) {
                                        case "right":
                                            player.moveDirection(inputLine);
                                            break;
                                        case "left":
                                            player.moveDirection(inputLine);
                                            break;
                                        case "up":
                                            player.moveDirection(inputLine);
                                            break;
                                        case "down":
                                            player.moveDirection(inputLine);
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
                                for (Map.Entry<Integer, PrintWriter> entry : playerOutputs.entrySet()) {
                                    int playerId = entry.getKey();
                                    PrintWriter out = entry.getValue();
                                    out.println(gameBoard.BoardData() + gameBoard.players[playerId].getItem());
                                }
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
        private final int bombDelayTimeShort = 1000;//1000ms
        private final int bombDelayTimeLong = 3000;//3000ms
        private final int bombTypes = 3;
        private final int bombTimeShort = -1;//0.1s
        private final int bombTimeLong = -50;//5s
        private final int bombDigit = 90;
        private final int waitTime = 5;//0.5s
        private final int bombWaitingTime = 3;//0.3s
        private final int itemNumber = 6;
        private final int itemTypes = 3;
        private final int itemTime = 100;//10s
        private final int itemDigit = 20;
        private final int sponedItemNumber = 5;

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
            if(board[x_after][y_after] > itemDigit && board[x_after][y_after] <= itemDigit + itemTypes) {
                players[playerNumber%100].updateItem(board[x_after][y_after] - itemDigit);
                board[x_after][y_after] = playerNumber;
                board[x_before][y_before] = 0;
                return true;
            }
            return false;
        }

        public synchronized void setBomb(int x, int y, int bombType) {
            if(board[x][y] == 0) {
                board[x][y] = bombDigit + bombType;
            }
        }

        public synchronized void setFire(int x, int y, int bombType) {
            int bombReallyTime = bombTimeShort;
            if(bombType == 2) bombReallyTime = bombTimeLong;
            if(board[x][y] > bombDigit && board[x][y] <= bombDigit + bombTypes) {
                board[x][y] = bombReallyTime;
                for(int i=1; i<bombRange; i++) {
                    if(x+i < board.length && board[x+i][y] <= 0){
                        board[x+i][y] = bombReallyTime;
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
                        board[x-i][y] = bombReallyTime;
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
                        board[x][y+i] = bombReallyTime;
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
                        board[x][y-i] = bombReallyTime;
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

        public synchronized void setItem() {
            for(int i=0; i<sponedItemNumber; i++){
                int itemType = (int) (Math.random() * itemTypes) + itemDigit + 1;
                int x = (int) (Math.random() * board.length);
                int y = (int) (Math.random() * board.length);
                if(board[x][y] == 0){
                    board[x][y] = itemType;
                }else{
                    i--;
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
                if(players[i] != null && players[i].playerBombWaitTime > 0){
                    players[i].playerBombWaitTime--;
                }
            }
        }

        private class MyPlayer {
            private int playerNumber;
            private int x, y;
            private String posture = "right";
            private int playerWaitTime = 0;
            private int playerBombWaitTime = 0;
            private String playerCondition = "waiting";
            private int[] items = new int[itemNumber];
            private int itemTail = 0;

            private final int[][] movingDirections = {{0, 1}, {0, -1}, {-1, 0}, {1, 0}};

            public MyPlayer(int playerNumber, int x, int y) {
                this.playerNumber = playerNumber;
                this.x = x;
                this.y = y;
                this.playerCondition = "alive";
                setPlayers(this);
                updateBoard(playerNumber, x, y);
                items[0] = 2;
                itemTail++;
            }

            public void updatePlayerNumber(int playerNumber) {
                this.playerNumber = playerNumber;
            }

            public void moveDirection(String direction) {
                if(playerWaitTime == 0 && playerCondition.equals("alive")) {
                    int[] playerMovingDirection = {0, 0};
                    switch (direction) {
                        case "right":
                            playerMovingDirection = movingDirections[0];
                            break;
                        case "left":
                            playerMovingDirection = movingDirections[1];
                            break;
                        case "up":
                            playerMovingDirection = movingDirections[2];
                            break;
                        case "down":
                            playerMovingDirection = movingDirections[3];
                            break;
                        default:
                            break;
                    }
                    int newX = x + playerMovingDirection[0];
                    int newY = y + playerMovingDirection[1];
                    if(newX >= 0 && newX < board.length && newY >= 0 && newY < board.length) {
                        if(move(playerNumber, x, y, newX, newY)){
                            x = newX;
                            y = newY;
                        }
                    }
                    posture = direction;

                    playerWaitTime = waitTime;
                }
            }

            public void playerSetBomb(){
                if(items[0] > 0 && playerCondition.equals("alive")){
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
                    if(playerBombWaitTime == 0 && 0<= bombX && bombX < board.length && 0<= bombY && bombY < board.length) {
                        new MyBomb(bombX, bombY, items[0]).start();
                        for(int i = 1; i<items.length; i++){
                            items[i-1] = items[i];
                        }
                        items[items.length-1] = 0;
                        itemTail--;
                        playerBombWaitTime = bombWaitingTime;
                    }

                    playerWaitTime = waitTime;

                }      
            }

            public void updateItem(int Myitem){
                if(itemTail < itemNumber){
                    items[itemTail] = Myitem;
                    itemTail++;
                }
            }

            public String getItem(){
                String data = "";
                for(int i=0; i<items.length; i++){
                    data += items[i] + ",";
                }
                return data;
            }
        }

        private class MyBomb{
            private int x, y, bombType;

            public MyBomb(int x, int y, int bombType) {
                this.x = x;
                this.y = y;
                this.bombType = bombType;
            }

            public synchronized void start() {
                Thread bombThread = new Thread(() -> {
                    try {
                        int bombDelayTime;
                        if(bombType == 3){
                            bombDelayTime = bombDelayTimeShort;
                        }else{
                            bombDelayTime = bombDelayTimeLong;
                        }
                        setBomb(x, y, bombType);
                        Thread.sleep(bombDelayTime);
                        setFire(x, y, bombType);
                    } catch (InterruptedException e) {
                        System.err.println("error: " + e.getMessage());
                    }
                });
                bombThread.start();
            }
        }

        private class MyClock{
            public synchronized void start() {
                AtomicInteger count = new AtomicInteger(0);
                Thread clockThread = new Thread(() -> {
                    try {
                        while(true){
                            clockFire();
                            clockPlayerWaitTime();
                            Thread.sleep(100);
                            count.incrementAndGet();
                            if(count.get() == itemTime){
                                setItem();
                                count.set(0);
                            }
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
