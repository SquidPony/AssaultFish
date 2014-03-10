package assaultfish.mapping;

import assaultfish.physical.Creature;
import assaultfish.physical.Element;
import assaultfish.physical.Terrain;
import squidpony.squidmath.PerlinNoise;

/**
 * This is a map for the fishing sim portion of the game.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class FishingMap {

    private Element element;
    private Terrain shore;
    private int surface, waveHeight;
    private int bobberX, bobberY, hookX, hookY;
    private FishingCell[][] map;
    private int width, height;

    /**
     * Causes one time step of animation to pass
     *
     * @param time size of the time step to take
     */
    public void timeStep(long time) {
        for (int x = 0; x < width; x++) {
            double scale = PerlinNoise.noise(x, time);
            scale += 1;//change to [0,2]
            scale *= 0.5;//change back to [0,1]
            scale *= waveHeight + 1;//change to [0, possible height+1] to account for 1 almost never happening

            for (int y = waveHeight; y <= surface; y++) {
                if (!map[x][y].isTerrain) {
                    
                }
            }
        }
    }

    /**
     * Describes the contents of a cell applicable only to the fishing map.
     */
    private class FishingCell {

        boolean isTerrain;
        Creature creature;
    }
}
