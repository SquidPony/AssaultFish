package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * Terrain is the base of the map.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Terrain extends Item {

    public static final Terrain DIRT = new Terrain("dirt floor", "Plain dirt floor.", '.', SColor.BRIGHT_GOLD_BROWN, false),
            STONE = new Terrain("stone floor", "Plain stone floor.", '.', SColor.LIGHT_GRAY, false),
            WATER = new Terrain("water", "Fairly deep water.", '~', Element.WATER, true);
    public boolean blocking;

    public Terrain(String name, String description, int symbol, SColor color, boolean blocking) {
        super(name, description, symbol, color);
        this.blocking = blocking;
    }

    public Terrain(String name, String description, int symbol, Element element, boolean blocking) {
        super(name, description, symbol, element);
        this.blocking = blocking;
    }

}
