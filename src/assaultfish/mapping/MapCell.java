package assaultfish.mapping;

import assaultfish.physical.Creature;
import assaultfish.physical.Item;
import assaultfish.physical.Terrain;
import assaultfish.physical.TerrainFeature;
import java.util.HashMap;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidgrid.fov.FOVCell;
import squidpony.squidgrid.util.Direction;

/**
 * Represents a single square in the game world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class MapCell implements FOVCell {

    public Creature creature;
    public Terrain terrain;
    public TerrainFeature feature;
    public Item item;
    public SColor light = SColor.BLACK;
    public boolean seen = false;
    private final HashMap<String, Float> fov = new HashMap<>();

    @Override
    public float resistance(String key) {
        switch (key) {
            case "movement":
                if (feature == null) {
                    return terrain.blocking ? 1f : 0f;
                } else {
                    return terrain.blocking || feature.blocking ? 1f : 0f;
                }
            case "sight":
                if (feature == null) {
                    return 0f;
                } else {
                    return feature.opacity;
                }
            default:
                return 1f;
        }
    }

    @Override
    public float resistance(String key, Direction direction) {
        return resistance(key);
    }

    @Override
    public void setFOVResult(String key, float value) {
        fov.put(key, value);
    }

    @Override
    public void setFOVResult(String key, Direction direction, float value) {
        fov.put(key, value);
    }

    public SColor color() {
        //unlit
        if (light.equals(SColor.BLACK)) {
            if (seen) {//previously seen
                if (item != null) {
                    return SColorFactory.blend(SColorFactory.desaturated(item.getColor()), SColor.BLACK, 0.75f);
                } else if (feature != null) {
                    return SColorFactory.blend(SColorFactory.desaturated(feature.getColor()), SColor.BLACK, 0.75f);
                } else {
                    return SColorFactory.blend(SColorFactory.desaturated(terrain.getColor()), SColor.BLACK, 0.75f);
                }
            }
            return SColor.BLACK;//nothing to see
        }

        //lit
        return SColorFactory.lightWith(getTopItem().getColor(), light);
    }

    public String getSymbol() {
        //unlit
        if (light.equals(SColor.BLACK)) {
            if (seen) {
                if (item != null) {
                    return item.getSymbol();
                } else if (feature != null) {
                    return feature.getSymbol();
                } else {
                    return terrain.getSymbol();
                }
            }
            return " ";//nothing to see
        }

        //lit
        return getTopItem().getSymbol();
    }

    public Item getTopItem() {
        Item display = creature;
        if (display == null) {
            display = item;
        }
        if (display == null) {
            display = feature;
        }
        if (display == null) {
            display = terrain;
        }
        return display;
    }
}
