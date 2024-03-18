package _game.mvc.model;

import _game.mvc.controller.CommandCenter;
import _game.mvc.controller.Game;
import _game.mvc.controller.GameOp;
import _game.mvc.controller.Utils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class EnemyBullet extends Sprite {

    public EnemyBullet(int x, int y) {

        setTeam(Team.FOE);
        setColor(Color.RED);
        setExpiry(10);
        setRadius(5);

        List<Point> listPoints = new ArrayList<>();
        listPoints.add(new Point(0, 3));
        listPoints.add(new Point(2, 0));
        listPoints.add(new Point(0, -3));
        listPoints.add(new Point(-2, 0));
        setCartesians(listPoints.toArray(new Point[0]));

        setCenter(new Point(x, y));
        setOrientation(90);
        setDeltaX(0);
        setDeltaY(45);
    }

    @Override
    public void move() {
        if (getCenter().x > Game.DIM.width) {
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else if (getCenter().x < 0) {
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else if (getCenter().y > Game.DIM.height) {
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else if (getCenter().y < 0) {
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else {
            double newXPos = getCenter().x + getDeltaX();
            double newYPos = getCenter().y + getDeltaY();
            setCenter(new Point((int) newXPos, (int) newYPos));
        }
        if (getExpiry() == 0) {
            CommandCenter.getInstance().getOpsQueue().enqueue(this, GameOp.Action.REMOVE);
        } else {
            setExpiry(getExpiry() - 1);
        }
    }

    @Override
    public void draw(Graphics g) {
        renderVector(g);

        List<PolarPoint> polars = Utils.cartesianToPolar(getCartesians());
        Function<PolarPoint, PolarPoint> rotatePolarByOrientation =
                pp -> new PolarPoint(
                        pp.getR(),
                        pp.getTheta() + Math.toRadians(getOrientation()) //rotated Theta
                );
        Function<PolarPoint, Point> polarToCartesian =
                pp -> new Point(
                        (int)  (pp.getR() * getRadius() * Math.sin(pp.getTheta())),
                        (int)  (pp.getR() * getRadius() * Math.cos(pp.getTheta())));
        Function<Point, Point> adjustForLocation =
                p -> new Point(
                        getCenter().x + p.x,
                        getCenter().y - p.y);

        g.fillPolygon(
                polars.stream()
                        .map(rotatePolarByOrientation)
                        .map(polarToCartesian)
                        .map(adjustForLocation)
                        .map(pnt -> pnt.x)
                        .mapToInt(Integer::intValue)
                        .toArray(),
                polars.stream()
                        .map(rotatePolarByOrientation)
                        .map(polarToCartesian)
                        .map(adjustForLocation)
                        .map(pnt -> pnt.y)
                        .mapToInt(Integer::intValue)
                        .toArray(),
                polars.size());
    }
}
