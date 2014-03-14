package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * Represents something that can sit in a map space but is not the terrain itself, a monster, or an
 * item. This includes things like doors, piles of junk, and low brush. They may be interacted with
 * but not picked up.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class TerrainFeature extends Item {

    public static final TerrainFeature STONE_WALL = new TerrainFeature("stone wall", "#", SColor.SILVER, true, true),
            DIRT_WALL = new TerrainFeature("dirt wall", "#", SColor.RUSSET, true, true),
            TREE = new TerrainFeature("tree", "T", SColor.FOREST_GREEN, true, false);
    public boolean blocking;
    public boolean opaque;

    public TerrainFeature(String name, String symbol, Element element, boolean blocking, boolean opaque) {
        super(name,  symbol, element);
        this.blocking = blocking;
        this.opaque = opaque;
    }

    public TerrainFeature(String name, String symbol, SColor color, boolean blocking, boolean opaque) {
        super(name,  symbol, color);
        this.blocking = blocking;
        this.opaque = opaque;
    }

    /**
     * Creates an elementally affiliated wall that is sight and movement blocking.
     *
     * @param e
     * @return
     */
    public static TerrainFeature createElementalWall(Element e) {
        return new TerrainFeature(e.name + " wall", "#", e, true, true);
    }

    public static TerrainFeature createElementalPuddle(Element e) {
        return new TerrainFeature(e.name + " puddle", "m", e, true, true);
    }

    /**
     * Creates a non-blocking feature using the creature name passed in to make a pile of bones.
     *
     * @param s
     * @return
     */
    public static TerrainFeature createBones(String s) {
        return new TerrainFeature("pile of " + s + " bones", "%", SColor.EGGSHELL_PAPER, false, false);
    }
}
