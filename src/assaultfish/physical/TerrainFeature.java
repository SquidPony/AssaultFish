package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * Represents something that can sit in a map space but is not the terrain itself, a monster, or an
 * item. This includes things like doors, piles of junk, and low brush.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class TerrainFeature extends Item {

    public static final TerrainFeature STONE_WALL = new TerrainFeature("stone wall", "A wall which blocks movement.", '#', SColor.SILVER, true, 1.0f),
            DIRT_WALL = new TerrainFeature("dirt wall", "A wall which blocks movement.", '#', SColor.RUSSET, true, 1.0f),
            TAR_MOUND = new TerrainFeature("tar mound", "A small mound of tar.", 'm', Element.TAR, true, 0.1f);
    public boolean blocking;
    public float opacity;

    public TerrainFeature(String name, String description, int symbol, Element element, boolean blocking, float opacity) {
        super(name, description, symbol, element);
        this.blocking = blocking;
        this.opacity = opacity;
    }

    public TerrainFeature(String name, String description, int symbol, SColor color, boolean blocking, float opacity) {
        super(name, description, symbol, color);
        this.blocking = blocking;
        this.opacity = opacity;
    }

    /**
     * Creates an elementally affiliated wall that is sight and movement blocking.
     *
     * @param e
     * @return
     */
    public static TerrainFeature createElementalWall(Element e) {
        return new TerrainFeature(e.name + " wall", "A stabalized " + e.adjective + " wall.", '#', e, true, 1.0f);
    }

    /**
     * Creates a non-blocking feature using the creature name passed in to make a pile of bones.
     *
     * @param s
     * @return
     */
    public static TerrainFeature createBones(String s) {
        return new TerrainFeature("pile of " + s + " bones", "A scattered pile of " + s + " bones.", '%', SColor.EGGSHELL_PAPER, false, 0.0f);
    }
}
