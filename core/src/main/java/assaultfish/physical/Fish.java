package assaultfish.physical;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.awt.Font;
import java.util.ArrayList;
import squidpony.squidgrid.gui.gdx.SquidColorCenter;
import squidpony.squidmath.RNG;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Fish extends Item {

    private static final SquidColorCenter COLORS = new SquidColorCenter();

    public static final String fishSymbols = "ᾷᾶᾴᾳᾲᾱᾰάὰᾇᾆᾅᾄᾃᾂᾁᾀἇἆἅἄἃἂἁἀαά",
            group1 = "ᾷᾴᾲᾅᾄᾃᾂᾁᾀ",
            group2 = "ἇἆᾇᾆ",
            group3 = "ᾶᾱᾰἅἄἃἂἁἀά",
            group4 = "ᾳαάὰ";
    public static String small, medium, large, giant;
    public Size size;

    public Fish(Size size, Element element) {
        this.size = size;
        this.element = element;
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
        color = COLORS.lighter(element.color);
    }

    public static String symbol(Size size) {

        switch (size) {
            case SMALL:
                return small;
            case MEDIUM:
                return medium;
            case LARGE:
                return large;
            case GIANT:
                return giant;
            default:
                return "x";
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
        String s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        small = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        medium = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        large = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        giant = "" + s.charAt(rng.nextInt(s.length()));
    }

    public static void initSymbols(BitmapFont font) {
        ArrayList<String> symbols = new ArrayList<>();
        String symbolChoices = "";
        for (char c : group1.toCharArray()) {
            if (font.getData().hasGlyph(c)) {
                symbolChoices += c;
            }
        }
        if (symbolChoices.isEmpty()) {
            symbolChoices = "f";
        }
        symbols.add(symbolChoices);

        symbolChoices = "";
        for (char c : group2.toCharArray()) {
            if (font.getData().hasGlyph(c)) {
                symbolChoices += c;
            }
        }
        if (symbolChoices.isEmpty()) {
            symbolChoices = "i";
        }
        symbols.add(symbolChoices);

        symbolChoices = "";
        for (char c : group3.toCharArray()) {
            if (font.getData().hasGlyph(c)) {
                symbolChoices += c;
            }
        }
        if (symbolChoices.isEmpty()) {
            symbolChoices = "s";
        }
        symbols.add(symbolChoices);

        symbolChoices = "";
        for (char c : group4.toCharArray()) {
            if (font.getData().hasGlyph(c)) {
                symbolChoices += c;
            }
        }
        if (symbolChoices.isEmpty()) {
            symbolChoices = "h";
        }
        symbols.add(symbolChoices);

        RNG rng = new RNG();
        String s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        small = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        medium = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        large = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols.toArray(new String[0]));
        symbols.remove(s);
        giant = "" + s.charAt(rng.nextInt(s.length()));
    }

}
