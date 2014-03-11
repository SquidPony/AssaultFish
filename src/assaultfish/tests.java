package assaultfish;

import java.awt.Color;
import squidpony.squidcolor.SColor;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class tests {
public static void main(String... args){
    SColor base = SColor.TRANSPARENT;
    System.out.println(SColor.BLACK.equals(base));
    System.out.println(Color.BLACK.equals(base));
    System.out.println(SColor.TRANSPARENT.equals(base));
    System.out.println(new Color(0xFF000000, true).equals(base));
    System.out.println(new Color(0x00000000, true).equals(base));
}
}
