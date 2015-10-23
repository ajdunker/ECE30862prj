package com.brackeen.javagamebook.tilegame.sprites;

import java.lang.reflect.Constructor;
import com.brackeen.javagamebook.graphics.*;

public class projectile extends Sprite {

    private Animation shootL;
    private Animation shootR;
    
    public int STATE_WAIT = 0;
    public int STATE_SHOT = 1;
    
    private int state;
    
    public projectile(Animation shootL, Animation shootR) {
        super(shootL);
        this.shootR = shootR;
        this.shootL = shootL;
        state = STATE_WAIT;
        
    }
    public Object clone() {
        // use reflection to create the correct subclass
        Constructor constructor = getClass().getConstructors()[0];
        try {
            return constructor.newInstance(new Object[] {
                (Animation)shootL.clone(),
                (Animation)shootR.clone()
            });
        }
        catch (Exception ex) {
            // should never happen
            ex.printStackTrace();
            return null;
        }
    }
    //public void collideHorizontal() {
    //   setVelocityX(0);
    //}
    
    public float getMaxSpeed() {
        return 0.75f;
    }
    
    public int getState() {
        return state;
    }
    
    public void setState(int shooting) {
        
    	this.state = shooting;
    }
}
