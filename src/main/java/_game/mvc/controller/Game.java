package _game.mvc.controller;

import _game.mvc.model.*;
import _game.mvc.view.GamePanel;

import javax.sound.sampled.Clip;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Random;

// ===============================================
// == This Game class is the CONTROLLER
// ===============================================

public class Game implements Runnable, KeyListener {

    // ===============================================
    // FIELDS
    // ===============================================

    public static final Dimension DIM = new Dimension(900, 750); //the dimension of the game.
    private final GamePanel gamePanel;
    //this is used throughout many classes.
    public static final Random R = new Random();

    public final static int ANIMATION_DELAY = 40; // milliseconds between frames

    public final static int FRAMES_PER_SECOND = 1000 / ANIMATION_DELAY;

    private final Thread animationThread;


    //key-codes
    private static final int
            PAUSE = 80, // p key
            QUIT = 81, // q key
            LEFT = 37, // rotate left; left arrow
            RIGHT = 39, // rotate right; right arrow
            UP = 38, // thrust; up arrow
            START = 83, // s key
            FIRE = 32, // space key
            MUTE = 77, // m-key mute
            NUKE = 78; // n-key mute
    private final Clip soundThrust;
    private final Clip soundBackground;

    // ===============================================
    // ==CONSTRUCTOR
    // ===============================================

    public Game() {

        gamePanel = new GamePanel(DIM);
        gamePanel.addKeyListener(this); //Game object implements KeyListener
        soundThrust = Sound.clipForLoopFactory("whitenoise.wav");
        soundBackground = Sound.clipForLoopFactory("music-background.wav");

        //fire up the animation thread
        animationThread = new Thread(this); // pass the animation thread a runnable object, the Game object
        animationThread.setDaemon(true);
        animationThread.start();
    }

    // ===============================================
    // ==METHODS
    // ===============================================

    public static void main(String[] args) {
        //typical Swing application start; we pass EventQueue a Runnable object.
        EventQueue.invokeLater(Game::new);
    }

    // Game implements runnable, and must have run method
    @Override
    public void run() {

        // lower animation thread's priority, thereby yielding to the 'Event Dispatch Thread' or EDT
        // thread which listens to keystrokes
        animationThread.setPriority(Thread.MIN_PRIORITY);

        // and get the current time
        long startTime = System.currentTimeMillis();

        // this thread animates the scene
        while (Thread.currentThread() == animationThread) {


            //this call will cause all movables to move() and draw() themselves every ~40ms
            // see GamePanel class for details
            gamePanel.update(gamePanel.getGraphics());

            checkCollisions();
            checkNewLevel();
            checkFloaters();
            //have enemy planes to attack falcon
            spawnEnemyPlane();
            spawnEnemyBullet();

            //this method will execute add() and remove() callbacks on Movable objects
            processGameOpsQueue();
            //keep track of the frame for development purposes
            CommandCenter.getInstance().incrementFrame();

            // surround the sleep() in a try/catch block
            // this simply controls delay time between
            // the frames of the animation
            try {
                // The total amount of time is guaranteed to be at least ANIMATION_DELAY long.  If processing (update)
                // between frames takes longer than ANIMATION_DELAY, then the difference between startTime -
                // System.currentTimeMillis() will be negative, then zero will be the sleep time
                startTime += ANIMATION_DELAY;

                Thread.sleep(Math.max(0,
                        startTime - System.currentTimeMillis()));
            } catch (InterruptedException e) {
                // do nothing (bury the exception), and just continue, e.g. skip this frame -- no big deal
            }
        }
    }

    private void checkFloaters() {
        spawnNewWallFloater();
        spawnShieldFloater();
        spawnNukeFloater();
    }

