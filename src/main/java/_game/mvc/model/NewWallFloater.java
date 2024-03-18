package _game.mvc.model;

import _game.mvc.controller.CommandCenter;
import _game.mvc.controller.Game;
import _game.mvc.controller.GameOp;
import _game.mvc.controller.Sound;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NewWallFloater extends Floater {

	private static final Color MAROON = new Color(186, 0, 22);
	//spawn every 40 seconds
	public static final int SPAWN_NEW_WALL_FLOATER = Game.FRAMES_PER_SECOND * 35;
	public NewWallFloater() {
		setColor(MAROON);
		setExpiry(230);
		List<Point> listPoints = new ArrayList<>();
		listPoints.add(new Point(4, 4));
		listPoints.add(new Point(4, -4));
		listPoints.add(new Point(-4, -4));
		listPoints.add(new Point(-4, 4));

		setCartesians(listPoints.toArray(new Point[0]));
	}

	@Override
	public void remove(LinkedList<Movable> list) {
		super.remove(list);
		//if getExpiry() > 0, then this remove was the result of a collision, rather than natural mortality
		if (getExpiry() > 0) {
			Sound.playSound("insect.wav");
			buildWall();
		}
	}

	private void buildWall() {
		// build wall in boundary
		final int BRICK_SIZE = Game.DIM.width / 30;
		final int ROWS = Game.DIM.height / BRICK_SIZE;
		final int COLS = Game.DIM.width / BRICK_SIZE;

		for (int i = 0; i < COLS; i++) {
			int xPosition = i * BRICK_SIZE;
			// add upper boundary using red brick
			CommandCenter.getInstance().getOpsQueue().enqueue(
					new Brick(new Point(xPosition, 0), BRICK_SIZE),
					GameOp.Action.ADD);
			// add down boundary
			CommandCenter.getInstance().getOpsQueue().enqueue(
					new Brick(new Point(xPosition, Game.DIM.height - 2*BRICK_SIZE), BRICK_SIZE),
					GameOp.Action.ADD);
		}

		for (int j = 1; j < ROWS - 1; j++) {
			int yPosition = j * BRICK_SIZE;
			// add left boundary
			CommandCenter.getInstance().getOpsQueue().enqueue(
					new Brick(new Point(0, yPosition), BRICK_SIZE),
					GameOp.Action.ADD);
			// add right boundary
			CommandCenter.getInstance().getOpsQueue().enqueue(
					new Brick(new Point(Game.DIM.width - BRICK_SIZE, yPosition), BRICK_SIZE),
					GameOp.Action.ADD);
		}
	}
}
