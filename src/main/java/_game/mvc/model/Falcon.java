package _game.mvc.model;

import _game.mvc.controller.CommandCenter;
import _game.mvc.controller.Game;
import _game.mvc.controller.Sound;
import lombok.Data;

import java.awt.*;
import java.util.*;
import java.util.List;

@Data
public class Falcon extends Sprite {

	// ==============================================================
	// FIELDS 
	// ==============================================================

	//static fields

	//number of degrees the falcon will turn at each animation cycle if the turnState is LEFT or RIGHT
	public final static int TURN_STEP = 7;
	//number of frames that the falcon will be protected after a spawn
	public static final int INITIAL_SPAWN_TIME = 46;
	//number of frames falcon will be protected after consuming a NewShieldFloater
	public static final int MAX_SHIELD = 200;
	public static final int MAX_NUKE = 600;

	public static final int MIN_RADIUS = 28;


	//instance fields (getters/setters provided by Lombok @Data above)
	private int shield;

	private int nukeMeter;
	private int invisible;
	private boolean maxSpeedAttained;

	//showLevel is not germane to the Falcon. Rather, it controls whether the level is shown in the middle of the
	// screen. However, given that the Falcon reference is never null, and that a Falcon is a Movable whose move/draw
	// methods are being called every ~40ms, this is a very convenient place to store this variable.
	private int showLevel;
	private boolean thrusting;
	//enum used for turnState field
	public enum TurnState {IDLE, LEFT, RIGHT}
	private TurnState turnState = TurnState.IDLE;


	// ==============================================================
	// CONSTRUCTOR
	// ==============================================================
	
	public Falcon() {

		setTeam(Team.FRIEND);
		setRadius(MIN_RADIUS);

		//cartesian points which define the shape of Falcon with Robert Alef's design
		List<Point> listShip = new ArrayList<>();
		listShip.add(new Point(0,9));
		listShip.add(new Point(-1, 6));
		listShip.add(new Point(-1,3));
		listShip.add(new Point(-4, 1));
		listShip.add(new Point(4,1));
		listShip.add(new Point(-4,1));
		listShip.add(new Point(-4, -2));
		listShip.add(new Point(-1, -2));
		listShip.add(new Point(-1, -9));
		listShip.add(new Point(-1, -2));
		listShip.add(new Point(-4, -2));
		listShip.add(new Point(-10, -8));
		listShip.add(new Point(-5, -9));
		listShip.add(new Point(-7, -11));
		listShip.add(new Point(-4, -11));
		listShip.add(new Point(-2, -9));
		listShip.add(new Point(-2, -10));
		listShip.add(new Point(-1, -10));
		listShip.add(new Point(-1, -9));
		listShip.add(new Point(1, -9));
		listShip.add(new Point(1, -10));
		listShip.add(new Point(2, -10));
		listShip.add(new Point(2, -9));
		listShip.add(new Point(4, -11));
		listShip.add(new Point(7, -11));
		listShip.add(new Point(5, -9));
		listShip.add(new Point(10, -8));
		listShip.add(new Point(4, -2));
		listShip.add(new Point(1, -2));
		listShip.add(new Point(1, -9));
		listShip.add(new Point(1, -2));
		listShip.add(new Point(4,-2));
		listShip.add(new Point(4, 1));
		listShip.add(new Point(1, 3));
		listShip.add(new Point(1,6));
		listShip.add(new Point(0,9));
		setCartesians(listShip.toArray(new Point[0]));
	}


