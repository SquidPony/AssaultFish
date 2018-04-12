package assaultfish.physical;

import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidmath.IRNG;
import squidpony.squidmath.OrderedMap;

import java.util.HashMap;

/**
 * The types of base elements in the world.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public enum Element {

    WATER("water", "watery", "soaks", "soaked", SColor.AZUL),
    AIR("air", "cloudy", "gusts", "gusted", SColor.ALICE_BLUE),
    ACID("acid", "corrosive", "melts", "melted", SColor.CHARTREUSE),
    TAR("tar", "goopy", "tars", "tarred", SColor.ALOEWOOD),
    SAND("sand", "sandy", "sandblasts", "sandblasted", SColor.KHAKI),
    BLOOD("blood", "vampiric", "sanguinates", "sanguinated", SColor.SCARLET),
    MAGMA("magma", "burning", "burns", "burned", SColor.SAFETY_ORANGE),
    MANA("mana", "mystical", "enchants", "enchanted", SColor.ROYAL_PURPLE);

    public final String name, adjective, presentVerb, pastVerb;
    public final SColor color;
    private static final OrderedMap<Element, HashMap<Element, Element>> transform = new OrderedMap<>();

    static {
        HashMap<Element, Element> hm;

        //water
        hm = new HashMap<>();
        hm.put(WATER, WATER);
        hm.put(AIR, WATER);
        hm.put(ACID, ACID);
        hm.put(TAR, TAR);
        hm.put(SAND, TAR);
        hm.put(BLOOD, BLOOD);
        hm.put(MAGMA, AIR);
        hm.put(MANA, WATER);
        transform.put(WATER, hm);

        //air
        hm = new HashMap<>();
        hm.put(WATER, WATER);
        hm.put(AIR, AIR);
        hm.put(ACID, MANA);
        hm.put(TAR, TAR);
        hm.put(SAND, SAND);
        hm.put(BLOOD, BLOOD);
        hm.put(MAGMA, MAGMA);
        hm.put(MANA, ACID);
        transform.put(AIR, hm);

        //acid
        hm = new HashMap<>();
        hm.put(WATER, ACID);
        hm.put(AIR, WATER);
        hm.put(ACID, ACID);
        hm.put(TAR, SAND);
        hm.put(SAND, AIR);
        hm.put(BLOOD, WATER);
        hm.put(MAGMA, TAR);
        hm.put(MANA, MANA);
        transform.put(ACID, hm);

        //tar
        hm = new HashMap<>();
        hm.put(WATER, TAR);
        hm.put(AIR, TAR);
        hm.put(ACID, SAND);
        hm.put(TAR, TAR);
        hm.put(SAND, TAR);
        hm.put(BLOOD, MANA);
        hm.put(MAGMA, SAND);
        hm.put(MANA, WATER);
        transform.put(TAR, hm);

        //sand
        hm = new HashMap<>();
        hm.put(WATER, TAR);
        hm.put(AIR, ACID);
        hm.put(ACID, WATER);
        hm.put(TAR, TAR);
        hm.put(SAND, SAND);
        hm.put(BLOOD, MAGMA);
        hm.put(MAGMA, SAND);
        hm.put(MANA, WATER);
        transform.put(SAND, hm);

        //blood
        hm = new HashMap<>();
        hm.put(WATER, BLOOD);
        hm.put(AIR, MANA);
        hm.put(ACID, WATER);
        hm.put(TAR, TAR);
        hm.put(SAND, TAR);
        hm.put(BLOOD, BLOOD);
        hm.put(MAGMA, AIR);
        hm.put(MANA, AIR);
        transform.put(BLOOD, hm);

        //magma
        hm = new HashMap<>();
        hm.put(WATER, SAND);
        hm.put(AIR, SAND);
        hm.put(ACID, TAR);
        hm.put(TAR, ACID);
        hm.put(SAND, TAR);
        hm.put(BLOOD, MANA);
        hm.put(MAGMA, MAGMA);
        hm.put(MANA, ACID);
        transform.put(MAGMA, hm);

        //mana
        hm = new HashMap<>();
        hm.put(WATER, MAGMA);
        hm.put(AIR, TAR);
        hm.put(ACID, BLOOD);
        hm.put(TAR, ACID);
        hm.put(SAND, AIR);
        hm.put(BLOOD, SAND);
        hm.put(MAGMA, WATER);
        hm.put(MANA, MANA);
        transform.put(MANA, hm);
    }

    private Element(String name, String adjective, String presentVerb, String pastVerb, SColor color) {
        this.name = name;
        this.adjective = adjective;
        this.presentVerb = presentVerb;
        this.pastVerb = pastVerb;
        this.color = color;
    }

    public Element combine(Element other) {
        return transform.get(this).get(other);
    }

    /**
     * Will return a random element of all possible elements except "NONE".
     *
     * @return
     */
    public static Element getRandomElement(IRNG rng) {
        return transform.randomKey(rng);
    }

}