    private void checkCollisions() {

        //This has order-of-growth of O(FOES * FRIENDS)
        Point pntFriendCenter, pntFoeCenter;
        int radFriend, radFoe;
        for (Movable movFriend : CommandCenter.getInstance().getMovFriends()) {
            for (Movable movFoe : CommandCenter.getInstance().getMovFoes()) {

                pntFriendCenter = movFriend.getCenter();
                pntFoeCenter = movFoe.getCenter();
                radFriend = movFriend.getRadius();
                radFoe = movFoe.getRadius();

                //detect collision
                if (pntFriendCenter.distance(pntFoeCenter) < (radFriend + radFoe)) {
                    CommandCenter.getInstance().getOpsQueue().enqueue(movFriend, GameOp.Action.REMOVE);
                    // case when player fires bullet hitting enemy plane or use nuke
                    if ((movFriend instanceof Bullet && movFoe instanceof EnemyPlane) ||
                            (movFriend instanceof Nuke && movFoe instanceof EnemyPlane)) {
                        EnemyPlane enemyPlane = (EnemyPlane) movFoe;
                        //give the user some points for destroying the asteroid
                        CommandCenter.getInstance().setScore(CommandCenter.getInstance().getScore() + radFoe);
                        //enqueue the foe
                        CommandCenter.getInstance().getOpsQueue().enqueue(movFoe, GameOp.Action.REMOVE);
                        CommandCenter.getInstance().getOpsQueue().enqueue(new WhiteCloudDebris(enemyPlane),
                                GameOp.Action.ADD);
                    // case when enemy bullet hits falcon or break wall
                    } else if ((movFriend instanceof Falcon && movFoe instanceof EnemyBullet) ||
                            (movFriend instanceof Bullet && movFoe instanceof Brick)) {
                        CommandCenter.getInstance().getOpsQueue().enqueue(movFoe, GameOp.Action.REMOVE);
                    // case when falcon hit enemy, this will cause the number of falcon reduced
                    } else if (movFriend instanceof Falcon && movFoe instanceof EnemyPlane) {
                        // enqueue the friend
                        CommandCenter.getInstance().getOpsQueue().enqueue(movFriend, GameOp.Action.REMOVE);
                    }
                }
            }
        }

        //check for collisions between falcon and floaters. Order of growth of O(FLOATERS)
        Point pntFalCenter = CommandCenter.getInstance().getFalcon().getCenter();
        int radFalcon = CommandCenter.getInstance().getFalcon().getRadius();

        Point pntFloaterCenter;
        int radFloater;
        for (Movable movFloater : CommandCenter.getInstance().getMovFloaters()) {
            pntFloaterCenter = movFloater.getCenter();
            radFloater = movFloater.getRadius();
            //detect collision
            if (pntFalCenter.distance(pntFloaterCenter) < (radFalcon + radFloater)) {
                //enqueue the floater
                CommandCenter.getInstance().getOpsQueue().enqueue(movFloater, GameOp.Action.REMOVE);
            }
        }

    }


    //This method adds and removes movables to/from their respective linked-lists.
    private void processGameOpsQueue() {

        //deferred mutation: these operations are done AFTER we have completed our collision detection to avoid
        // mutating the movable linkedlists while iterating them above.
        while (!CommandCenter.getInstance().getOpsQueue().isEmpty()) {

            GameOp gameOp = CommandCenter.getInstance().getOpsQueue().dequeue();

            //given team, determine which linked-list this object will be added-to or removed-from
            LinkedList<Movable> list;
            Movable mov = gameOp.getMovable();
            switch (mov.getTeam()) {
                case FOE:
                    list = CommandCenter.getInstance().getMovFoes();
                    break;
                case FRIEND:
                    list = CommandCenter.getInstance().getMovFriends();
                    break;
                case FLOATER:
                    list = CommandCenter.getInstance().getMovFloaters();
                    break;
                case DEBRIS:
                default:
                    list = CommandCenter.getInstance().getMovDebris();
            }

            //pass the appropriate linked-list from above
            //this block will execute the add() or remove() callbacks in the Movable models.
            GameOp.Action action = gameOp.getAction();
            if (action == GameOp.Action.ADD)
                mov.add(list);
            else //REMOVE
                mov.remove(list);

        }//end while
    }



    private void spawnNewWallFloater() {

        if (CommandCenter.getInstance().getFrame() % NewWallFloater.SPAWN_NEW_WALL_FLOATER == 0 && isBrickFree()) {
            CommandCenter.getInstance().getOpsQueue().enqueue(new NewWallFloater(), GameOp.Action.ADD);
        }
    }

    private void spawnShieldFloater() {

        if (CommandCenter.getInstance().getFrame() % ShieldFloater.SPAWN_SHIELD_FLOATER == 0) {
            CommandCenter.getInstance().getOpsQueue().enqueue(new ShieldFloater(), GameOp.Action.ADD);
        }
    }

    private void spawnNukeFloater() {

        if (CommandCenter.getInstance().getFrame() % NukeFloater.SPAWN_NUKE_FLOATER == 0) {
            CommandCenter.getInstance().getOpsQueue().enqueue(new NukeFloater(), GameOp.Action.ADD);
        }
    }

    private void spawnEnemyPlane() {

        if (CommandCenter.getInstance().getFrame() % EnemyPlane.SPAWN_Enemy_Plane == 0) {
            int level = CommandCenter.getInstance().getLevel();
            Falcon fal = CommandCenter.getInstance().getFalcon();
            EnemyPlane enemyPlane = new EnemyPlane(level, fal);
            CommandCenter.getInstance().getOpsQueue().enqueue(enemyPlane, GameOp.Action.ADD);
        }
    }

