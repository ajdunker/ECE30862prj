package com.brackeen.javagamebook.tilegame;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.sampled.AudioFormat;

import com.brackeen.javagamebook.graphics.*;
import com.brackeen.javagamebook.sound.*;
import com.brackeen.javagamebook.input.*;
import com.brackeen.javagamebook.test.GameCore;
import com.brackeen.javagamebook.tilegame.sprites.*;

import java.io.File;

//added for input
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
    GameManager manages all parts of the game.
*/
public class GameManager extends GameCore {

    public static void main(String[] args) {
        new GameManager().run();
    }

    // uncompressed, 44100Hz, 16-bit, mono, signed, little-endian
    private static final AudioFormat PLAYBACK_FORMAT =
        new AudioFormat(44100, 16, 1, true, false);

    private static final int DRUM_TRACK = 1;

    public static final float GRAVITY = 0.002f;
    public static String maptxt;

    private Point pointCache = new Point();
    private TileMap map;
    private MidiPlayer midiPlayer;
    private SoundManager soundManager;
    private ResourceManager resourceManager;
    private Sound prizeSound;
    private Sound boopSound;
    private Sound shootSound;
    private Sound dundunSound;
    private Sound healthSound;
    private Sound expSound;
    private Sound gasSound;
    private InputManager inputManager;
    private TileMapRenderer renderer;

    private GameAction moveLeft;
    private GameAction moveRight;
    private GameAction shoot;
    private GameAction moveUp;
    private GameAction moveDown;
    
    private GameAction jump;
    private GameAction exit;
    
    private int shotspeed = 500; //milliseconds between successive shots
    private int shotcount = 0; //shot count goes up to 10
    private long lastshot = 0; //time of last shot
    private long waittime = 0; //time at which 10th shot was hit
    private int cooldown = 0; //cooldown flag
    
    private int playerdir = 1; // -1 is left, 1 is right
    private float lastX = 0; //for tracking player movement with health
    private float movedX = 0; //for tracking how long the player has moved
    private long time0 = 0; //how long player has been motionless

    private float X = 0; //local copy of current creature X position
    private float Y = 0; //local copy of current creature Y position
    private int needabullet = 0; // need to create a bullet sprite for the current creature
    
    public void init() {
    	//take map input
    	BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
        	System.out.println("Enter String (must be .txt format):");
            try {
    			maptxt = br.readLine();
    			File f = new File("maps/" + maptxt);
    			if(f.exists()) {
    				break;
    			}
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			//e.printStackTrace();
    		}
        }

    	
        super.init();

        // set up input manager
        initInput();

        // start resource manager
        resourceManager = new ResourceManager(
        screen.getFullScreenWindow().getGraphicsConfiguration());

        // load resources
        renderer = new TileMapRenderer();
        renderer.setBackground(
            resourceManager.loadImage("background.png"));
        renderer.setScoreKeep(
        	resourceManager.loadImage("score.png"));

        // load first map
        try {
			map = resourceManager.loadMap("maps/" + maptxt);
		} catch (IOException e) {
			try {
				map = resourceManager.loadMap("maps/default.txt");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
			}
		}

        // load sounds
        
        soundManager = new SoundManager(PLAYBACK_FORMAT);
        prizeSound = soundManager.getSound("./sounds/prize.wav");
        boopSound = soundManager.getSound("./sounds/boop2.wav");
        shootSound = soundManager.getSound("./sounds/laser.wav");
        dundunSound = soundManager.getSound("./sounds/dundun.wav");
        healthSound = soundManager.getSound("./sounds/health.wav");
        gasSound = soundManager.getSound("./sounds/gurgle_x_converted.wav");
        expSound = soundManager.getSound("./sounds/explosion_x_converted.wav");
       
        // load sprites
        resourceManager.loadCreatureSprites();
        
