package assaultfish.fishing;

import assaultfish.physical.Element;
import assaultfish.physical.Item;
import assaultfish.physical.Size;
import java.awt.Font;
import java.util.ArrayList;
import squidpony.squidmath.RNG;
import squidpony.squidutility.SCollections;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Fish extends Item {

    public static final String fishSymbols = "ᾷᾶᾴᾳᾲᾱᾰάὰᾇᾆᾅᾄᾃᾂᾁᾀἇἆἅἄἃἂἁἀαά",
            group1 = "ᾷᾴᾲᾅᾄᾃᾂᾁᾀ",
            group2 = "ἇἆᾇᾆ",
            group3 = "ᾶᾱᾰἅἄἃἂἁἀά",
            group4 = "ᾳαάὰ";
    public static String small, medium, large, giant;

    public Fish(Size size, Element element) {
        name = element.adjective;
        symbol = "X";
        switch (size) {
            case SMALL:
                name += " guppy";
                symbol = small;
                break;
            case MEDIUM:
                name += " eel";
                symbol = medium;
                break;
            case LARGE:
                name += " squid";
                symbol = large;
                break;
            case GIANT:
                name += " dolphin";
                symbol = giant;
                break;
        }

    }

    /**
     * Sets up randomized symbols for the fish sizes and ensures that the font can display them.
     * Falls back to standard letters if needed.
     *
     * @param font
     */
    public static void initSymbols(Font font) {
        ArrayList<String> symbols = new ArrayList<>();
        String symbolChoices = "";
        for (char c : group1.toCharArray()) {
            if (font.canDisplay(c)) {
                symbolChoices += c;
            }
        }
        if ("".equals(symbolChoices)) {
            symbolChoices = "f";
        }
        symbols.add(symbolChoices);

        symbolChoices = "";
        for (char c : group2.toCharArray()) {
            if (font.canDisplay(c)) {
                symbolChoices += c;
            }
        }
        if ("".equals(symbolChoices)) {
            symbolChoices = "i";
        }
        symbols.add(symbolChoices);

        symbolChoices = "";
        for (char c : group3.toCharArray()) {
            if (font.canDisplay(c)) {
                symbolChoices += c;
            }
        }
        if ("".equals(symbolChoices)) {
            symbolChoices = "s";
        }
        symbols.add(symbolChoices);

        symbolChoices = "";
        for (char c : group4.toCharArray()) {
            if (font.canDisplay(c)) {
                symbolChoices += c;
            }
        }
        if ("".equals(symbolChoices)) {
            symbolChoices = "h";
        }
        symbols.add(symbolChoices);

        RNG rng = new RNG();
        String s = SCollections.getRandomElement(symbols);
        symbols.remove(s);
        small = "" + s.charAt(rng.nextInt(s.length()));

        s = SCollections.getRandomElement(symbols);
        symbols.remove(s);
        medium = "" + s.charAt(rng.nextInt(s.length()));

        s = SCollections.getRandomElement(symbols);
        symbols.remove(s);
        large = "" + s.charAt(rng.nextInt(s.length()));

        s = SCollections.getRandomElement(symbols);
        symbols.remove(s);
        giant = "" + s.charAt(rng.nextInt(s.length()));
    }

}
