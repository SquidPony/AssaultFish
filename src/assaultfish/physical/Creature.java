package assaultfish.physical;

import squidpony.squidcolor.SColor;

/**
 * This class represents an enemy creature.
 *
 * @author Eben Howard
 */
public class Creature extends Item {

    public static final Creature //this is a list of the various monster templatespublic static final Monster //this is a list of the various monster templatespublic static final Creature //this is a list of the various monster templatespublic static final Monster //this is a list of the various monster templates
            SNOWMAN = new Creature("snowman", 5, "☃", SColor.ALICE_BLUE),
            PLAYER = new Creature("player", 10, "☺", SColor.BRIGHT_TURQUOISE);

    public int health, strength;

    /**
     * Creates a new monster.
     *
     * @param name
     * @param health
     * @param symbol
     * @param color
     */
    public Creature(String name, int health, String symbol, SColor color) {
        super(name,  symbol, color);
        this.health = health;
    }

    /**
     * Creates a new Monster that is a clone of the passed in monster.
     *
     * @param other
     */
    public Creature(Creature other) {
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
        return obj instanceof Creature ? ((Creature) obj).name.equals(name) : false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() + symbol.codePointAt(0) + color.hashCode();
    }

}
