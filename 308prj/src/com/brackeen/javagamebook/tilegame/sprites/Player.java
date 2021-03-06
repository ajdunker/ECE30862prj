package com.brackeen.javagamebook.tilegame.sprites;

import com.brackeen.javagamebook.graphics.Animation;

/**
    The Player.
*/
public class Player extends Creature {

    private static final float JUMP_SPEED = -.95f;

    public boolean onGround;
    public int isShooting = 0;
    public int invincible = 0;
    public long startInvincible = 0;	//for 1 sec
    public int startInv = 0;			//for 10 steps
    
    private int health = 20;
    private int score = 0;

    public Player(Animation left, Animation right,
        Animation deadLeft, Animation deadRight)
    {
        super(left, right, deadLeft, deadRight);
    }

    public void collideHorizontal() {
        setVelocityX(0);
    }


    public void collideVertical() {
        // check if collided with ground
        if (getVelocityY() > 0) {
            onGround = true;
        }
        setVelocityY(0);
    }
    
    public void setY(float y) {
        // check if falling
        if (Math.round(y) > Math.round(getY())) {
            onGround = false;
        }
        super.setY(y);
    }
    
    public void setHealth(int x) {
    	if(x > 0 && x <= 40) {
    		health = x;
    	}
    }
    
    public void modifyHealth(int x) {
    	if(health + x > 40){
    		health = 40;
    	}
    	else {
    		health += x;
    	}
    }

    public int getHealth() {
    	return health;
    }
    
    public void modifyScore(int x) {
    	score += x;
    }
    
    public int getScore() {
    	return score;
    }
    
    public void wakeUp() {
        // do nothing
    }

    /**
        Makes the player jump if the player is on the ground or
        if forceJump is true.
    */
    public void jump(boolean forceJump) {
        if (onGround || forceJump) {
            onGround = false;
            setVelocityY(JUMP_SPEED);
        }
    }


    public float getMaxSpeed() {
        return 0.5f;
    }

}
