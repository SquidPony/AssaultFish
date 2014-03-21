package assaultfish.physical;

import squidpony.squidmath.RNG;

/**
 * This class represents an enemy creature.
 *
 * @author Eben Howard
 */
public class Creature extends Item {

    private static final RNG rng = new RNG();

    public static final Creature AIRMAN = new Creature("man", 5, "☃", Element.AIR),
            SANDMAN = new Creature("man", 5, "☃", Element.SAND),
            TARMAN = new Creature("man", 5, "☃", Element.TAR),
            ACIDMAN = new Creature("man", 5, "☃", Element.ACID),
            MAGICMAN = new Creature("man", 5, "☃", Element.MANA),
            BLOODMAN = new Creature("man", 5, "☃", Element.BLOOD),
            MAGMAMAN = new Creature("man", 5, "☃", Element.MAGMA),
            WATERMAN = new Creature("man", 5, "☃", Element.WATER),
            PLAYER = new Creature("player", 5, "☺", Element.BLOOD);

    public int health, strength;

    public static Creature getRandomMonster() {
        return new Creature(rng.getRandomElement(new Creature[]{AIRMAN, SANDMAN, TARMAN, ACIDMAN, MAGICMAN, BLOODMAN, MAGMAMAN, WATERMAN}));
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
     * Reduces the monster's health by the amount passed in. Returns true if
     * this results in the health being equal to or below zero.
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
