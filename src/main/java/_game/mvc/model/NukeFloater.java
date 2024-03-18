package _game.mvc.model;

import _game.mvc.controller.CommandCenter;
import _game.mvc.controller.Game;
import _game.mvc.controller.Sound;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NukeFloater extends Floater {

	public static final int SPAWN_NUKE_FLOATER = Game.FRAMES_PER_SECOND * 20;
	public NukeFloater() {
		setColor(Color.ORANGE);
		setExpiry(120);

		List<Point> listPoints = new ArrayList<>();
		listPoints.add(new Point(0, 5));
		listPoints.add(new Point(4, 0));
		listPoints.add(new Point(4, -2));
		listPoints.add(new Point(7, -4));
		listPoints.add(new Point(7, -6));
		listPoints.add(new Point(4,-6));
		listPoints.add(new Point(4,-2));
		listPoints.add(new Point(4,-6));
		listPoints.add(new Point(0,-6));
		listPoints.add(new Point(-4,-6));
		listPoints.add(new Point(-7,-6));
		listPoints.add(new Point(-7,-4));
		listPoints.add(new Point(-4,-2));
		listPoints.add(new Point(-4,-6));
		listPoints.add(new Point(-4,-2));
		listPoints.add(new Point(-4,0));

		setCartesians(listPoints.toArray(new Point[0]));
	}

	@Override
	public void draw(Graphics g) {
		super.renderVector(g);
	}

	@Override
	public void remove(LinkedList<Movable> list) {
		super.remove(list);
		//if getExpiry() > 0, then this remove was the result of a collision, rather than natural mortality
		if (getExpiry() > 0) {
			Sound.playSound("nuke-up.wav");
			CommandCenter.getInstance().getFalcon().setNukeMeter(Falcon.MAX_NUKE);
		}

	}
}
