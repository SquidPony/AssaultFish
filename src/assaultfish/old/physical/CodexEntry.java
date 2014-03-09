package assaultfish.old.physical;

import assaultfish.physical.Item;
import squidpony.squidcolor.SColor;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class CodexEntry extends Item{
    public boolean known = false;
    public Class type;

    public CodexEntry(char symbol,SColor color, String name, String description, Class type, boolean known){
        this.symbol = symbol;
        this.color = color;
        this.name = name;
        this.description = description;
        this.type = type;
        this.known = known;
    }
}
