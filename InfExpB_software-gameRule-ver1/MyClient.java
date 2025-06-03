import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

abstract class Object {
    public static final int tile_size = 45;
    public BufferedImage img;
    public int size = 45;
    public int x, y; 
    public Color c;

    Object(int xx, int yy, Color col) {
        x = xx * tile_size;
        y = yy * tile_size;
        c = col;
    }

    abstract void draw(Graphics g);
}

class Ground extends Object {
    Ground(int xx, int yy) {
        super(xx, yy, Color.LIGHT_GRAY);
        img = MyClient.gnd_img;
    }

    void draw(Graphics g) {
        g.drawImage(img, x, y, null);
    }
}

class Burned extends Object {
    public boolean anime;
    public Image img2;

    Burned(int xx, int yy, boolean a) {
        super(xx, yy, Color.ORANGE);
        anime = a;
        img = MyClient.burned1_img;
        img2 = MyClient.burned2_img;
    }

    void draw(Graphics g) {
        if (anime) {
            g.drawImage(img, x, y, null);
        } else {
            g.drawImage(img2, x, y, null);
        }
    }
}

class Wall extends Object {
    Wall(int xx, int yy) {
        super(xx, yy, Color.DARK_GRAY);
    }

    void draw(Graphics g) {
        g.setColor(c);
        g.fillRect(x + (tile_size - size) / 2, y + (tile_size - size) / 2, size, size);
    }
}

class Player extends Object {
    public static final Color[] color_list = { Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW  };
    public int life;

    Player(int xx, int yy, int i) {
        super(xx, yy, color_list[i]);
        size = size * 2 / 3;
        img = MyClient.gnd_img;
    }

    void draw(Graphics g) {
        g.drawImage(img, x, y, null);
        g.setColor(c);
        g.fillRect(x + (tile_size - size) / 2, y + (tile_size - size) / 2, size, size);
    }
}

abstract class Bomb extends Object {
    public boolean fired;
    public boolean anime;
    public Image img_f;
    public Image img_f2;

    Bomb(int xx, int yy, Color col, boolean f, boolean a) {
        super(xx, yy, col);
        fired = f;
        anime = a;
    }

    void draw(Graphics g) {
        if (fired) {
            if (anime) {
                g.drawImage(img_f, x, y, null);
            } else {
                g.drawImage(img_f2, x, y, null);
            }
        } else {
            g.drawImage(img, x, y, null);
        }
    }
}

class NomBomb extends Bomb {
    NomBomb(int xx, int yy, boolean f, boolean a) {
        super(xx, yy, Color.BLACK, f, a);
        img = MyClient.bomb_img;
        img_f = MyClient.bombf1_img;
        img_f2 = MyClient.bombf2_img;
    }
}

class FireBomb extends Bomb {
    FireBomb(int xx, int yy, boolean f, boolean a) {
        super(xx, yy, Color.PINK, f, a);
        img = MyClient.firebomb_img;
        img_f = MyClient.firebombf1_img;
        img_f2 = MyClient.firebombf2_img;
    }
}

class FastBomb extends Bomb {
    FastBomb(int xx, int yy, boolean f, boolean a) {
        super(xx, yy, Color.BLUE, f, a);
        img = MyClient.fastbomb_img;
        img_f = MyClient.fastbombf1_img;
        img_f2 = MyClient.fastbombf2_img;
    }
}

public class MyClient {
    public static int N = 10;
    final static private int size = Object.tile_size * N; 
    final static private int XOFFSET = 20; 
    final static private int YOFFSET = 80;
    static public BufferedImage gnd_img, burned1_img, burned2_img, bomb_img, bombf1_img, bombf2_img, firebomb_img,
            firebombf1_img, firebombf2_img, fastbomb_img, fastbombf1_img, fastbombf2_img;
    private Image offscreen = null;

    public static void main(String[] args) {
        try {
            gnd_img = ImageIO.read(new File("./texture/ground.png"));
            burned1_img = ImageIO.read(new File("./texture/burned1.png"));
            burned2_img = ImageIO.read(new File("./texture/burned2.png"));
            bomb_img = ImageIO.read(new File("./texture/nombomb.png"));
            bombf1_img = ImageIO.read(new File("./texture/nombomb_f1.png"));
            bombf2_img = ImageIO.read(new File("./texture/nombomb_f2.png"));
            firebomb_img = ImageIO.read(new File("./texture/firebomb.png"));
            firebombf1_img = ImageIO.read(new File("./texture/firebomb_f1.png"));
            firebombf2_img = ImageIO.read(new File("./texture/firebomb_f2.png"));
            fastbomb_img = ImageIO.read(new File("./texture/fastbomb.png"));
            fastbombf1_img = ImageIO.read(new File("./texture/fastbomb_f1.png"));
            fastbombf2_img = ImageIO.read(new File("./texture/fastbomb_f2.png"));
        } catch (IOException e) {
            System.out.println("image file not found. [" + gnd_img + "]");// [107]
        }
        MyClient client = new MyClient();
        client.start();
    }

