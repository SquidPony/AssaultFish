package assaultfish.old.physical;

import assaultfish.physical.Item;
import java.util.HashMap;
import squidpony.squidgrid.fov.FOVCell;
import squidpony.squidgrid.util.Direction;

/**
 * Anything stationary that might block movement.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Furniture extends Item implements FOVCell {

    public HashMap<String, Float> resistances = new HashMap<>();
    public HashMap<String, HashMap<Direction, Float>> lighting = new HashMap<>();
    public boolean movementBlocking = false;//indicates that creatures can not move through it
    public boolean sightBlocking = false;
    public boolean hoppable = false;

    public Furniture() {
    }

    @Override
    public float resistance(String key) {
        return resistances.get(key);
    }

    @Override
    public float resistance(String key, Direction direction) {//all objects in this game have the same opacity in all directions
        return resistance(key);
    }

    @Override
    public void setFOVResult(String key, float value) {
        HashMap<Direction, Float> light = lighting.get(key);
        if (light == null) {
            light = new HashMap<>();
            lighting.put(key, light);
        }

        light.put(Direction.NONE, value);
    }

    @Override
    public void setFOVResult(String key, Direction direction, float value) {
        HashMap<Direction, Float> light = lighting.get(key);
        if (light == null) {
            light = new HashMap<>();
            lighting.put(key, light);
        }

        light.put(direction, value);
    }
}
