package assaultfish.old.physical;

import assaultfish.physical.Item;
import assaultfish.mapping.Map;
import static assaultfish.old.physical.ActionType.CONVERSE;
import static assaultfish.old.physical.ActionType.IDLE;
import static assaultfish.old.physical.ActionType.MOVE;
import static assaultfish.old.physical.ActionType.SHOOT;
import java.awt.Point;
import java.util.ArrayList;
import squidpony.squidmath.RNG;
import squidpony.squidgrid.util.BasicRadiusStrategy;
import squidpony.squidgrid.util.RadiusStrategy;

/**
 * A creature can take actions and interact with the game world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Creature extends Item {

    private static RNG rng = new RNG();
    private static RadiusStrategy strat = BasicRadiusStrategy.SQUARE;
    public Point location;
    public boolean smelled = false;
    public boolean heard = false;
    public boolean aware = false;//if they know the player is there
    public boolean grabbable = true;
    public String deathSound, idleSound;
    //
    //sight related (constant, may be consciously modified by creature if trying to hide
    public float size = 2f;//in meters, for only the largest dimension (height or width). default to human sized
    public float opacity = 1f;//default to opaque
    public float hidingModifier = 0.8f;//how much their visual size is reduced when hiding
    public boolean hiding = false;//marks if they're activily hiding
    //
    //sound related (action caused, last until action changed)
    public float movement = 1f;//default to normal footstep volume
    public boolean hurrying = false;//marks if their in a hurry and less cautious about sound production
    public float hurryingModifyer = 2f;//default to normal running / loud conversation level
    public boolean sneaking = false;//marks if they're activily trying to reduce their sounds
    public float sneakingModifyer = 0.8f;//how much their noises are reduced when sneaking
    public float conversation = 1f;//normal speaking volume
    //
    //smell related (constant)
    public float odor = 1f;//normal human body odor when using deoderant
    //
    //stats
    public int vision = 10;
    public int smell = 10;//sense of smell, not odor
    public int hearing = 10;
    public int meleeAccuracy = 5;//base meleeAccuracy
    public int meleeDamage = 3;
    public int health = 10;
    public int armor = 0, shield = 0, barrier = 0;
    public boolean male = true;
    public int xp = 10;
    public ArrayList<Condition> conditions;//being on fire, poisoned, damage reduction, etc. anything with a time limit
    //
    //equipment
    public Weapon weapon;
    public float range;//how far away an enemy will shoot
    public int thermalClips = 1;

    /**
     * Returns the creature's volume output based on hurrying and sneaking.
     *
     * Note that a creature may be both hurrying and sneaking and both modifiers
     * will apply.
     *
     * @return
     */
    public float getVolume() {
        float volume = movement;
        if (hurrying) {
            volume *= hurryingModifyer;
        }
        if (sneaking) {
            volume *= sneakingModifyer;
        }
        return volume;
    }

    /**
     * Returns the creature's visibility based on size and hiding. Opacity is
     * not taken into account.
     *
     * @return
     */
    public float getVisibility() {
        float visibility = size;
        if (hiding) {
            visibility *= hidingModifier;
        }
        return visibility;
    }

    /**
     * Returns an action based on the provided map and the creature's AI.
     *
     * @param map
     * @return
     */
    public ActionType getAction(Map map, Creature player) {
        if (!aware) {
            return rng.nextBoolean() ? IDLE : CONVERSE;
        }

        if (weapon != null && map.isVisible(location.x, location.y, player.location.x, player.location.y) && strat.radius(location.x, location.y, player.location.x, player.location.y) <= range && rng.nextDouble() < 0.15) {
            return SHOOT;
        } else {
            return MOVE;
        }
    }

}
