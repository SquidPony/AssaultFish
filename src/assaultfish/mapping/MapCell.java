package assaultfish.mapping;

import assaultfish.physical.Item;
import assaultfish.physical.Creature;
import assaultfish.physical.Element;
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

    public Creature creature;
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

    public MapCell(Terrain terrain, TerrainFeature feature) {
        this.terrain = terrain;
        this.feature = feature;
    }

    public SColor backgroundColor() {
        if (seen) {
            if (light.equals(SColor.BLACK) || terrain.element != null) {
                return SColorFactory.dimmest(SColor.BLACK_DYE);
            } else {
                return SColorFactory.dimmest(terrain.color);
            }
        } else {
            return SColor.BLACK;
        }
//        if (light.equals(SColor.BLACK)) {
//            if (seen) {
//                if (feature == null || (creature == null && item == null)) {
//                    return SColorFactory.blend(SColorFactory.desaturated(terrain.color), SColor.BLACK, 0.75f);
//                } else {
//                    return SColorFactory.blend(SColorFactory.desaturated(feature.color), SColor.BLACK, 0.75f);
//                }
//            } else {
//                return light;
//            }
//        }
//        
//        if (feature != null) {
//            if (creature == null && item == null) {
//                return SColorFactory.lightWith(terrain.color, light);
//            } else {
//                return SColorFactory.lightWith(feature.color, light);
//            }
//        } else if (creature != null) {
//            return SColorFactory.lightWith(creature.color, light);
//        } else if (item != null) {
//            return SColorFactory.lightWith(item.color, light);
//        } else {
//            return SColorFactory.lightWith(terrain.color, light);
//        }
    }

    public SColor foregroundColor() {
        if (seen) {//previously seen
            //unlit
            if (light.equals(SColor.BLACK)) {
                return SColorFactory.blend(SColorFactory.desaturated(creature != null ? creature.color : item != null ? item.color : feature != null ? feature.color : SColor.TRANSPARENT), SColor.BLACK, 0.75f);
            }
            //lit
            return SColorFactory.lightWith(creature != null ? creature.color : item != null ? item.color : feature != null ? feature.color : terrain.color, light);
        }

        return SColor.TRANSPARENT;//nothing to see
    }

    public String getSymbol() {
        if (!light.equals(SColor.BLACK)) {
            if (creature != null) {
                return creature.symbol;
            } else if (item != null) {
                return item.symbol;
            }
        }

        if (feature != null) {
            return feature.symbol;
        }

        return terrain.symbol;
    }

    public boolean isOpaque() {
        return feature == null ? false : feature.opaque;
    }

    public boolean isBlocking() {
        return (feature == null ? false : feature.blocking) || terrain.blocking;
    }

}
