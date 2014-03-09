package assaultfish;

/**
 * This class represents some item of treasure found in the game.
 *
 * @author Eben Howard
 */
public class Treasure implements Comparable<Treasure> {

    public static final Treasure GEMSTONE = new Treasure("gem", 1000),
            MONEY_BAG = new Treasure("money bag", 200),
            POO = new Treasure("poo", 1);
    public static final Treasure[] PROTOTYPES = new Treasure[]{GEMSTONE, MONEY_BAG, POO};

    private final String name;
    private final int value;

    /**
     * Creates a treasure with the provided name and value.
     *
     * @param name
     * @param value
     */
    public Treasure(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public Treasure(Treasure other) {
        name = other.name;
        value = other.value;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int compareTo(Treasure o) {
        return (int) Math.signum(value - o.value);
    }
}
