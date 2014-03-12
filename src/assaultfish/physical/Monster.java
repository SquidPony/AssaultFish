package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * This class represents an enemy creature.
 *
 * @author Eben Howard
 */
public class Monster extends Item {

    public static final Monster //this is a list of the various monster templatespublic static final Monster //this is a list of the various monster templates
            SNOWMAN = new Monster("snowman", 5, "☃", SColor.ALICE_BLUE),
            PLAYER = new Monster("player", 10, "☺", SColor.BRIGHT_TURQUOISE);

    public int health;

    /**
     * Creates a new monster.
     *
     * @param name
     * @param health
     * @param symbol
     * @param color
     */
    public Monster(String name, int health, String symbol, SColor color) {
        super(name,  symbol, color);
        this.health = health;
    }

    /**
     * Creates a new Monster that is a clone of the passed in monster.
     *
     * @param other
     */
    public Monster(Monster other) {
        super(other);
        this.health = other.health;
    }

    public int getHealth() {
        return health;
    }

    /**
     * Reduces the monster's health by the amount passed in. Returns true if this results in the
     * health being equal to or below zero.
     *
     * @param damage
     * @return
     */
    public boolean causeDamage(int damage) {
        health -= damage;
        return health <= 0;
    }

    @Override
    public String toString() {
        return name + ": " + symbol + " @ " + health;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Monster ? ((Monster) obj).name.equals(name) : false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + symbol.codePointAt(0) + color.hashCode();
    }

}