        // start music
        midiPlayer = new MidiPlayer();
        Sequence sequence =
            midiPlayer.getSequence("./sounds/music.midi");
        //midiPlayer.play(sequence, true);
        //toggleDrumPlayback();
    }


    /**
        Closes any resurces used by the GameManager.
    */
    public void stop() {
        super.stop();
        midiPlayer.close();
        soundManager.close();
    }


    private void initInput() {
        moveLeft = new GameAction("moveLeft");
        moveRight = new GameAction("moveRight");
        jump = new GameAction("jump",
            GameAction.DETECT_INITAL_PRESS_ONLY);
        exit = new GameAction("exit",
            GameAction.DETECT_INITAL_PRESS_ONLY);
        shoot = new GameAction("shoot");
        moveUp = new GameAction("moveUp");
        moveDown = new GameAction("moveDown");

        inputManager = new InputManager(
            screen.getFullScreenWindow());
        inputManager.setCursor(InputManager.INVISIBLE_CURSOR);

        inputManager.mapToKey(moveLeft, KeyEvent.VK_LEFT);
        inputManager.mapToKey(moveRight, KeyEvent.VK_RIGHT);
        inputManager.mapToKey(shoot, KeyEvent.VK_S);
        inputManager.mapToKey(jump, KeyEvent.VK_SPACE);
        inputManager.mapToKey(exit, KeyEvent.VK_ESCAPE);
        inputManager.mapToKey(moveDown, KeyEvent.VK_DOWN);
        inputManager.mapToKey(moveUp, KeyEvent.VK_UP);
        
    }


    private void checkInput(long elapsedTime) {

        if (exit.isPressed()) {
            stop();
        }
        
        Player player = (Player)map.getPlayer();
        if (needabullet == 1){
        	needabullet = 0;
    		Sprite sprite = (Sprite)resourceManager.proSprite.clone();
        	sprite.setX(X-90);
        	sprite.setY(Y);
        	sprite.setVelocityX(-2);
            map.addSprite(sprite);
            soundManager.play(prizeSound);
        }
		
        
        if (player.isAlive()) {
            float velocityX = 0;
            if (moveLeft.isPressed()) {
                velocityX-=player.getMaxSpeed();
                playerdir = -1;
            }
            if (moveRight.isPressed()) {
                velocityX+=player.getMaxSpeed();
                playerdir = 1;
            }
            if (jump.isPressed()) {
                player.jump(false);
            }
            
            if (moveUp.isPressed()){
            	player.jump(false);
            }
            if (moveDown.isPressed()){
            	player.setVelocityY(player.getVelocityY() + 1);;
            }
            
            if (shoot.isPressed()) {
            	if ((System.currentTimeMillis() - lastshot > shotspeed) && (cooldown == 0)) {
            		
            		if (System.currentTimeMillis() - lastshot <= (shotspeed + 50)){
            			shotcount = shotcount + 1 ;
            		}
            		else{
            			shotcount = 0;
            		}
            		
            		if (shotcount < 10){
            			Sprite sprite = (Sprite)resourceManager.proSprite.clone();
		            	sprite.setX(player.getX()+(playerdir*90));
		            	sprite.setY(player.getY()+25);
		            	sprite.setVelocityX(2*playerdir);
		                map.addSprite(sprite);
		                soundManager.play(shootSound);
		                lastshot = System.currentTimeMillis();
	            		
            		}
            		else {
            			waittime = System.currentTimeMillis();
            			cooldown = 1;
            		}
            	}
            	else{
            		if (System.currentTimeMillis() - waittime >= 1000){
            			cooldown = 0;
            		}
            	}
            }
            
            player.setVelocityX(velocityX);
        }

    }


    public void draw(Graphics2D g) {
        renderer.draw(g, map,
            screen.getWidth(), screen.getHeight());
    }


    /**
        Gets the current map.
    */
    public TileMap getMap() {
        return map;
    }


    /**
        Turns on/off drum playback in the midi music (track 1).
    */
    public void toggleDrumPlayback() {
        Sequencer sequencer = midiPlayer.getSequencer();
        if (sequencer != null) {
            sequencer.setTrackMute(DRUM_TRACK,
                !sequencer.getTrackMute(DRUM_TRACK));
        }
    }


    /**
        Gets the tile that a Sprites collides with. Only the
        Sprite's X or Y should be changed, not both. Returns null
        if no collision is detected.
    */
    public Point getTileCollision(Sprite sprite,
        float newX, float newY)
    {
        float fromX = Math.min(sprite.getX(), newX);
        float fromY = Math.min(sprite.getY(), newY);
        float toX = Math.max(sprite.getX(), newX);
        float toY = Math.max(sprite.getY(), newY);

        // get the tile locations
        int fromTileX = TileMapRenderer.pixelsToTiles(fromX);
        int fromTileY = TileMapRenderer.pixelsToTiles(fromY);
        int toTileX = TileMapRenderer.pixelsToTiles(
            toX + sprite.getWidth() - 1);
        int toTileY = TileMapRenderer.pixelsToTiles(
            toY + sprite.getHeight() - 1);

        // check each tile for a collision
        for (int x=fromTileX; x<=toTileX; x++) {
            for (int y=fromTileY; y<=toTileY; y++) {
                if (x < 0 || x >= map.getWidth() ||
                    map.getTile(x, y) != null)
                {
                    // collision found, return the tile
                    pointCache.setLocation(x, y);
                    return pointCache;
                }
            }
        }

        // no collision found
        return null;
    }


    /**
        Checks if two Sprites collide with one another. Returns
        false if the two Sprites are the same. Returns false if
        one of the Sprites is a Creature that is not alive.
    */
    public boolean isCollision(Sprite s1, Sprite s2) {
        // if the Sprites are the same, return false
        if (s1 == s2) {
            return false;
        }

        // if one of the Sprites is a dead Creature, return false
        if (s1 instanceof Creature && !((Creature)s1).isAlive()) {
            return false;
        }
        if (s2 instanceof Creature && !((Creature)s2).isAlive()) {
            return false;
        }

        // get the pixel location of the Sprites
        int s1x = Math.round(s1.getX());
        int s1y = Math.round(s1.getY());
        int s2x = Math.round(s2.getX());
        int s2y = Math.round(s2.getY());

        // check if the two sprites' boundaries intersect
        return (s1x < s2x + s2.getWidth() &&
            s2x < s1x + s1.getWidth() &&
            s1y < s2y + s2.getHeight() &&
            s2y < s1y + s1.getHeight());
    }


    /**
        Gets the Sprite that collides with the specified Sprite,
        or null if no Sprite collides with the specified Sprite.
    */
    public Sprite getSpriteCollision(Sprite sprite) {

        // run through the list of Sprites
        Iterator i = map.getSprites();
        while (i.hasNext()) {
            Sprite otherSprite = (Sprite)i.next();
            if (isCollision(sprite, otherSprite)) {
                // collision found, return the Sprite
                return otherSprite;
            }
        }

        // no collision found
        return null;
    }


    /**
        Updates Animation, position, and velocity of all Sprites
        in the current map.
    */
    public void update(long elapsedTime) {
    	
        Creature player = (Creature)map.getPlayer();
        Player player1 = (Player)map.getPlayer();
        
        if ((System.currentTimeMillis() - player1.startInvincible >= 10000)|| player1.startInv >= 10){
        	player1.invincible = 0;
        }

        // player is dead! start map over
        if (player.getState() == Creature.STATE_DEAD) {
        	player1.setHealth(0);
        	movedX = 0;
        	lastX = 0;
        	time0 = 0;
            map = resourceManager.reloadMap();
            player1.setHealth(20);
            return;
        }

        if(player1.getHealth() <= 0) {
        	player.setState(Creature.STATE_DYING);
        }
        
        //updating health for moving
        if(lastX == 0){
        	lastX = player.getX();
        }
        else {
        	movedX += Math.abs(lastX - player.getX());
        	lastX = player.getX();
        }
        if(movedX >= 2*player.getWidth()) {
        	player1.modifyHealth(1);
        	movedX = 0;
        	if(player1.invincible == 1) {
        		player1.startInv++;
        	}
        }
        //updating health for staying motionless
        if(player.getVelocityX() == 0 && player1.onGround == true) {
        	time0 += elapsedTime;
        }
        else {
        	time0 = 0;
        }
        if (time0 >= 1000) {
        	player1.modifyHealth(5);
        	time0 = 0;
        }
                
        // get keyboard/mouse input
        checkInput(elapsedTime);

        // update player
        updateCreature(player, elapsedTime);
        player.update(elapsedTime);

        // update other sprites
        Iterator i = map.getSprites();

        while (i.hasNext()) {
            Sprite sprite = (Sprite)i.next();
            if (sprite instanceof Creature) {
                Creature creature = (Creature)sprite;
                if (creature.getState() == Creature.STATE_DEAD) {
                    i.remove();
                }
                else {
                    updateCreature(creature, elapsedTime);
                }
            }
            // normal update
            sprite.update(elapsedTime);
        }
    }


    /**
        Updates the creature, applying gravity for creatures that
        aren't flying, and checks collisions.
    */
    private void updateCreature(Creature creature,
        long elapsedTime)
    {

        // apply gravity
        if (!creature.isFlying()) {
            creature.setVelocityY(creature.getVelocityY() +
                GRAVITY * elapsedTime);
        }

        // change x
        float dx = creature.getVelocityX();
        float oldX = creature.getX();
        float newX = oldX + dx * elapsedTime;
        Point tile =
            getTileCollision(creature, newX, creature.getY());
        if (tile == null) {
            creature.setX(newX);
        }
        else {
            // line up with the tile boundary
            if (dx > 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x) -
                    creature.getWidth());
            }
            else if (dx < 0) {
                creature.setX(
                    TileMapRenderer.tilesToPixels(tile.x + 1));
            }
            creature.collideHorizontal();
        }
        if (creature instanceof Player) {
            checkPlayerCollision((Player)creature, false);
        }
        else {
        	//this will create a bullet sprite for the current creature.
        	//It will only create a sprite for the creature if it has been "woken up"
        	//Also the shot speed is based on the time of the last shot and is twice as slow as the player's shots
        	checkCreatureCollision(creature);
        	if ((creature.getVelocityX() != 0) && (System.currentTimeMillis() - creature.lastshot > (shotspeed*2))) {
        		if ((System.currentTimeMillis() - creature.startmoving >= 500) || (movedX >= 2*map.getPlayer().getWidth())){
        			needabullet = 1;
    	            creature.lastshot = System.currentTimeMillis();
    	            X = creature.getX();
    	            Y = creature.getY();
        		}
        	}
        }

        // change y
        float dy = creature.getVelocityY();
        float oldY = creature.getY();
        float newY = oldY + dy * elapsedTime;
        tile = getTileCollision(creature, creature.getX(), newY);
        if (tile == null) {
            creature.setY(newY);
        }
        else {
            // line up with the tile boundary
            if (dy > 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y) -
                    creature.getHeight());
            }
            else if (dy < 0) {
                creature.setY(
                    TileMapRenderer.tilesToPixels(tile.y + 1));
            }
            creature.collideVertical();
        }
        if (creature instanceof Player) {
            boolean canKill = (oldY < creature.getY());
            checkPlayerCollision((Player)creature, canKill);
        }
    }


    /**
        Checks for Player collision with other Sprites. If
        canKill is true, collisions with Creatures will kill
        them.
    */
    public void checkPlayerCollision(Player player,
        boolean canKill)
    {
        if (!player.isAlive()) {
            return;
        }

        // check for player collision with other sprites
        Sprite collisionSprite = getSpriteCollision(player);
        if (collisionSprite instanceof PowerUp) {
            acquirePowerUp((PowerUp)collisionSprite);
        }
        else if (collisionSprite instanceof Creature) {
            Creature badguy = (Creature)collisionSprite;
            if (canKill) {
                // kill the badguy and make player bounce
                soundManager.play(boopSound);
                badguy.setState(Creature.STATE_DYING);
                player.setY(badguy.getY() - player.getHeight());
                player.jump(true);
            }
            else {
                // player dies!
            	if(player.invincible == 0) {
                    player.setHealth(0);
                    soundManager.play(dundunSound);
                    player.setState(Creature.STATE_DYING);	
            	} else {
            		soundManager.play(boopSound);
                    badguy.setState(Creature.STATE_DYING);
            	}
            }
        }
        else if (collisionSprite instanceof projectile) {
        	//if player is hit by a bullet, then decrease it's health by 5
        	//make the bullet "disappear" by moving it off the screen 
        	//check to see if player is invincible before decreasing health
        	if (player.invincible == 0){
        		player.modifyHealth(-5);
        	}
        	collisionSprite.setY(5000);
        	collisionSprite.setVelocityX(0);
        	
        }
    }
    
    public void checkCreatureCollision(Creature creature) {
    	if (!creature.isAlive()) {
    		return;
    	}
    	Sprite collisionSprite = getSpriteCollision(creature);
    	if (collisionSprite instanceof projectile) {
    		//if the player kills a creature, then increase it's health by 5
    		//move the bullet off of the screen.
    		creature.setState(Creature.STATE_DYING);
    		soundManager.play(boopSound);
    		collisionSprite.setY(5000);
    		collisionSprite.setVelocityX(0);
    		Player player = (Player)map.getPlayer();
    		player.modifyHealth(5);
    	}
    }

    /**
        Gives the player the speicifed power up and removes it
        from the map.
    */
    public void acquirePowerUp(PowerUp powerUp) {
        // remove it from the map
        map.removeSprite(powerUp);
        Player player = (Player)map.getPlayer();

        if (powerUp instanceof PowerUp.Star) {
            // do something here, like give the player points
            soundManager.play(prizeSound);
            
            player.invincible = 1;
            player.startInvincible = System.currentTimeMillis();
        }
        else if (powerUp instanceof PowerUp.Music) {
            // change the music
            soundManager.play(prizeSound);
            player.setHealth(player.getHealth()+5);
        }
        else if (powerUp instanceof PowerUp.Goal) {
            // advance to next map
            soundManager.play(prizeSound,
                new EchoFilter(2000, .7f), false);
            map = resourceManager.loadNextMap();
        }
        else if (powerUp instanceof PowerUp.Shroom) {
        	soundManager.play(healthSound);
        	player.setHealth(player.getHealth()+5);
        }
        else if (powerUp instanceof PowerUp.ExpBl) {
        	soundManager.play(expSound);
        	player.setHealth(player.getHealth()-10);
        	player.setVelocityX(0);
        }
        else if (powerUp instanceof PowerUp.GasBl) {
        	soundManager.play(gasSound);
        	waittime = System.currentTimeMillis();
			cooldown = 1;
        }
    }

}


