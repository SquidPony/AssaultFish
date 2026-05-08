package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * Terrain is the base of the map.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Terrain {

    public static final Terrain DIRT = new Terrain("dirt", ".", SColor.ALOEWOOD, false),
            STONE = new Terrain("rocky ground", ".", SColor.DULL_BLUE, false),
            GRASS = new Terrain("grass", ".", SColor.GREEN_BAMBOO, false);

    public String name;
    public String symbol;
    public SColor color;
    public Element element = null;
    public boolean blocking = false;
    public boolean lake = false;

    public Terrain(String name, String symbol, SColor color, boolean blocking) {
        this.name = name;
        this.symbol = symbol;
        this.color = color;
        this.blocking = blocking;
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
        lake = other.lake;
    }

    public static Terrain makeElementalFloor(Element element, boolean blocking) {
        return new Terrain(element.adjective + " floor", "_", element.color, blocking);
    }

    public static Terrain makeElementalPool(Element element) {
        Terrain t = new Terrain(element.adjective + " pool", "~", element, true);
        t.lake = true;
        return t;
    }
}
