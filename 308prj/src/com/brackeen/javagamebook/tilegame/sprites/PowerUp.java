package com.brackeen.javagamebook.tilegame.sprites;

import java.lang.reflect.Constructor;
import com.brackeen.javagamebook.graphics.*;

/**
    A PowerUp class is a Sprite that the player can pick up.
*/
public abstract class PowerUp extends Sprite {

    public PowerUp(Animation anim) {
        super(anim);
    }


    public Object clone() {
        // use reflection to create the correct subclass
        Constructor constructor = getClass().getConstructors()[0];
        try {
            return constructor.newInstance(
                new Object[] {(Animation)anim.clone()});
        }
        catch (Exception ex) {
            // should never happen
            ex.printStackTrace();
            return null;
        }
    }


    /**
        A Star PowerUp. Gives the player points.
    */
    public static class Star extends PowerUp {
        public Star(Animation anim) {
            super(anim);
        }
    }


    /**
        A Music PowerUp. Changes the game music.
    */
    public static class Music extends PowerUp {
        public Music(Animation anim) {
            super(anim);
        }
    }


    /**
        A Goal PowerUp. Advances to the next map.
    */
    public static class Goal extends PowerUp {
        public Goal(Animation anim) {
            super(anim);
        }
    }
    
    /**
    	A Shroom PowerUp. Adds health.
	*/
	public static class Shroom extends PowerUp {
	    public Shroom(Animation anim) {
	        super(anim);
	    }
	}
	
    /**
		Explostion Block
	*/
	public static class GasBl extends PowerUp {
	    public GasBl(Animation anim) {
	        super(anim);
	    }
	}
	
    /**
		Gas Block
	*/
	public static class ExpBl extends PowerUp {
	    public ExpBl(Animation anim) {
	        super(anim);
	    }
	}

}
