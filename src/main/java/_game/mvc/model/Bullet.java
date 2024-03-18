package _game.mvc.model;

import _game.mvc.controller.CommandCenter;
import _game.mvc.controller.Game;
import _game.mvc.controller.GameOp;
import _game.mvc.controller.Sound;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Bullet extends Sprite {

    public Bullet(Falcon falcon) {

        setTeam(Team.FRIEND);
        setColor(Color.WHITE);

        //a bullet expires after 45 frames.
        setExpiry(45);
        setRadius(12);

        //everything is relative to the falcon ship that fired the bullet
        setCenter(falcon.getCenter());

        //set the bullet orientation to the falcon (ship) orientation
        setOrientation(falcon.getOrientation());

        final double FIRE_POWER = 35.0;
        double vectorX =
                Math.cos(Math.toRadians(getOrientation())) * FIRE_POWER;
        double vectorY =
                Math.sin(Math.toRadians(getOrientation())) * FIRE_POWER;

        //fire force: falcon inertia + fire-vector
        setDeltaX(falcon.getDeltaX() + vectorX);
        setDeltaY(falcon.getDeltaY() + vectorY);

        //we have a reference to the falcon passed into the constructor. Let's create some kick-back.
        //fire kick-back on the falcon: inertia - fire-vector / some arbitrary divisor
        final double KICK_BACK_DIVISOR = 36.0;
        falcon.setDeltaX(falcon.getDeltaX() - vectorX / KICK_BACK_DIVISOR);
        falcon.setDeltaY(falcon.getDeltaY() - vectorY / KICK_BACK_DIVISOR);

        //define the points on a cartesian grid
        List<Point> listPoints = new ArrayList<>();
        listPoints.add(new Point(0, 3)); //top point
        listPoints.add(new Point(1, -1)); //right bottom
        listPoints.add(new Point(0, 0));
        listPoints.add(new Point(-1, -1)); //left bottom

        setCartesians(listPoints.toArray(new Point[0]));
    }

    @Override
    public void move() {
        if (getCenter().x > Game.DIM.width) {
            setExpiry(2);
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else if (getCenter().x < 0) {
            setExpiry(2);
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else if (getCenter().y > Game.DIM.height) {
            setExpiry(2);
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else if (getCenter().y < 0) {
            setExpiry(2);
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else {
            double newXPos = getCenter().x + getDeltaX();
            double newYPos = getCenter().y + getDeltaY();
            setCenter(new Point((int) newXPos, (int) newYPos));
        }
    }


    @Override
    public void draw(Graphics g) {
        renderVector(g);
    }

    @Override
    public void add(LinkedList<Movable> list) {
        super.add(list);
        Sound.playSound("thump.wav");
    }
}
