package assaultfish.mapping;

import assaultfish.physical.Item;
import assaultfish.physical.Monster;
import assaultfish.physical.Terrain;
import assaultfish.physical.TerrainFeature;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;

/**
 * Represents a single square in the game world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class MapCell {

    public Monster creature;
    public Terrain terrain;
    public TerrainFeature feature;
    public Item item;
    public SColor light = SColor.BLACK;
    public boolean seen = false;

    public MapCell() {
    }

    public MapCell(Terrain t) {
        terrain = t;
    }

    public MapCell(Terrain treasure, TerrainFeature feature) {
        this.terrain = treasure;
        this.feature = feature;
    }

    public SColor backgroundColor() {
        if (light.equals(SColor.BLACK)) {
            if (seen) {
                if (feature != null) {
                    return SColorFactory.blend(SColorFactory.desaturated(feature.color), SColor.BLACK, 0.75f);
                } else {
                    return SColorFactory.blend(SColorFactory.desaturated(terrain.color), SColor.BLACK, 0.75f);
                }
            } else {
                return light;
            }
        }

        return SColorFactory.lightWith(feature == null ? terrain.color : feature.color, light);
    }

    public SColor foregroundColor() {
        //unlit
        if (light.equals(SColor.BLACK)) {
            if (seen && (creature != null || item != null)) {//previously seen
                return SColorFactory.blend(SColorFactory.desaturated(item == null ? creature.color : item.getColor()), SColor.BLACK, 0.75f);
            }
            return SColor.TRANSPARENT;//nothing to see
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
                } else {
                    return terrain.symbol;
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
