package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * Terrain is the base of the map.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Terrain {

    public final Terrain DIRT = new Terrain("dirt floor", ".", SColor.BRIGHT_GOLD_BROWN, false),
            STONE = new Terrain("stone floor", ".", SColor.LIGHT_GRAY, false),
            WATER = new Terrain("water", "~", Element.WATER, true);

    public String name;
    public String symbol;
    public SColor color;
    public Element element;
    public boolean blocking;

    public Terrain(String name, String symbol, SColor color, boolean blocking) {
        this.name = name;
        this.symbol = symbol;
        this.color = color;
        this.blocking = blocking;
        element = Element.NONE;
    }

    public Terrain(String name, String symbol, Element element, boolean blocking) {
        this.name = name;
        this.symbol = symbol;
        this.element = element;
        this.blocking = blocking;
        color = element.color;
    }

    public Terrain(Terrain other) {
        name = other.name;
        symbol = other.symbol;
        element = other.element;
        blocking = other.blocking;
        color = other.color;
    }

    public static Terrain makeElementalFloor(Element element, boolean blocking) {
        return new Terrain(element.adjective + " floor", ".", element.color, blocking);
    }
}
