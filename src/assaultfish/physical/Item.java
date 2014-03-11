package assaultfish.physical;

import java.awt.Point;
import squidpony.squidcolor.SColor;

/**
 * An Item is something that can exist in the game world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Item {

    private String name, description;
    private String symbol;
    private Element element;
    private SColor color;
    public Point location;

    public Item() {
    }

    /**
     * This constructor requires an elemental affiliation and will color the object based on the
     * element provided.
     *
     * @param name
     * @param description
     * @param symbol
     * @param element
     */
    public Item(String name, String description, int symbol, Element element) {
        this.name = name;
        this.description = description;
        this.symbol = new String(Character.toChars(symbol));
        this.element = element;
    }

    /**
     * This constructor assumes no elemental affiliation and requires a color.
     *
     * @param name
     * @param description
     * @param symbol
     * @param color
     */
    public Item(String name, String description, int symbol, SColor color) {
        this.name = name;
        this.description = description;
        this.symbol = new String(Character.toChars(symbol));
        this.color = color;
    }

    public Item(Item other) {
        name = other.name;
        description = other.description;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description + (element == null ? "" : " It has been saturated with " + element.name + ".");
    }
}
