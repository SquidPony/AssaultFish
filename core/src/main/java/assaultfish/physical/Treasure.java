package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * This class represents some item of treasure found in the game.
 *
 * @author Eben Howard
 */
public class Treasure extends Item implements Comparable<Treasure> {

    public static final Treasure GEMSTONE = new Treasure("emerald", 1000, SColor.EMERALD),
            MONEY_BAG = new Treasure("money bag", 200, SColor.BROWNER),
            POO = new Treasure("poo", 1, SColor.BROWNER);
    public static final Treasure[] PROTOTYPES = new Treasure[]{GEMSTONE, MONEY_BAG, POO};

    public int value;

    /**
     * Creates a treasure with the provided name and value.
     *
     * @param name
     * @param value
     * @param color
     */
    public Treasure(String name, int value, SColor color) {
        super(name, "$", color);
        this.value = value;
    }

    public Treasure(Treasure other) {
        super(other);
        value = other.value;
    }

    @Override
    public int compareTo(Treasure o) {
        return (int) Math.signum(value - o.value);
    }
}
