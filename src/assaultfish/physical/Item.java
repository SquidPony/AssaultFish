package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * An Item is something that can exist in the game world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Item {

    public String name;
    public String symbol;
    public Element element;
    public SColor color;
    public int x, y;

    public Item() {
    }

    /**
     * This constructor requires an elemental affiliation and will color the object based on the
     * element provided.
     *
     * @param name
     * @param symbol
     * @param element
     */
    public Item(String name,  String symbol, Element element) {
        this.name = name;
        this.symbol = symbol;
        this.element = element;
    }

    /**
     * This constructor assumes no elemental affiliation and requires a color.
     *
     * @param name
     * @param symbol
     * @param color
     */
    public Item(String name, String symbol, SColor color) {
        this.name = name;
        this.symbol = symbol;
        this.color = color;
    }

    public Item(Item other) {
        name = other.name;
        symbol = other.symbol;
        element = other.element;
    }

    public String getSymbol() {
        return symbol;
    }

    public SColor getColor() {
        return element == null ? color : element.color;
    }

    public String getDisplayName() {
        return (element == null ? "" : element.adjective + " ") + name;
    }

}
