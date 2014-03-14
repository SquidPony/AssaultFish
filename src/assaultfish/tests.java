package assaultfish;

import squidpony.squidgrid.util.Direction;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class tests {

    public static void main(String... args) {
        for (Direction dir : Direction.values()) {
            System.out.println("    " + dir + ": " + Direction.getDirection(dir.deltaX, dir.deltaY) + "");
        }
    }
}