    public void start() {
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
            sendTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    String dir = frame.getDirection();
                    out.println(dir);
                }
            }, 0, 10);

            Thread receiveThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String header = line.split(",")[0];
                        if (header.equals("starting")) {
                            frame.showStarting(line.split(",")[1]);
                        }else{
                            frame.showBoard(line);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("error: " + e.getMessage());
                }
            });
            receiveThread.start();
            receiveThread.join();
            socket.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    private class MyFrame extends JFrame {
        private final Set<String> pressedKeys = new HashSet<>();
        private final int anime_rate = 5;
        private int tick = 0;
        
        private final int playerLife = 3;

        boolean[] isAlive = new boolean[4];

        public MyFrame() {
            setTitle("Game");
            setBounds(0, 0, XOFFSET * 2 + size + 250, YOFFSET * 2 + size);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setVisible(true);

            for (int i = 0; i < 4; i++) {
                isAlive[i] = true;
            }

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
            if (pressedKeys.isEmpty())
                return "stopped";
            if (pressedKeys.contains("bomb"))
                return "bomb";
            if (pressedKeys.contains("up"))
                return "up";
            if (pressedKeys.contains("down"))
                return "down";
            if (pressedKeys.contains("left"))
                return "left";
            if (pressedKeys.contains("right"))
                return "right";
            return "stopped";
        }

        public void showStarting(String playerNumber) {//starting screen
            BufferedImage buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g2 = buffer.createGraphics();

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 24));
            g2.drawString("Starting... " + playerNumber + " players now", getWidth() / 2 - 60, getHeight() / 2);

            Graphics g = this.getGraphics();
            g.drawImage(buffer, 0, 0, null);

            g2.dispose();
            g.dispose();
        }


        public void showBoard(String boardData) {
            int[] playersLife = new int[4];
            int[] playersBomb = new int[4];
            int playerNumber = 0;

            for (int i = 0; i < playersBomb.length; i++) {
                playersBomb[i] = 0;
            }

            tick = (++tick) % (anime_rate * 2);
            if (offscreen == null) {
                offscreen = this.createImage(size + 250, size);
            }
            Graphics g = offscreen.getGraphics();
            g.clearRect(0, 0, size + 250, size);

            Object[][] objs = new Object[N][N];
            String[] data = boardData.split(",");
            boolean fire = false;
            for (int i = 0; i < objs.length; i++) {
                for (int j = 0; j < objs[0].length; j++) {
                    int s = Integer.parseInt(data[i * objs.length + j]);
                    switch (s) {
                        case 0:
                            objs[j][i] = new Ground(j, i);
                            break;
                        case 91:
                            fire = true;
                        case 21:
                            objs[j][i] = new NomBomb(j, i, fire, tick / anime_rate == 1);
                            fire = false;
                            break;
                        case 92:
                            fire = true;
                        case 22:
                            objs[j][i] = new FireBomb(j, i, fire, tick / anime_rate == 1);
                            fire = false;
                            break;
                        case 93:
                            fire = true;
                        case 23:
                            objs[j][i] = new FastBomb(j, i, fire, tick / anime_rate == 1);
                            fire = false;
                            break;
                        default:
                            if (s < 0) {
                                objs[j][i] = new Burned(j, i, tick / anime_rate == 1);
                            } else {
                                objs[j][i] = new Player(j, i, s % 4);
                                playersLife[s % 100 - 1] = s / 100 - 10;
                            }
                            break;
                    }
                    objs[j][i].draw(g);
                }
            }

            for (int i = objs.length * objs[0].length; i < data.length; i++) {
                int s = Integer.parseInt(data[i]);
                if ( i == data.length - 1 ) { playerNumber = s; break;}
                playersBomb[s]++;
            }

        
            g.setColor(Color.BLACK);
            g.fillRect(size, 0, 250, size);

            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            int startX = size + 10;
            int startY = 20;
            g.setColor(Color.WHITE);
            g.drawString("[STATUS]", startX, startY);
            startY += 20;

            for (int i = 0; i < 4; i++) {
                g.setColor(Player.color_list[(i + 1) % 4]);
                g.drawString("Player " + (i + 1), startX, startY);
                startY += 18;

               
                g.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g.drawString("Life : ", startX + 10, startY);
                int life = playerLife - playersLife[i];
                StringBuilder sb = new StringBuilder();
                if (isAlive[i]) {
                    for (int j = 0; j < life; j++) {
                        sb.append("\u2665");
                    }
                    if (life == 0) {
                        isAlive[i] = false;
                    }
                }
                g.drawString(sb.toString(), startX + 70, startY);
                startY += 20;

                g.setFont(new Font("SansSerif", Font.PLAIN, 16));
                g.setColor(Color.WHITE);
                String str = ((i + 1) == playerNumber) ? "" + playersBomb[1] : "???";
                g.drawString("\uD83D\uDCA3 (Black): " + str, startX + 10, startY);
                startY += 18;
                str = ((i + 1) == playerNumber) ? "" + playersBomb[2] : "???";
                g.drawString("\uD83D\uDCA3 (Red): " + str, startX + 10, startY);
                startY += 18;
                str = ((i + 1) == playerNumber) ? "" + playersBomb[3] : "???";
                g.drawString("\uD83D\uDCA3 (Blue): " + str, startX + 10, startY);
                startY += 25;
            }

            Graphics currentg = this.getGraphics();
            currentg.drawImage(offscreen, XOFFSET, YOFFSET, this);
        }

    }
}