    private void spawnEnemyBullet() {
        for (Movable movFoe : CommandCenter.getInstance().getMovFoes()) {
            if (movFoe instanceof EnemyPlane) {
                EnemyPlane enemyPlane = (EnemyPlane) movFoe;
                int x = enemyPlane.getCenter().x;
                int y = enemyPlane.getCenter().y + enemyPlane.getRadius();
                // fire bullets
                if (enemyPlane.getPlaneIndex() == 1) {
                    CommandCenter.getInstance().getOpsQueue().enqueue(new EnemyBullet(x, y), GameOp.Action.ADD);
                } else if (enemyPlane.getPlaneIndex() == 2) { // spawn more bullets
                    CommandCenter.getInstance().getOpsQueue().enqueue(new EnemyBullet(x-8, y), GameOp.Action.ADD);
                    CommandCenter.getInstance().getOpsQueue().enqueue(new EnemyBullet(x+8, y), GameOp.Action.ADD);
                    CommandCenter.getInstance().getOpsQueue().enqueue(new EnemyBullet(x, y+3), GameOp.Action.ADD);
                }
            }
        }
    }

    private boolean isBrickFree() {
        //if there are no more Bricks on the screen
        boolean brickFree = true;
        for (Movable movFoe : CommandCenter.getInstance().getMovFoes()) {
            if (movFoe instanceof Brick) {
                brickFree = false;
                break;
            }
        }
        return brickFree;
    }

    private boolean isLevelClear() {
        //if player gets enough scores, then get next level
        long currentScore = CommandCenter.getInstance().getScore();
        int currentLevel = CommandCenter.getInstance().getLevel();
        return currentScore > 0 && currentScore >= 100L * (currentLevel + 1);
    }

    private void checkNewLevel() {
        if (isLevelClear()) {
            //currentLevel will be zero at beginning of game
            int level = CommandCenter.getInstance().getLevel();
            CommandCenter.getInstance().setScore(CommandCenter.getInstance().getScore());
            //bump the level up
            level = level + 1;
            CommandCenter.getInstance().setLevel(level);
            //make falcon invincible momentarily in case new asteroids spawn on top of him, and give player
            //time to adjust to new asteroids in game space.
            if (CommandCenter.getInstance().getFalcon().getShield() < Falcon.INITIAL_SPAWN_TIME)
                CommandCenter.getInstance().getFalcon().setShield(Falcon.INITIAL_SPAWN_TIME);
            //show "Level X" in middle of screen
            CommandCenter.getInstance().getFalcon().setShowLevel(Falcon.INITIAL_SPAWN_TIME);

        }
    }


    // Varargs for stopping looping-music-clips
    private static void stopLoopingSounds(Clip... clpClips) {
        Arrays.stream(clpClips).forEach(clip -> clip.stop());
    }

    // ===============================================
    // KEYLISTENER METHODS
    // ===============================================

    @Override
    public void keyPressed(KeyEvent e) {
        Falcon falcon = CommandCenter.getInstance().getFalcon();
        int keyCode = e.getKeyCode();

        if (keyCode == START && CommandCenter.getInstance().isGameOver()) {
            CommandCenter.getInstance().initGame();
            return;
        }


        switch (keyCode) {
            case PAUSE:
                CommandCenter.getInstance().setPaused(!CommandCenter.getInstance().isPaused());
                if (CommandCenter.getInstance().isPaused()) stopLoopingSounds(soundBackground, soundThrust);
                break;
            case QUIT:
                System.exit(0);
                break;
            case UP:
                falcon.setThrusting(true);
                soundThrust.loop(Clip.LOOP_CONTINUOUSLY);
                break;
            case LEFT:
                falcon.setTurnState(Falcon.TurnState.LEFT);
                break;
            case RIGHT:
                falcon.setTurnState(Falcon.TurnState.RIGHT);
                break;

            default:
                break;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {
        Falcon falcon = CommandCenter.getInstance().getFalcon();
        int keyCode = e.getKeyCode();
        //show the key-code in the console
        System.out.println(keyCode);

        switch (keyCode) {
            case FIRE:
                CommandCenter.getInstance().getOpsQueue().enqueue(new Bullet(falcon), GameOp.Action.ADD);
                break;
            case NUKE:
                CommandCenter.getInstance().getOpsQueue().enqueue(new Nuke(falcon), GameOp.Action.ADD);
                break;
            //releasing either the LEFT or RIGHT arrow key will set the TurnState to IDLE
            case LEFT:
            case RIGHT:
                falcon.setTurnState(Falcon.TurnState.IDLE);
                break;
            case UP:
                falcon.setThrusting(false);
                soundThrust.stop();
                break;

            case MUTE:
                CommandCenter.getInstance().setMuted(!CommandCenter.getInstance().isMuted());

                if (!CommandCenter.getInstance().isMuted()) {
                    stopLoopingSounds(soundBackground);
                } else {
                    soundBackground.loop(Clip.LOOP_CONTINUOUSLY);
                }
                break;

            default:
                break;
        }

    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

}


