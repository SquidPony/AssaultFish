package assaultfish.physical;

import squidpony.squidmath.RNG;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public enum Size {

    SMALL, MEDIUM, LARGE, GIANT;

    public static Size getRandomSize() {
        RNG rng = new RNG();
        return new Size[]{SMALL, MEDIUM, LARGE, GIANT}[rng.nextInt(4)];
    }
}
