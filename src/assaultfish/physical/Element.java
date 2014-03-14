package assaultfish.physical;

import squidpony.squidcolor.SColor;
import squidpony.squidmath.RNG;

/**
 * The types of base elements in the world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public enum Element {

    WATER("water", "watery", SColor.AZUL),
    AIR("air", "airy", SColor.ALICE_BLUE),
    ACID("acid", "acidic", SColor.CHARTREUSE),
    TAR("tar", "tarred", SColor.STOREROOM_BROWN),
    SAND("sand", "sandy", SColor.KHAKI),
    BLOOD("blood", "bloody", SColor.SCARLET),
    MAGMA("magma", "heated", SColor.SAFETY_ORANGE),
    MANA("mana", "enchanted", SColor.ROYAL_PURPLE);

    public final String name, adjective;
    public final SColor color;

    private Element(String name, String adjective, SColor color) {
        this.name = name;
        this.adjective = adjective;
        this.color = color;
    }

    /**
     * Will return a random element of all possible elements except "NONE".
     *
     * @return
     */
    public static Element getRandomElement() {
        return new RNG().getRandomElement(new Element[]{WATER, AIR, ACID, TAR, SAND, MAGMA, MANA, BLOOD});
    }

}
