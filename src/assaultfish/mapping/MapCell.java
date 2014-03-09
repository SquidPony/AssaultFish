package assaultfish.mapping;

import assaultfish.old.physical.Creature;
import assaultfish.old.physical.Interaction;
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
    public Interaction interaction;
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
            if (creature != null) {
                if (creature.heard || creature.smelled) {
                    return SColorFactory.blend(SColorFactory.desaturate(creature.color, 0.3f), SColor.BLACK, 0.65f);
                }
            }
            if (seen) {
                if (item != null) {
                    return SColorFactory.blend(SColorFactory.desaturated(item.color), SColor.BLACK, 0.75f);
                } else {
                    return SColorFactory.blend(SColorFactory.desaturated(furniture.color), SColor.BLACK, 0.75f);
                }
            }
            return SColor.BLACK;//nothing to see
        }

        //lit
        SColor coloring = null;
        Item display = creature;
        if (display == null) {
            display = item;
        } else {
            if (creature.hiding) {
                coloring = SColorFactory.blend(SColor.BLACK, creature.color, 0.8f);
            }
        }
        if (display == null) {
            display = furniture;
        }
        if (coloring == null) {
            coloring = display.color;
        }
        return SColorFactory.lightWith(coloring, light);
    }

    public int getSymbol() {
        //unlit
        if (light.equals(SColor.BLACK)) {
            if (creature != null) {
                if (creature.heard || creature.smelled) {
                    return creature.symbol;
                }
            }
            if (seen) {
                if (item != null) {
                    return item.symbol;
                } else {
                    return furniture.symbol;
                }
            }
            return ' ';//nothing to see
        }

        //lit
        Item display = creature;
        if (display == null) {
            display = item;
        }
        if (display == null) {
            display = furniture;
        }
        return display.symbol;
    }
}
