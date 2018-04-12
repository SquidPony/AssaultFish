package assaultfish.physical;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import squidpony.squidmath.GWTRNG;

import java.util.ArrayList;

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
        color = element.color.cpy().lerp(Color.WHITE, 0.2f);
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
    public static void initSymbols(BitmapFont font) {
        ArrayList<String> symbols = new ArrayList<>();
        StringBuilder symbolChoices = new StringBuilder();
        BitmapFont.BitmapFontData data = font.getData();
        for (char c : group1.toCharArray()) {
            if (data.hasGlyph(c)) {
                symbolChoices.append(c);
            }
        }
        if ("".equals(symbolChoices.toString())) {
            symbolChoices = new StringBuilder("f");
        }
        symbols.add(symbolChoices.toString());

        symbolChoices.setLength(0);
        for (char c : group2.toCharArray()) {
            if (data.hasGlyph(c)) {
                symbolChoices.append(c);
            }
        }
        if ("".equals(symbolChoices.toString())) {
            symbolChoices = new StringBuilder("i");
        }
        symbols.add(symbolChoices.toString());

        symbolChoices.setLength(0);
        for (char c : group3.toCharArray()) {
            if (data.hasGlyph(c)) {
                symbolChoices.append(c);
            }
        }
        if ("".equals(symbolChoices.toString())) {
            symbolChoices = new StringBuilder("s");
        }
        symbols.add(symbolChoices.toString());

        symbolChoices.setLength(0);
        for (char c : group4.toCharArray()) {
            if (data.hasGlyph(c)) {
                symbolChoices.append(c);
            }
        }
        if ("".equals(symbolChoices.toString())) {
            symbolChoices = new StringBuilder("h");
        }
        symbols.add(symbolChoices.toString());

        GWTRNG rng = new GWTRNG();
        String s = rng.getRandomElement(symbols);
        symbols.remove(s);
        small = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols);
        symbols.remove(s);
        medium = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols);
        symbols.remove(s);
        large = "" + s.charAt(rng.nextInt(s.length()));

        s = rng.getRandomElement(symbols);
        symbols.remove(s);
        giant = "" + s.charAt(rng.nextInt(s.length()));
    }

}
