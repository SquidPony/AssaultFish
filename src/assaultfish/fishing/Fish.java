package assaultfish.fishing;

import assaultfish.physical.Item;
import squidpony.squidcolor.SColor;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Fish extends Item {

    public static String fishSymbols = "ᾷᾶᾴᾳᾲᾱᾰάὰᾇᾆᾅᾄᾃᾂᾁᾀἇἆἅἄἃἂἁἀαά";

    public Fish(char c) {
        super(fishSymbols, fishSymbols, c, SColor.GREEN_YELLOW);
    }
}
