package assaultfish.mapping;

import assaultfish.physical.Item;
import assaultfish.physical.Monster;
import assaultfish.physical.Terrain;
import assaultfish.physical.TerrainFeature;
import java.util.ArrayList;
import java.util.Arrays;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidgrid.util.Direction;

/**
 * Represents a single square in the game world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class MapCell{

    public Monster creature;
    public Terrain terrain;
    public ArrayList<TerrainFeature> features;
    public Item item;
    public SColor light = SColor.BLACK;
    public boolean seen = false;

    public MapCell() {
    }

    public MapCell(Terrain t) {
        terrain = t;
    }

    public MapCell(Terrain t, TerrainFeature... f) {
        terrain = t;
        features.addAll(Arrays.asList(f));
    }

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