	// ==============================================================
	// METHODS 
	// ==============================================================
	@Override
	public void move() {
		//avoid exceed bounds
		if (getCenter().x + getRadius() > Game.DIM.width) {
			setCenter(new Point(Game.DIM.width - getRadius(), getCenter().y));
			setOrientation(getOrientation());
			//decrementFalconNumAndSpawn();
		} else if (getCenter().x - getRadius() < 0) {
			setCenter(new Point(getRadius(), getCenter().y));
			setOrientation(getOrientation());
			//decrementFalconNumAndSpawn();
		} else if (getCenter().y + getRadius() > Game.DIM.height) {
			setCenter(new Point(getCenter().x, Game.DIM.height - getRadius()));
			setOrientation(getOrientation());
			//decrementFalconNumAndSpawn();
		} else if (getCenter().y - getRadius() < 0) {
			setCenter(new Point(getCenter().x, getRadius()));
			setOrientation(getOrientation());
			//decrementFalconNumAndSpawn();
		} else {
			double newXPos = getCenter().x + getDeltaX();
			double newYPos = getCenter().y + getDeltaY();
			setCenter(new Point((int) newXPos, (int) newYPos));
		}


		if (invisible > 0) invisible--;
		if (shield > 0) shield--;
		if (nukeMeter > 0) nukeMeter--;
		//The falcon is a convenient place to decrement the showLevel variable as the falcon
		//move() method is being called every frame (~40ms); and the falcon reference is never null.
		if (showLevel > 0) showLevel--;

		final double THRUST = 0.85;
		final int MAX_VELOCITY = 40;


		//apply some thrust vectors using trig.
		if (thrusting) {
			double vectorX = Math.cos(Math.toRadians(getOrientation()))
					* THRUST;
			double vectorY = Math.sin(Math.toRadians(getOrientation()))
					* THRUST;

			//Absolute velocity is the hypotenuse of deltaX and deltaY
			int absVelocity =
					(int) Math.sqrt(Math.pow(getDeltaX()+ vectorX, 2) + Math.pow(getDeltaY() + vectorY, 2));

			//only accelerate (or adjust radius) if we are below the maximum absVelocity.
			if (absVelocity < MAX_VELOCITY){
				//accelerate
				setDeltaX(getDeltaX() + vectorX);
				setDeltaY(getDeltaY() + vectorY);
				//Make the ship radius bigger when the absolute velocity increases, thereby increasing difficulty when not
				// protected, and allowing player to use the shield offensively when protected.
				setRadius(MIN_RADIUS + absVelocity / 3);
				maxSpeedAttained = false;
			} else {
				//at max speed, you will lose steerage if you attempt to accelerate in the same general direction
				//show WARNING message to player using this flag (see drawFalconStatus() of GamePanel class)
				maxSpeedAttained = true;
			}

		}

		//adjust the orientation given turnState
		int adjustOr = getOrientation();
		switch (turnState){
			case LEFT:
				adjustOr = getOrientation() <= 0 ? 360 - TURN_STEP : getOrientation() - TURN_STEP;
				break;
			case RIGHT:
				adjustOr = getOrientation() >= 360 ? TURN_STEP : getOrientation() + TURN_STEP;
				break;
			case IDLE:
			default:
				//do nothing
		}
		setOrientation(adjustOr);
	}

	//Since the superclass Spite does not provide an
	// implementation for draw() (contract method from Movable) ,we inherit that contract debt, and therefore must
	// provide an implementation. This is a raster and vector (see drawShield below) implementation of draw().
	@Override
	public void draw(Graphics g) {
		if (!(invisible > 0)) {
			if (shield > 0) { //protected
				if (thrusting) {
					setColor(Color.ORANGE);
				} else {
					setColor(Color.GREEN);
				}
				drawShield(g);
			} else { //not protected
				if (thrusting) {
					setColor(Color.ORANGE);
				} else {
					setColor(Color.WHITE);
				}
			}
		}
		renderVector(g);
	}

	private void drawShield(Graphics g){
		g.setColor(Color.GREEN);
		g.drawOval(getCenter().x - getRadius(), getCenter().y - getRadius(), getRadius() *2, getRadius() *2);
	}

	@Override
	public void remove(LinkedList<Movable> list) {
		//The falcon is never actually removed from the game-space; instead we decrement numFalcons
		//only execute the decrementFalconNumAndSpawn() method if shield is down.
		if ( shield == 0)  decrementFalconNumAndSpawn();
	}


	public void decrementFalconNumAndSpawn(){

		CommandCenter.getInstance().setNumFalcons(CommandCenter.getInstance().getNumFalcons() -1);
		if (CommandCenter.getInstance().isGameOver()) return;
		Sound.playSound("shipspawn.wav");
		setShield(Falcon.INITIAL_SPAWN_TIME);
		setInvisible(Falcon.INITIAL_SPAWN_TIME/4);
		//put falcon in the middle of the game-space
		setCenter(new Point(Game.DIM.width / 2, Game.DIM.height / 2));
		//random number between 0-360 in steps of TURN_STEP
		setOrientation(270);
		setDeltaX(0);
		setDeltaY(0);
		setRadius(Falcon.MIN_RADIUS);
		setMaxSpeedAttained(false);
		setNukeMeter(0);
	}
}
