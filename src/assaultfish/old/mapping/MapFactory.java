package assaultfish.old.mapping;

import assaultfish.mapping.MapCell;
import assaultfish.mapping.Map;
import assaultfish.old.physical.Creature;
import assaultfish.old.physical.CreatureFactory;
import assaultfish.old.physical.Furniture;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidgrid.util.BasicRadiusStrategy;
import squidpony.squidmath.RNG;

/**
 * Creates and maintains caches of maps.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class MapFactory {

    public static final String[] FIREBASE_WHITE = new String[]{
        "-------------------------------------------",
        "--------------------------UUUUUUUUU--------",
        "--------------------------U.......U--------",
        "--------------------------U..#.#..U--------",
        "--------------------------U..#.#..U--------",
        "--------------------------UU..W..UU--------",
        "---------------------------U.....U---------",
        "------------------UUUUUUUUUU.....UUUU------",
        "-----------------UU........u........U------",
        "-----------------U....uW.......X.u###------",
        "-----------------U....u....#.#....##-------",
        "---------UUUUUUUU#UW.uu...#####.W.####-----",
        "---------U.W......W.X..W..#........u.#-----",
        "---------U..#UUUU##uu....##..u.W.....#-----",
        "---------U..U----U.W......#.Wu..uuuW.#-----",
        "---------U..#UUUU#uu....u##uuu..u....#-----",
        "---------U.W.........W...W......uW...#-----",
        "--------UU..................W...###.W#-----",
        "--------U....####.W.u....u##uuu..#...#-----",
        "----UUUUU#W.##########.W.###...W.#...#-----",
        "---UU............W........##.....#X..#-----",
        "--UU.u....W............uu.###.W####..####--",
        "-UU...W................W..#....W...W....#--",
        "-UU.u...u...UUUUUUUUUU#..##......u...uu.#--",
        "-U..u...U................#########...uW.##-",
        "-U.x.X.UU..W...........W.......W.W.W.....#-",
        "-U..W..UU...##...###.....#########W.W#uu##-",
        "-U.u..UUUU...UUUUUUUUU..######...#...W..#--",
        "-U.u..UUUU..#U-------UW.######.W..W..X..#--",
        "-U...W.....W.U-------U...#####...#......#--",
        "-UU.Xu..W....U-------U#..#####...########--",
        "--UU.....#..WUUUUUUUUU#W.#...#.X.#---------",
        "---UUUUUU#....###..W.....W...W...#---------",
        "------U....W..W.......W..#.W.#...#---------",
        "--################################---------",
        "-------------------------------------------"
    };
    public HashMap<String, Furniture> furnitures = new HashMap<>();
    public CreatureFactory creatureFactory = new CreatureFactory();
    private RNG rng = new RNG();

    public MapFactory() {
        creatureFactory.instantiateDefaultCreatures();
    }

    /**
     * Returns a default map for testing purposes.
     *
     * @return
     */
    public Map defaultMap(String[] blueprint) {
        int width = blueprint[0].length(), height = blueprint.length;
        MapCell[][] layout = new MapCell[width][height];
        Map map = new Map();
        Creature creature;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                char c = blueprint[y].charAt(x);
                switch (c) {//check for special locations
                    case 'x'://player spawn
                        c = '.';//change to plain ground
                        map.playerStart = new Point(x, y);
                        break;
                    case 'X'://enemy spawn
                        c = '.';//change to plain ground
                        map.spawns.add(new Point(x, y));
                    //spawns are also waypoints, so fall through
                    case 'W'://place a waypoint
                        c = '.';
                        layout[x][y] = buildFurnitureCell(c);
                        break;
                    case 'g'://place a geth hunter manually
                        c = '.';//change to plain ground
                        creature = creatureFactory.getCreature("geth hunter");
                        map.creatures.add(creature);
                        layout[x][y] = buildFurnitureCell(c);
                        layout[x][y].creature = creature;
                        creature.location = new Point(x, y);
                        break;
                    case '1'://place a geth hunter manually
                        c = '.';//change to plain ground
                        creature = creatureFactory.getCreature("geth trooper");
                        map.creatures.add(creature);
                        layout[x][y] = buildFurnitureCell(c);
                        layout[x][y].creature = creature;
                        creature.location = new Point(x, y);
                        break;
                    case 'B'://place a geth hunter manually
                        c = '.';//change to plain ground
                        creature = creatureFactory.getCreature("brute");
                        map.creatures.add(creature);
                        layout[x][y] = buildFurnitureCell(c);
                        layout[x][y].creature = creature;
                        creature.location = new Point(x, y);
                        break;
                }
                if (layout[x][y] == null) {//only create if not created in special case above
                    layout[x][y] = buildFurnitureCell(c);
                }
            }
        }
        map.setLayout(layout);

        return map;
    }

    /**
     * Attempts to spawn the given number of creatures.
     *
     * Because they will be placed randomly within range of a spawn point, the full number is
     * unlikely to be reached.
     *
     * @param map
     * @param quantity
     */
    public void spawn(Map map, Creature[] creatures, Point avoid) {
        Point location;
        ArrayList<Point> possibles = new ArrayList<>(map.spawns);
        do {
            location = possibles.get(rng.nextInt(possibles.size()));
            possibles.remove(location);
        } while (possibles.size() > 1 && BasicRadiusStrategy.CIRCLE.radius(avoid.x, avoid.y, location.x, location.y) < 15);
        for (Creature c : creatures) {
            boolean placed = false;
            while (!placed) {
                int x = rng.between(location.x - 5, location.x + 5);
                int y = rng.between(location.y - 5, location.y + 5);
                if (x >= 0 && x < map.width && y >= 0 && y < map.height && map.map[x][y].creature == null && !map.map[x][y].furniture.movementBlocking) {
                    c.location = new Point(x, y);
                    map.map[x][y].creature = c;
                    map.creatures.add(c);
                    placed = true;
                }
            }
        }
    }

    /**
     * Builds a cell based on the character in the map.
     *
     * @param c
     * @return
     */
    private MapCell buildFurnitureCell(char c) {
        float resistance = 0f, scent = 0f, sound = 0f;//default is transparent
        SColor color;
        String name = "unknown";
        String description = "Beyond the ken of mortal beings.";
        boolean sightBlocking = false, movementBlocking = false, hoppable = false;
        switch (c) {
            case '.'://stone ground
                color = SColor.SLATE_GRAY;
                name = "ground";
                description = "Normal ground.";
                break;
            case ','://pathway
                color = SColor.STOREROOM_BROWN;
                c = '.';
                name = "dirt";
                description = "Bare dirt.";
                break;
            case 'c':
                color = SColor.SEPIA;
                name = "chair";
                description = "A common  synth-rubber chair.";
                movementBlocking = true;
                hoppable = true;
                break;
            case 'u':
                color = SColor.BRONZE;
                name = "barrier";
                description = "A low barrier. Can be hopped over.";
                movementBlocking = true;
                hoppable = true;
                break;
            case 'U':
                color = SColorFactory.lighter(SColor.SILVER_GREY);
                name = "low wall";
                description = "A wall low enough to see over but not climb over.";
                movementBlocking = true;
                break;
            case '/':
                color = SColor.BROWNER;
                name = "open door";
                description = "An open door.";
                break;
            case '≈':
                color = SColor.AZUL;
                c = '~';
                name = "deep water";
                description = "Deep water.";
                movementBlocking = true;
                break;
            case '<':
            case '>':
                color = SColor.SLATE_GRAY;
                name = "stairs";
                description = "A stairway which is blocked with detritus.";
                sightBlocking = true;
                movementBlocking = true;
                break;
            case 't':
                color = SColor.BROWNER;
                name = "table";
                description = "A standard plasti-wood table.";
                sightBlocking = false;
                movementBlocking = true;
                resistance = 0.3f;
                break;
            case 'T':
                color = SColor.FOREST_GREEN;
                name = "tree";
                description = "A large tree.";
                sightBlocking = true;
                movementBlocking = true;
                resistance = 0.7f;
                break;
            case 'E':
                color = SColor.SILVER;
                name = "shelf";
                description = "A shelving unit.";
                sightBlocking = true;
                movementBlocking = true;
                resistance = 0.8f;
                break;
            case 'S':
                color = SColor.BREWED_MUSTARD_BROWN;
                name = "statue";
                description = "An artistic representation of something abstract.";
                sightBlocking = true;
                resistance = 0.9f;
                break;
            case '#':
                color = SColor.IRON;
                name = "wall";
                description = "A standard prefab wall.";
                resistance = 1f;
                sightBlocking = true;
                movementBlocking = true;
                break;
            case '+':
                color = SColor.BROWNER;
                name = "closed door";
                description = "A closed door.";
                sightBlocking = true;
                movementBlocking = true;
                resistance = 1f;
                break;
            case '-':
                color = SColor.DARK_RED_DYE;
                name = "open space";
                description = "Nothing but air.";
                sightBlocking = false;
                movementBlocking = true;
                resistance = 1f;
                break;
            default://opaque items
                resistance = 1f;//unknown is opaque
                color = SColor.DEEP_PINK;
                sightBlocking = false;
                movementBlocking = true;
        }
        MapCell ret = new MapCell();
        if (!furnitures.containsKey(name)) {
            addFurniture(name, description, c, color, SColorFactory.blend(SColor.BLACK, SColor.WHITE, resistance), movementBlocking, sightBlocking, hoppable, scent, sound);
        }
        ret.furniture = furnitures.get(name);
        return ret;
    }

    public void addFurniture(String name, String description, char symbol, SColor color, SColor colorResistance, boolean movementBlocking, boolean sightBlocking, boolean hoppable, float scentResistance, float soundResistance) {
        Furniture furn = new Furniture();
        furn.name = name;
        furn.description = description;
        furn.symbol = symbol;
        furn.color = color;
        furn.movementBlocking = movementBlocking;
        furn.sightBlocking = sightBlocking;
        furn.hoppable = hoppable;
        furn.resistances.put("scent", scentResistance);
        furn.resistances.put("sound", soundResistance);//mostly blocks sound, but not entirely
        furnitures.put(furn.name, furn);
    }
}
