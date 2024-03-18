package _game.mvc.model;

import _game.mvc.controller.CommandCenter;
import _game.mvc.controller.Game;
import _game.mvc.controller.Sound;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ShieldFloater extends Floater {
	//spawn every 25 seconds
	public static final int SPAWN_SHIELD_FLOATER = Game.FRAMES_PER_SECOND * 20;
	public ShieldFloater() {
		setColor(Color.GREEN);
		setExpiry(260);

		List<Point> listPoints = new ArrayList<>();
		listPoints.add(new Point(0, 6));
		listPoints.add(new Point(2, 5));
		listPoints.add(new Point(4, 6));
		listPoints.add(new Point(4,-3));
		listPoints.add(new Point(0, -6));
		listPoints.add(new Point(-4,-3));
		listPoints.add(new Point(-4, 6));
		listPoints.add(new Point(-2,5));

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
			Sound.playSound("shieldup.wav");
		    CommandCenter.getInstance().getFalcon().setShield(Falcon.MAX_SHIELD);
	   }
	}


}
