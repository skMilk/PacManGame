import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class PacManGame extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private int pacmanX, pacmanY, pacmanSize = 30;
    private int ghostSize = 30;
    private int speed;
    private int ghostSpeed = 3; // Constant ghost speed
    private int score;
    private final int dotSize = 10;
    private ArrayList<Point> dots;
    private ArrayList<Ghost> ghosts;
    private int level;
    private boolean gameOver;
    private String difficulty;
    private Point powerUp;
    private boolean powerUpActive;
    private int powerUpDuration = 100; // Duration of power-up in game ticks
    private int ticksSincePowerUp;
    private Random random;
    private long lastPowerUpTime;
    private boolean inMenu;

    // Wall layouts for different levels (example data)
    private int[][][] wallLayouts = {
        { {50, 50, 10, 300}, {50, 50, 300, 10}, {340, 50, 10, 300}, {50, 340, 300, 10} },
        { {50, 100, 10, 200}, {150, 50, 200, 10}, {50, 50, 10, 10} },
        // Add more levels with wall layouts as needed
    };

    public PacManGame() {
        random = new Random();
        lastPowerUpTime = System.currentTimeMillis();
        inMenu = true;
        initializeMenu();
    }

    private void initializeMenu() {
        String message = "Welcome to Pac-Man!\n"
                + "Use the following keys to control Pac-Man:\n"
                + "W - Move Up\n"
                + "A - Move Left\n"
                + "S - Move Down\n"
                + "D - Move Right\n"
                + "Eat the dots to gain points and avoid the ghosts!\n"
                + "Press Enter to start the game.";
        JOptionPane.showMessageDialog(null, message, "Pac-Man Game", JOptionPane.INFORMATION_MESSAGE);
        startGame();
    }

    private void startGame() {
        String[] options = {"Easy", "Medium", "Hard"};
        difficulty = (String) JOptionPane.showInputDialog(null, "Choose Difficulty", "Difficulty",
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        switch (difficulty) {
            case "Easy":
                speed = 6; // Pac-Man speed
                break;
            case "Medium":
                speed = 8; // Pac-Man speed
                break;
            case "Hard":
                speed = 10; // Pac-Man speed
                break;
        }

        initializeGame();
        timer = new Timer(30, this);
        timer.start();
        setFocusable(true);
        addKeyListener(this);
    }

    private void initializeGame() {
        random = new Random();
        score = 0;
        level = 1;
        gameOver = false;
        powerUpActive = false;
        ticksSincePowerUp = 0;
        dots = new ArrayList<>();
        ghosts = new ArrayList<>();
        initDots();
        initGhosts();
        
        // Ensure Pac-Man starts in a valid position
        do {
            pacmanX = random.nextInt(11) * 30 + 50; // Random x coordinate
            pacmanY = random.nextInt(11) * 30 + 50; // Random y coordinate
        } while (collidesWithWall(pacmanX, pacmanY)); // Ensure it's not colliding with a wall
    }

    private void initDots() {
        dots.clear();
        for (int i = 50; i < 350; i += 30) {
            for (int j = 50; j < 350; j += 30) {
                dots.add(new Point(i, j));
            }
        }
    }

    private void initGhosts() {
        ghosts.clear();
        String[] ghostTypes = {"chaser", "random", "random", "chaser", "random"};

        for (int i = 0; i < Math.min(level + 1, 5); i++) {
            Color color = Color.getHSBColor(random.nextFloat(), 1.0f, 1.0f);
            ghosts.add(new Ghost(200 + i * 30, 200 + i * 30, ghostSize, ghostTypes[i % ghostTypes.length], color));
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(Color.BLACK);
        if (inMenu) {
            drawMenu(g);
        } else {
            drawWalls(g);
            drawDots(g);
            drawPacman(g);
            drawGhosts(g);
            drawPowerUp(g);
            drawScore(g);
            drawLevel(g);
            if (gameOver) {
                drawGameOver(g);
            }
        }
    }

    private void drawMenu(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString("Press Enter to Start the Game", 100, 200);
    }

    private void drawWalls(Graphics g) {
        g.setColor(Color.BLUE);
        for (int[] wall : wallLayouts[level % wallLayouts.length]) {
            g.fillRect(wall[0], wall[1], wall[2], wall[3]); // Draw all walls
        }
    }

    private void drawDots(Graphics g) {
        g.setColor(Color.WHITE);
        for (Point dot : dots) {
            g.fillOval(dot.x, dot.y, dotSize, dotSize);
        }
    }

    private void drawPacman(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillArc(pacmanX, pacmanY, pacmanSize, pacmanSize, 45, 270);
        g.setColor(Color.BLACK);
        g.fillOval(pacmanX + 10, pacmanY + 10, 5, 5); // Pac-Man's eye
    }

    private void drawGhosts(Graphics g) {
        for (Ghost ghost : ghosts) {
            ghost.draw(g);
        }
    }

    private void drawPowerUp(Graphics g) {
        if (powerUp != null) {
            g.setColor(Color.ORANGE);
            g.fillOval(powerUp.x, powerUp.y, 20, 20); // Draw power-up
            g.setColor(Color.BLACK);
            g.fillOval(powerUp.x + 7, powerUp.y + 7, 6, 6); // Black dot in the center
        }
    }

    private void drawScore(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
    }

    private void drawLevel(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString("Level: " + level, 10, 40);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString("Game Over! Press R to Restart", 100, 200);
    }

    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !inMenu) {
            for (Ghost ghost : ghosts) {
                ghost.move(pacmanX, pacmanY, wallLayouts[level % wallLayouts.length]);
            }
            checkCollision();
            checkDotCollision();
            checkPowerUpCollision();
            checkLevelProgression();
            updatePowerUp();
            spawnPowerUp();
            repaint();
        }
    }

    private void checkCollision() {
        Rectangle pacmanRect = new Rectangle(pacmanX, pacmanY, pacmanSize, pacmanSize);
        for (Ghost ghost : ghosts) {
            Rectangle ghostRect = new Rectangle(ghost.getX(), ghost.getY(), ghostSize, ghostSize);
            if (pacmanRect.intersects(ghostRect) && !powerUpActive) {
                gameOver = true;
                timer.stop();
            }
        }
    }

    private void checkDotCollision() {
        for (int i = 0; i < dots.size(); i++) {
            Point dot = dots.get(i);
            Rectangle dotRect = new Rectangle(dot.x, dot.y, dotSize, dotSize);
            Rectangle pacmanRect = new Rectangle(pacmanX, pacmanY, pacmanSize, pacmanSize);
            if (pacmanRect.intersects(dotRect)) {
                dots.remove(i);
                score += 10; // Increased points per dot
                break;
            }
        }
    }

    private void checkPowerUpCollision() {
        if (powerUp != null) {
            Rectangle powerUpRect = new Rectangle(powerUp.x, powerUp.y, 20, 20);
            Rectangle pacmanRect = new Rectangle(pacmanX, pacmanY, pacmanSize, pacmanSize);
            if (pacmanRect.intersects(powerUpRect)) {
                powerUpActive = true;
                ticksSincePowerUp = 0;
                powerUp = null;
            }
        }
    }

    private void checkLevelProgression() {
        if (score > 0 && score % 300 == 0) { // Level up every 300 points
            level++;
            if (level > 1000) {
                level = 1000; // Cap level at 1000
            }
            initGhosts(); // Spawn new ghosts for the new level
        }
    }

    private void spawnPowerUp() {
        long currentTime = System.currentTimeMillis();
        if (powerUp == null && (currentTime - lastPowerUpTime) > 60000) { // Spawn every minute
            int x = random.nextInt(10) * 30 + 50; // Random x coordinate
            int y = random.nextInt(10) * 30 + 50; // Random y coordinate
            powerUp = new Point(x, y);
            lastPowerUpTime = currentTime;
        }
    }

    private void updatePowerUp() {
        if (powerUpActive) {
            ticksSincePowerUp++;
            if (ticksSincePowerUp >= powerUpDuration) {
                powerUpActive = false;
            }
        }
    }

    private boolean collidesWithWall(int x, int y) {
        Rectangle rect = new Rectangle(x, y, pacmanSize, pacmanSize);
        for (int[] wall : wallLayouts[level % wallLayouts.length]) {
            Rectangle wallRect = new Rectangle(wall[0], wall[1], wall[2], wall[3]);
            if (rect.intersects(wallRect)) {
                return true; // Collision detected
            }
        }
        return false; // No collision
    }

    public void keyPressed(KeyEvent e) {
        if (gameOver && e.getKeyCode() == KeyEvent.VK_R) {
            restartGame();
        } else if (e.getKeyCode() == KeyEvent.VK_ENTER && inMenu) {
            inMenu = false;
            repaint();
        } else if (!gameOver && !inMenu) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W: movePacman(0, -speed); break;
                case KeyEvent.VK_S: movePacman(0, speed); break;
                case KeyEvent.VK_A: movePacman(-speed, 0); break;
                case KeyEvent.VK_D: movePacman(speed, 0); break;
            }
        }
    }

    private void restartGame() {
        initializeGame();
        timer.start();
    }

    private void movePacman(int deltaX, int deltaY) {
        int newX = pacmanX + deltaX;
        int newY = pacmanY + deltaY;

        // Allow Pac-Man to move through walls
        pacmanX = newX;
        pacmanY = newY;
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame frame = new JFrame("Pac-Man Game");
        PacManGame game = new PacManGame();
        frame.add(game);
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

class Ghost {
    private int x, y, size;
    private String type;
    private Color color;
    private int speed; // Keep speed as int for simplicity
    private int lastDirection;

    public Ghost(int x, int y, int size, String type, Color color) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.type = type;
        this.color = color;
        this.speed = 1; // Set ghost speed to 1
        this.lastDirection = -1; // No direction initially
    }

    public void move(int pacmanX, int pacmanY, int[][] walls) {
        // Store previous position for potential rollback
        int prevX = x;
        int prevY = y;

        if (type.equals("chaser")) {
            // Move towards Pac-Man
            if (x < pacmanX) {
                x += speed;
                lastDirection = 3; // Right
            } else if (x > pacmanX) {
                x -= speed;
                lastDirection = 2; // Left
            }

            if (y < pacmanY) {
                y += speed;
                lastDirection = 1; // Down
            } else if (y > pacmanY) {
                y -= speed;
                lastDirection = 0; // Up
            }
        } else if (type.equals("random")) {
            // Random movement logic
            int direction = (int) (Math.random() * 4); // 0: up, 1: down, 2: left, 3: right
            lastDirection = direction; // Store the last direction for potential use

            switch (direction) {
                case 0: y -= speed; break; // Up
                case 1: y += speed; break; // Down
                case 2: x -= speed; break; // Left
                case 3: x += speed; break; // Right
            }
        }

        // Prevent ghosts from going through walls
        for (int[] wall : walls) {
            if (x < wall[0] + wall[2] && x + size > wall[0] &&
                y < wall[1] + wall[3] && y + size > wall[1]) {
                // Collision detected, revert position
                x = prevX;
                y = prevY;
                break; // Exit the loop after the first collision
            }
        }
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillOval(x, y, size, size); // Draw ghost
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}
