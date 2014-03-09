package assaultfish.physical;

import squidpony.squidcolor.SColor;

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
    MAGMA("magma", "heated", SColor.SCARLET),
    MANA("mana", "enchanted", SColor.ROYAL_PURPLE);

    public String name, adjective;
    public SColor color;

    private Element(String name, String adjective, SColor color) {
        this.name = name;
        this.adjective = adjective;
        this.color = color;
    }

}
