package assaultfish.mapping;

import assaultfish.old.physical.Creature;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import squidpony.squidgrid.fov.FOVTranslator;
import squidpony.squidgrid.fov.ShadowFOV;
import squidpony.squidutility.graph.PointGraph;
import squidpony.squidutility.graph.Vertex;

/**
 * This is a map for the turn based roguelike portion of the game.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Map {

    public MapCell[][] map;
    public boolean[][][][] visibility;//marks which areas are visible from each map square
    public ArrayList<Creature> creatures = new ArrayList<>();
    public Point playerStart;
    public ArrayList<Point> spawns = new ArrayList<>();
    public int width, height;
    public PointGraph graph;

    public void setLayout(MapCell[][] layout) {
        map = layout;
        width = layout.length;
        height = layout[0].length;

        graph = new PointGraph();

        float[][] sightBlocking;
        sightBlocking = new float[width][height];
        visibility = new boolean[width][height][][];
        FOVTranslator fov = new FOVTranslator(new ShadowFOV());
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (map[x][y].feature.opacity >= 1f) {
                    sightBlocking[x][y] = 1f;
                } else {
                    sightBlocking[x][y] = 0f;
                    graph.addVertex(new Vertex(new Point(x, y)));
                }
            }
        }

        //place appropriately
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                fov.calculateFOV(sightBlocking, x, y, Math.max(width, height));
                visibility[x][y] = fov.getBooleanArray();
            }
        }

        //make bi-directional
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int x2 = 0; x2 < width; x2++) {
                    for (int y2 = 0; y2 < height; y2++) {
                        visibility[x][y][x2][y2] |= visibility[x2][y2][x][y];
                    }
                }
            }
        }

        graph.calculateEdges(sightBlocking);
    }

    public boolean isVisible(int startx, int starty, int endx, int endy) {
        return visibility[startx][starty][endx][endy] || visibility[endx][endy][startx][starty];
    }

    /**
     * Returns the closest waypoint, using a circular radius strategy.
     *
     * @param from
     * @param to
     * @return
     */
    public Point getClosestWaypoint(Point from, Point to) {

        LinkedList<Vertex> path = graph.getDijkstraPath(from, to);
        if (path == null) {
            return to;//no waypoints between targets
        }

        Point previous = path.get(0).point;
        for (Vertex v : path) {
            if (visibility[from.x][from.y][v.point.x][v.point.y]) {
                previous = v.point;
            } else {
                return previous;//go for the one furthest away that's visible
            }
        }
        return previous;
    }
}
