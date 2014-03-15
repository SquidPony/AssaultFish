package assaultfish.physical;

import squidpony.squidmath.RNG;

/**
 * This class represents an enemy creature.
 *
 * @author Eben Howard
 */
public class Creature extends Item {

    public static final Creature SNOWMAN = new Creature("snowman", 5, "☃", Element.AIR),
            SANDMAN = new Creature("sandman", 5, "☃", Element.SAND),
            TARMAN = new Creature("tarman", 5, "☃", Element.TAR),
            ACIDMAN = new Creature("acidman", 5, "☃", Element.ACID),
            MAGICMAN = new Creature("magicman", 5, "☃", Element.MANA),
            PLAYER = new Creature("player", 5, "☺", Element.BLOOD);

    public int health, strength;

    public static Creature getRandomMonster() {
        return new RNG().getRandomElement(new Creature[]{SNOWMAN, SANDMAN, TARMAN, ACIDMAN, MAGICMAN});
    }

    /**
     * Creates a new monster.
     *
     * @param name
     * @param health
     * @param symbol
     * @param element
     */
    public Creature(String name, int health, String symbol, Element element) {
        super(name, symbol, element);
        this.name = name;
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
