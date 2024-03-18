package _game.mvc.model;

import _game.mvc.controller.CommandCenter;
import _game.mvc.controller.Game;
import _game.mvc.controller.GameOp;
import _game.mvc.controller.Sound;
import lombok.Data;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

@Data
public class EnemyPlane extends Sprite {

    public static final int SPAWN_Enemy_Plane = Game.FRAMES_PER_SECOND * 2;

    private int life;
    private int planeIndex;
    private Falcon fal;

    public EnemyPlane (int level, Falcon fal) {
        this.fal = fal;
        setCenter(new Point(Game.R.nextInt(Game.DIM.width), 0));

        Map<Integer, Integer> sizeMap = new HashMap<>();
        sizeMap.put(0, 30); // low level
        sizeMap.put(1, 40); // medium level
        sizeMap.put(2, 50); // high level
        // generate plane by given level
        if (level <= 1) {
            setRadius(sizeMap.get(0) + Game.R.nextInt(5));
            setExpiry(200);
            planeIndex = Game.R.nextInt(2);
        } else if (level < 4) {
            setRadius(sizeMap.get(1) + Game.R.nextInt(5));
            setExpiry(250);
            planeIndex = Game.R.nextInt(3);
        } else {
            setRadius(sizeMap.get(2) + Game.R.nextInt(10));
            setExpiry(300);
            planeIndex = Game.R.nextInt(4);
        }
        setOrientation(0);
        setTeam(Team.FOE);

        Map<Integer, BufferedImage> rasterMap = new HashMap<>();
        rasterMap.put(0, loadGraphic("/imgs/enemy/white_plane.png") ); // normal plane
        rasterMap.put(1, loadGraphic("/imgs/enemy/green_plane.png") ); // fire bullets
        rasterMap.put(2, loadGraphic("/imgs/enemy/blue_plane.png") ); // fire more bullets
        rasterMap.put(3, loadGraphic("/imgs/enemy/red_plane.png") ); // move towards falcon, more dangerous
        setRasterMap(rasterMap);
    }
    public void move() {
        super.move();
        if (getExpiry() == 0) {
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else {
            setExpiry(getExpiry() - 1);
            if (planeIndex < 3) {
                // simply move downwards in the game
                setDeltaX(0);
                setDeltaY(5);
            } else if (planeIndex == 3) { // the most dangerous red plane
                // get location of falcon and move towards to it
                int deltaX = fal.getCenter().x - this.getCenter().x;
                int deltaY = fal.getCenter().y - this.getCenter().y;
                double rad = Math.atan2(deltaY, deltaX);
                setDeltaX(Math.cos(rad) * 5);
                setDeltaY(Math.sin(rad) * 5);
            }
        }
    }

    @Override
    public void draw(Graphics g) {
        renderRaster((Graphics2D) g, getRasterMap().get(this.planeIndex));
    }

    @Override
    public void remove(LinkedList<Movable> list) {
        super.remove(list);
        Sound.playSound("kapow.wav");
    }
}
