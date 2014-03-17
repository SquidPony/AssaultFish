package assaultfish;

import assaultfish.mapping.MapCell;
import assaultfish.physical.BallisticsSolver;
import assaultfish.physical.Creature;
import assaultfish.physical.Element;
import assaultfish.physical.Fish;
import assaultfish.physical.Size;
import assaultfish.physical.Terrain;
import assaultfish.physical.TerrainFeature;
import assaultfish.physical.Treasure;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import javafx.scene.media.MediaException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputAdapter;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidgrid.fov.FOVTranslator;
import squidpony.squidgrid.fov.ShadowFOV;
import squidpony.squidgrid.gui.awt.TextCellFactory;
import squidpony.squidgrid.gui.swing.SwingPane;
import squidpony.squidgrid.los.LOSSolver;
import squidpony.squidgrid.util.BasicRadiusStrategy;
import squidpony.squidgrid.util.Direction;
import static squidpony.squidgrid.util.Direction.*;
import squidpony.squidgrid.util.RadiusStrategy;
import squidpony.squidmath.Bresenham;
import squidpony.squidmath.PerlinNoise;
import squidpony.squidmath.RNG;
import squidpony.squidsound.SoundManager;
import squidpony.squidutility.SCollections;
import squidpony.squidutility.graph.PointGraph;

/**
 * This class starts up the game.
 *
 * @author Eben Howard
 */
public class AssaultFish {

    private static final String version = "1.3";

    private static final double widthScale = 1.2,
            heightScale = 1.2;
    private static final int width = 80,
            height = 40,
            fishHeight = (int) ((height - 3) * heightScale),
            fishWidth = (int) (width * widthScale),
            fontSize = 20,
            largeTextScale = 4,
            liquidHeight = largeTextScale * 4,
            terrainWidth = largeTextScale * 2 + 1,
            inventoryHeight = 6,
            maxHealth = 6,
            healthX = width - 15;
    private static final int maxFish = 6;
    private static final int overlayAlpha = 100;
    private static Font font = new Font("Arial Unicode MS", Font.PLAIN, fontSize);
    private static volatile long outputEndTime;
    private static final Rectangle helpIconLocation = new Rectangle(width - 5, 1, 4, 1),
            muteIconLocation = new Rectangle(width - 5, 2, 4, 1),
            exitIconLocation = new Rectangle(width - 5, 3, 4, 1);

//    private static final String fishingPole = "🎣",//fishing pole and fish
//            whale = "🐋",//whale
//            octopus = "🐙",//octopus
//            //fish = "🐟",//fish
//            tropicalFish = "🐠",//tropical fish
//            blowfish = "🐡",//blowfish
//            spoutingWhale = "🐳",//spouting whale
//            gemstone = "💎",//gemstone
//            moneyBag = "💰";//money bag
    private final FOVTranslator fov = new FOVTranslator(new ShadowFOV());
    private final LOSSolver los = new squidpony.squidgrid.los.BresenhamLOS();
    private final RNG rng = new squidpony.squidmath.RNG();

    private TextCellFactory textFactory;
    private TextCellFactory fishText;

    private JFrame frame;
    private SwingPane mapPanel, outputPanel, meterPanel, fishingViewPanel,
            fishPane, largeTextPane, helpPane, fishThrowingPanel,
            winPane, diePane;
    private JPanel fishingMasterPanel;
    private JLayeredPane layers;
    private MeterListener meterListener;
    private FishMouse fishMouse;
    private MapMouse mapMouse;
    private FishInventoryMouse inventoryMouse;

    private Creature player;

    private ArrayList<Creature> monsters = new ArrayList<>();
    private ArrayList<Treasure> treasuresFound = new ArrayList<>();
    private MapCell[][] map;

    private Fish selectedFish = null;
    private SwingPane overlayPanel;
    private final Point overlayLocation = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);

    private final TreeMap<Element, TreeMap<Size, Integer>> fishInventory = new TreeMap<>();
    private SwingPane fishInventoryPanel;

    private volatile boolean casting = false, canCast = false, canClick = true;
    private double castingStrength = 0.4;

    /* ------------------ From FishingPanel ------------------------- */
    private boolean[][] terrainMap;
    private boolean[][] liquidMap;
    private Fish[][] fishMap;
    private List<Fish> fishes = new LinkedList<>();
    private double wind = 10, gravity = 20;
    private Terrain terrain;
    private Element element;
    private char bobber = '●',//O•☉✆✇♁┢Ø∅∮⊕⊖⊗⊘⊙⊚⊛⊜⊝Ⓧ◍◎●◐◑◒◓◔◕☯☮☻☺☹✪➊➋➌➍➎➏➐➑➒➓〄〇〶
            hook = 'J',
            wall = '#';
    private Point bobberLocation;
    private Point hookLocation;
    private SColor lineColor = SColor.BURNT_BAMBOO,
            bobberColor = SColor.SCARLET,
            hookColor = SColor.BRASS,
            skyColor = SColor.ALICE_BLUE,
            playerColor = SColor.BETEL_NUT_DYE;
    private volatile boolean nowFishing = false;

    /*  --------------------- Mapping -----------------------------*/
    private PointGraph graph = new PointGraph();
    private float[][] sightBlocking = new float[width][height];

    /*  ------------------------ Sound -------------------------*/
    private static SoundManager sound;
    private static final String SOUND_PREF = "Sound Pref";
    private static Preferences prefs;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        prefs = Preferences.userNodeForPackage(assaultfish.AssaultFish.class);
        String soundOn = prefs.get(SOUND_PREF, "on"); // "a string"

        try {
            sound = new SoundManager();
            sound.loadMediaResources(new File("sound"), true);
            if (!soundOn.equals("on")) {
                sound.setMusicVolume(0);
            }
            sound.playMusic("Eden");
        } catch (MediaException e) {
//            System.err.println(e.getLocalizedMessage());
        }

        Fish.initSymbols(font);
        if (SColorFactory.pallet("meter") == null) {
            ArrayList<SColor> pallet = SColorFactory.asGradient(SColor.RED, SColor.ORANGE);
            pallet.addAll(SColorFactory.asGradient(SColor.ORANGE, SColor.YELLOW));
            pallet.addAll(SColorFactory.asGradient(SColor.YELLOW, SColor.ELECTRIC_GREEN));
            SColorFactory.addPallet("meter", pallet);
        }
        new AssaultFish().go();
    }

    /**
     * Starts the game.
     */
    private void go() {
        //dummy up starting inventory
        for (Element e : Element.values()) {
            fishInventory.put(e, new TreeMap<Size, Integer>());
            for (Size s : Size.values()) {
                fishInventory.get(e).put(s, rng.nextInt(2));
//                fishInventory.get(e).put(s, 0);
            }
        }

        player = new Creature(Creature.PLAYER);
        player.color = SColor.CORNFLOWER_BLUE;

        initializeFrame();
        initializeFishInventory();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        showHelp();

        createMap();
        updateMap();
//        printOut("Welcome to Assault Fish!    This is version " + version);
        flipMouseControl(true);

        frame.requestFocus();
    }

    private void restart() {
        frame.setVisible(false);
        AssaultFish.main(new String[]{});
        frame.removeAll();
        frame = null;
    }

    private void showHelp() {
        if (helpPane == null) {
            helpPane = new SwingPane(width, height, textFactory);
            helpPane.erase();
            SColor fade = SColor.DARK_GRAY;
            SColor heading = SColor.RED_PIGMENT;
            SColor command = SColor.SCHOOL_BUS_YELLOW;
            SColor sc = new SColor(fade.getRed(), fade.getGreen(), fade.getBlue(), 150);
            for (int x = 0; x < helpPane.getGridWidth(); x++) {
                helpPane.clearCell(x, 0, sc);
                helpPane.clearCell(x, 1, sc);
                helpPane.clearCell(x, helpPane.getGridHeight() - 1, sc);
                helpPane.clearCell(x, helpPane.getGridHeight() - 2, sc);
            }
            for (int y = 0; y < helpPane.getGridHeight(); y++) {
                helpPane.clearCell(0, y, sc);
                helpPane.clearCell(1, y, sc);
                helpPane.clearCell(helpPane.getGridWidth() - 1, y, sc);
                helpPane.clearCell(helpPane.getGridWidth() - 2, y, sc);
            }
            sc = new SColor(fade.getRed(), fade.getGreen(), fade.getBlue(), 240);
            for (int x = 2; x < helpPane.getGridWidth() - 2; x++) {
                for (int y = 2; y < helpPane.getGridHeight() - 2; y++) {
                    helpPane.clearCell(x, y, sc);
                }
            }

            String text;
            int x;
            int y = 3;
            int left = 5;

            text = "ASSAULT FISH  v" + version;
            x = (helpPane.getGridWidth() - text.length()) / 2;//centered
            helpPane.placeHorizontalString(x, y, text, heading, sc);
            y += 2;

            text = "Your peaceful life as a fisherman has come to an end.";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "A horde of elementals has decended upon the land and it is your duty";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "to fight them any way you can! And that means using the fishing skills";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "of your forefathers to fish from the many local elemental pools and";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "strategically throw your explosive catch at the enemy!";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text);
            y += 3;

            text = "Main Map Controls";
            x = (helpPane.getGridWidth() - text.length()) / 2;//centered
            helpPane.placeHorizontalString(x, y, text, heading, sc);
            y += 2;

            text = "Without a fish selected for throwing:";
            x = (helpPane.getGridWidth() - text.length()) / 2;//centered
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Left click";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - move";
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Ctrl-Left click";
            x = left;
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - examine";
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Left click on fish";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - select fish";
            helpPane.placeHorizontalString(x, y, text);
            y += 3;

            text = "With a fish selected for throwing:";
            x = (helpPane.getGridWidth() - text.length()) / 2;//centered
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Left click";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - throw fish";
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Right click";
            x = left;
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - deselect fish";
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Left click on fish";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - select a new fish";
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Left click on the slected fish";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - deselect fish";
            helpPane.placeHorizontalString(x, y, text);
            y += 3;

            text = "Fishing Controls";
            x = (helpPane.getGridWidth() - text.length()) / 2;//centered
            helpPane.placeHorizontalString(x, y, text, heading, sc);
            y += 1;

            text = "Left click";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - start casting meter / cast";
            helpPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "Right click";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, command, sc);
            x += text.length();
            text = " - stop fishing";
            helpPane.placeHorizontalString(x, y, text);
            y += 3;

            text = "Elemental enemies are destroyed by";
            x = (helpPane.getGridWidth() - text.length()) / 2;//centered
            helpPane.placeHorizontalString(x, y, text, heading, sc);
            y += 1;

            text = "Acid";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, Element.ACID.color, sc);
            x += text.length();
            text = " - Sand";
            helpPane.placeHorizontalString(x, y, text, Element.SAND.color, sc);
            y += 1;

            text = "Air";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, Element.AIR.color, sc);
            x += text.length();
            text = " - Mana";
            helpPane.placeHorizontalString(x, y, text, Element.MANA.color, sc);
            y += 1;

            text = "Blood";
            x = left;//left justified
            helpPane.placeHorizontalString(x, y, text, Element.BLOOD.color, sc);
            x += text.length();
            text = " - Tar";
            helpPane.placeHorizontalString(x, y, text, Element.TAR.color, sc);
            y -= 2;

            text = "Magma";
            x = left + 20;
            helpPane.placeHorizontalString(x, y, text, Element.MAGMA.color, sc);
            x += text.length();
            text = " - Water";
            helpPane.placeHorizontalString(x, y, text, Element.WATER.color, sc);
            y += 1;

            text = "Mana";
            x = left + 20;
            helpPane.placeHorizontalString(x, y, text, Element.MANA.color, sc);
            x += text.length();
            text = " - Blood";
            helpPane.placeHorizontalString(x, y, text, Element.BLOOD.color, sc);
            y += 1;

            text = "Sand";
            x = left + 20;
            helpPane.placeHorizontalString(x, y, text, Element.SAND.color, sc);
            x += text.length();
            text = " - Acid";
            helpPane.placeHorizontalString(x, y, text, Element.ACID.color, sc);
            y -= 2;

            text = "Tar";
            x = left + 40;
            helpPane.placeHorizontalString(x, y, text, Element.TAR.color, sc);
            x += text.length();
            text = " - Magma";
            helpPane.placeHorizontalString(x, y, text, Element.MAGMA.color, sc);
            y += 1;

            text = "Water";
            x = left + 40;
            helpPane.placeHorizontalString(x, y, text, Element.WATER.color, sc);
            x += text.length();
            text = " - Air";
            helpPane.placeHorizontalString(x, y, text, Element.AIR.color, sc);

            text = "--  press mouse button to continue --";
            x = (helpPane.getGridWidth() - text.length()) / 2;//centered
            y = height - 3;
            helpPane.placeHorizontalString(x, y, text, heading, sc);

            helpPane.refresh();
            layers.setLayer(helpPane, JLayeredPane.DRAG_LAYER);

            helpPane.addMouseListener(new MouseInputAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    layers.remove(helpPane);
                    layers.repaint();
                }

            });
        }
        layers.add(helpPane);
    }

    private void goFish() {
        nowFishing = true;
        selectedFish = null;
        updateFishInventoryPanel();
        fishes = new LinkedList<>();
        fishPane.erase();
        for (int i = 0; i < 40; i++) {
            fishes.add(new Fish(Size.SMALL, element));
        }
        for (int i = 0; i < 20; i++) {
            fishes.add(new Fish(Size.MEDIUM, element));
        }
        for (int i = 0; i < 10; i++) {
            fishes.add(new Fish(Size.LARGE, element));
        }
        for (int i = 0; i < 2; i++) {
            fishes.add(new Fish(Size.GIANT, element));
        }

        initFishingMap();
        initFish();
        initFishingDisplay();
        fishingMasterPanel.setVisible(true);
        layers.add(fishingMasterPanel);
        flipMouseControl(false);
        printOut("You are now fishing from " + terrain.name + " shore into the " + element.name + " lake.");
        canCast = true;
    }

    /**
     * This is the main game loop method that takes input and process the results. Right now it
     * doesn't loop!
     */
    private void runTurn() {
        updateMap();

        if (monsters.isEmpty()) {
            win();
        }

        checkForReactions();

//        //build djikstra map for pathfinding
//        for (int x = 0; x < width; x++) {
//            for (int y = 0; y < height; y++) {
//                if (map[x][y].feature != null && map[x][y].feature.opaque) {
//                    sightBlocking[x][y] = 1f;
//                } else {
//                    sightBlocking[x][y] = 0f;
//                    graph.addVertex(new Vertex(new Point(x, y)));
//                }
//            }
//        }
//        graph.calculateEdges(sightBlocking);
        moveAllMonsters();
        updateMap();
    }

    private void workClick(final int x, final int y) {
//        Thread thread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
        boolean success = tryToMove(Direction.getDirection(x - player.x, y - player.y));
        if (success) {
            runTurn();
        }
//            }
//        });
//        thread.start();
    }

    private void exiting() {
//        printOut("Thanks for playing, press any key to exit.");
//        keyListener.blockOnEmpty(false);
//        keyListener.flush();
//        while (!keyListener.hasNext()) {
//            Thread.yield();
//        }
        System.exit(0);
    }

    private void throwFish(int targetX, int targetY) {
        if (selectedFish == null) {
            return;
        }

        int n = fishInventory.get(selectedFish.element).get(selectedFish.size);
        if (n < 1) {
            selectedFish = null;
            return;
        }

        layers.remove(overlayPanel);

        RadiusStrategy strat = BasicRadiusStrategy.CIRCLE;
        int radius = 1;
        SColor c = selectedFish.color;
        switch (selectedFish.size) {
            case SMALL:
                radius = 2;
                break;
            case MEDIUM:
                radius = 3;
                break;
            case LARGE:
                radius = 5;
                break;
            case GIANT:
                radius = 9;
                break;
        }

        //find line taken by thrown fish
        Queue<Point> line = Bresenham.line2D(new Point(player.x, player.y), new Point(targetX, targetY));
        do {
            Point p = line.poll();
            targetX = p.x;
            targetY = p.y;
            fishThrowingPanel.erase();
            fishThrowingPanel.placeCharacter(targetX, targetY, selectedFish.symbol.charAt(0), selectedFish.color);
            fishThrowingPanel.refresh();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        } while (!line.isEmpty()
                && (map[targetX][targetY].creature == null || map[targetX][targetY].creature == player)
                && (map[targetX][targetY].feature == null || map[targetX][targetY].feature == TerrainFeature.BUSH));
        fishThrowingPanel.erase();
        fishThrowingPanel.refresh();

        boolean[][] modified = new boolean[width][height];

        for (double i = 0; i <= 1; i += 0.1) {
            double radiusMod = (radius + 0.1) * i;
            for (int x = (int) (targetX - radiusMod); x <= targetX + radiusMod; x++) {
                for (int y = (int) (targetY - radiusMod); y <= targetY + radiusMod; y++) {
                    if (strat.radius(targetX, targetY, x, y) <= radiusMod) {
                        if (x >= 0 && x < width && y >= 0 && y < height) {
                            if (!modified[x][y]) {
                                reactToElementChange(x, y, selectedFish.element);
                                fishThrowingPanel.placeCharacter(x, y, '*', selectedFish.color);
                                modified[x][y] = true;
                                mapPanel.placeCharacter(x, y, map[x][y].getSymbol().charAt(0), map[x][y].foregroundColor(), map[x][y].backgroundColor());
                            }
                        }
                    }
                }
            }
            fishThrowingPanel.refresh();
            updateMap();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }

        removeFish(selectedFish);
        fishThrowingPanel.erase();
        fishThrowingPanel.refresh();
        updateOverlay();
        layers.add(overlayPanel);
        updateMap();
        runTurn();
    }

    private void reactToElementChange(int x, int y, Element e) {
        //change terrain
        if (map[x][y].terrain.element == null || map[x][y].terrain.element.combine(selectedFish.element) != map[x][y].terrain.element) {
            boolean wasLake = map[x][y].terrain.lake;
            if (wasLake) {
                map[x][y].terrain = Terrain.makeElementalPool(map[x][y].terrain.element == null ? selectedFish.element : map[x][y].terrain.element.combine(selectedFish.element));
            } else {
                map[x][y].terrain = Terrain.makeElementalFloor(map[x][y].terrain.element == null ? selectedFish.element : map[x][y].terrain.element.combine(selectedFish.element), false);
            }
        }

        Creature c = map[x][y].creature;
        //check for creature destruction
        if (c != null && c != player) {
            switch (c.element) {
                case ACID:
                    if (e == Element.SAND) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
                case AIR:
                    if (e == Element.MANA) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
                case BLOOD:
                    if (e == Element.TAR) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
                case MAGMA:
                    if (e == Element.WATER) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
                case MANA:
                    if (e == Element.BLOOD) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
                case SAND:
                    if (e == Element.ACID) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
                case TAR:
                    if (e == Element.MAGMA) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
                case WATER:
                    if (e == Element.AIR) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                    break;
            }
        }

        c = map[x][y].creature;
        //check for creature change
        if (c != null && c != player) {
            map[x][y].creature = new Creature(c.name, c.health, c.symbol, c.element.combine(e));
            monsters.remove(c);
            monsters.add(map[x][y].creature);
        }

        //check for tree destruction
        TerrainFeature tf = map[x][y].feature;
        if (tf == TerrainFeature.TREE) {
            if (e == Element.MAGMA || e == Element.ACID) {
                map[x][y].feature = null;
            } else if (e == Element.MANA) {
                map[x][y].feature = TerrainFeature.BUSH;
            } else if (e == Element.TAR) {
                map[x][y].feature = TerrainFeature.STONE_WALL;
            }
        } else if (tf == TerrainFeature.BUSH) {
            if (e == Element.MAGMA || e == Element.ACID || e == Element.AIR) {
                map[x][y].feature = null;
            } else if (e == Element.BLOOD || e == Element.WATER) {
                map[x][y].feature = TerrainFeature.TREE;
            } else if (e == Element.TAR || e == Element.SAND) {
                map[x][y].feature = TerrainFeature.STONE_WALL;
            }
        } else if (tf == TerrainFeature.STONE_WALL) {
            if (e == Element.ACID) {
                map[x][y].feature = null;
            } else if (e == Element.BLOOD) {
                map[x][y].feature = TerrainFeature.BUSH;
            } else if (e == Element.MANA) {
                map[x][y].feature = TerrainFeature.TREE;
            }
        } else if (tf == null) {
            if (e == Element.MANA) {
                if (rng.nextDouble() < 0.05) {
                    map[x][y].feature = TerrainFeature.TREE;
                }
            } else if (e == Element.WATER || e == Element.BLOOD) {
                if (rng.nextDouble() < 0.05) {
                    map[x][y].feature = TerrainFeature.BUSH;
                }
            } else if (e == Element.TAR || e == Element.SAND) {
                if (rng.nextDouble() < 0.05) {
                    map[x][y].feature = TerrainFeature.STONE_WALL;
                }
            }
        }

        c = map[x][y].creature;
        tf = map[x][y].feature;
        if (c == player) {
            if (tf != null && tf.blocking) {
                die("You are crushed by the sudden appearance of a " + tf.name);
            } else if (map[x][y].terrain.blocking) {
                die("You don't survive the sudden appearance of a " + map[x][y].terrain.name);
            }
        }

    }

    private void win() {
        canClick = false;
        if (winPane == null) {
            winPane = new SwingPane(width, height, textFactory);
            winPane.erase();
            SColor fade = SColor.DARK_GRAY;
            SColor heading = SColor.RED_PIGMENT;
            SColor command = SColor.SCHOOL_BUS_YELLOW;
            SColor sc = new SColor(fade.getRed(), fade.getGreen(), fade.getBlue(), 150);
            for (int x = 0; x < winPane.getGridWidth(); x++) {
                winPane.clearCell(x, 0, sc);
                winPane.clearCell(x, 1, sc);
                winPane.clearCell(x, winPane.getGridHeight() - 1, sc);
                winPane.clearCell(x, winPane.getGridHeight() - 2, sc);
            }
            for (int y = 0; y < winPane.getGridHeight(); y++) {
                winPane.clearCell(0, y, sc);
                winPane.clearCell(1, y, sc);
                winPane.clearCell(winPane.getGridWidth() - 1, y, sc);
                winPane.clearCell(winPane.getGridWidth() - 2, y, sc);
            }
            sc = new SColor(fade.getRed(), fade.getGreen(), fade.getBlue(), 240);
            for (int x = 2; x < winPane.getGridWidth() - 2; x++) {
                for (int y = 2; y < winPane.getGridHeight() - 2; y++) {
                    winPane.clearCell(x, y, sc);
                }
            }

            String text;
            int x;
            int y = 3;
            int left = 5;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (diePane.getGridWidth() - text.length()) / 2;//centered
            diePane.placeHorizontalString(x, y, text, SColor.ELECTRIC_GREEN, sc);
            y += 2;

            text = "Your peaceful life as a fisherman has come to an end.";
            x = left;//left justified
            winPane.placeHorizontalString(x, y, text);
            y += 1;
            text = "Because you destroyed the elemental menace.";
            x = left;//left justified
            winPane.placeHorizontalString(x, y, text);
            y += 2;
            text = "Your fame and prowess are now legendary across the land! Your";
            x = left;//left justified
            winPane.placeHorizontalString(x, y, text);
            y += 1;
            text = "nights are filled with the shouts and laughter of all your";
            x = left;//left justified
            winPane.placeHorizontalString(x, y, text);
            y += 1;
            text = "friends and family.";
            x = left;//left justified
            winPane.placeHorizontalString(x, y, text);
            y += 2;
            text = "You live happily ever after.";
            x = left;//left justified
            winPane.placeHorizontalString(x, y, text);
            y += 1;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (winPane.getGridWidth() - text.length()) / 2;//centered
            y = height - 3;
            winPane.placeHorizontalString(x, y, text, SColor.ELECTRIC_GREEN, sc);

            winPane.refresh();
            layers.setLayer(winPane, JLayeredPane.DRAG_LAYER);

            final long readTime = System.currentTimeMillis() + 200;
            winPane.addMouseListener(new MouseInputAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    if (System.currentTimeMillis() > readTime) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            exiting();
                        } else {
                            restart();
                        }
                    }
                }

            });
        }
        layers.add(winPane);
    }

    private void die(String reason) {
        canClick = false;
        if (diePane == null) {
            diePane = new SwingPane(width, height, textFactory);
            diePane.erase();
            SColor fade = SColor.DARK_GRAY;
            SColor heading = SColor.RED_PIGMENT;
            SColor command = SColor.SCHOOL_BUS_YELLOW;
            SColor sc = new SColor(fade.getRed(), fade.getGreen(), fade.getBlue(), 150);
            for (int x = 0; x < diePane.getGridWidth(); x++) {
                diePane.clearCell(x, 0, sc);
                diePane.clearCell(x, 1, sc);
                diePane.clearCell(x, diePane.getGridHeight() - 1, sc);
                diePane.clearCell(x, diePane.getGridHeight() - 2, sc);
            }
            for (int y = 0; y < diePane.getGridHeight(); y++) {
                diePane.clearCell(0, y, sc);
                diePane.clearCell(1, y, sc);
                diePane.clearCell(diePane.getGridWidth() - 1, y, sc);
                diePane.clearCell(diePane.getGridWidth() - 2, y, sc);
            }
            sc = new SColor(fade.getRed(), fade.getGreen(), fade.getBlue(), 240);
            for (int x = 2; x < diePane.getGridWidth() - 2; x++) {
                for (int y = 2; y < diePane.getGridHeight() - 2; y++) {
                    diePane.clearCell(x, y, sc);
                }
            }

            String text;
            int x;
            int y = 3;
            int left = 5;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (diePane.getGridWidth() - text.length()) / 2;//centered
            diePane.placeHorizontalString(x, y, text, SColor.ELECTRIC_GREEN, sc);
            y += 2;

            text = "Your peaceful life as a fisherman has come to an end.";
            x = left;//left justified
            diePane.placeHorizontalString(x, y, text);
            y += 1;
            text = "Because you died.";
            x = left;//left justified
            diePane.placeHorizontalString(x, y, text);
            y += 2;
            text = "It's lucky for you though, now you don't have to hear the";
            x = left;//left justified
            diePane.placeHorizontalString(x, y, text);
            y += 1;
            text = "screams of your friends and loved ones as they are torn apart";
            x = left;//left justified
            diePane.placeHorizontalString(x, y, text);
            y += 1;
            text = "by vicious elemental beings.";
            x = left;//left justified
            diePane.placeHorizontalString(x, y, text);
            y += 3;

            text = "You died because:";
            x = left;//left justified
            diePane.placeHorizontalString(x, y, text, SColor.SAFETY_ORANGE, sc);
            y += 1;
            text = reason;
            x = left;//left justified
            diePane.placeHorizontalString(x, y, text);
            y += 1;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (diePane.getGridWidth() - text.length()) / 2;//centered
            y = height - 3;
            diePane.placeHorizontalString(x, y, text, SColor.ELECTRIC_GREEN, sc);

            diePane.refresh();
            layers.setLayer(diePane, JLayeredPane.DRAG_LAYER);

            final long readTime = System.currentTimeMillis() + 200;
            diePane.addMouseListener(new MouseInputAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    if (System.currentTimeMillis() > readTime) {
                        if (SwingUtilities.isLeftMouseButton(e)) {
                            exiting();
                        } else {
                            restart();
                        }
                    }
                }

            });
        }
        layers.add(diePane);
    }

    private void addFish(Fish fish) {
        int n = fishInventory.get(fish.element).get(fish.size);
        if (n < maxFish) {
            printOut("Caught a " + fish.name + "!");
            fishInventory.get(fish.element).put(fish.size, n + 1);
            updateFishInventoryPanel();
        } else {
            printOut("No more room for " + fish.name + ".");
        }
    }

    private void removeFish(Fish fish) {
        int n = fishInventory.get(fish.element).get(fish.size);
        if (n > 0) {
//            if (n == 1) {
//                printOut("That was your last " + fish.name + "!");
            selectedFish = null;
//            } else {
//                printOut("Throwing a " + fish.name + ".");
//            }
            fishInventory.get(fish.element).put(fish.size, n - 1);
            updateFishInventoryPanel();
        } else {
//            printOut("No more " + fish.name + " in your inventory.");
        }
    }

    /**
     * Attempts to move in the given direction. If a monster is in that direction then the player
     * attacks the monster.
     *
     * Returns false if there was a wall in the direction and so no action was taken.
     *
     * @param dir
     * @return
     */
    private boolean tryToMove(Direction dir) {
        MapCell tile = map[player.x + dir.deltaX][player.y + dir.deltaY];
        if (tile.isBlocking()) {
            if (tile.terrain.lake) {
                terrain = map[player.x][player.y].terrain;
                element = tile.terrain.element;
                goFish();
            } else {
                printOut("You can't walk through the " + (tile.feature != null && tile.feature.blocking ? tile.feature.name : tile.terrain.name) + ".");
            }
            return false;
        }

        Creature monster = tile.creature;
        if (monster == null) {//move the player
            map[player.x][player.y].creature = null;
            player.x += dir.deltaX;
            player.y += dir.deltaY;
            map[player.x][player.y].creature = player;
            return true;
        } else if (monster != player) {
            printOut("You have no way to directly hurt the " + monster.name + "!");
            return false;
        }
        return true;
    }

    /**
     * Updates the map display to show the current view
     */
    private void updateMap() {
        doFOV();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y].light = SColor.WHITE;
//                map[x][y].seen = true;
                mapPanel.placeCharacter(x, y, map[x][y].getSymbol().charAt(0), map[x][y].foregroundColor(), map[x][y].backgroundColor());
            }
        }

        mapPanel.placeCharacter(player.x, player.y, player.symbol.charAt(0));
        mapPanel.refresh();
    }

    private void updateFishInventoryPanel() {
        int x = 1;//start off with a bit of padding
        fishInventoryPanel.removeHighlight();
        for (Element e : Element.values()) {
            int y = 1;
            for (Size s : Size.values()) {
                if (selectedFish != null && selectedFish.element == e && selectedFish.size == s) {
                    fishInventoryPanel.highlight(x, y, x + maxFish - 1, y);
                }
                int n = fishInventory.get(e).get(s);
                for (int i = 0; i < maxFish; i++) {
                    if (i < n) {
                        fishInventoryPanel.placeCharacter(x + i, y, Fish.symbol(s).charAt(0), e.color);
                    } else {
                        fishInventoryPanel.clearCell(x + i, y);
                    }
                }
                y++;
            }
            x += maxFish + 1;
        }

        for (x = 0; x < maxHealth; x++) {
            if (x < player.health) {
                fishInventoryPanel.placeCharacter(x + healthX, 2, bobber, SColor.BLOOD);
            } else {
                fishInventoryPanel.clearCell(x + healthX, 2);
            }
        }

        fishInventoryPanel.refresh();
    }

    private void updateOverlay() {
        overlayPanel.erase();

        RadiusStrategy strat = BasicRadiusStrategy.CIRCLE;

        if (selectedFish != null) {
            int radius = 1;
            SColor c = selectedFish.color;
            switch (selectedFish.size) {
                case SMALL:
                    radius = 2;
                    break;
                case MEDIUM:
                    radius = 3;
                    break;
                case LARGE:
                    radius = 5;
                    break;
                case GIANT:
                    radius = 9;
                    break;
            }
            for (int x = overlayLocation.x - radius; x <= overlayLocation.x + radius; x++) {
                for (int y = overlayLocation.y - radius; y <= overlayLocation.y + radius; y++) {
                    if (strat.radius(overlayLocation.x, overlayLocation.y, x, y) <= radius + 0.1) {
                        overlayPanel.clearCell(x, y, new SColor(c.getRed(), c.getGreen(), c.getBlue(), overlayAlpha));
                    }
                }
            }
        }

        overlayPanel.refresh();
    }

    /**
     * Sets the output panel to show the message.
     *
     * @param message
     */
    private void printOut(String message) {

        outputPanel.erase();
        outputPanel.setVisible(true);

        outputPanel.placeHorizontalString(outputPanel.getGridWidth() - message.length() - 1, 1, message);
        outputPanel.refresh();

        final long startTime = System.currentTimeMillis();
        final long duration = 2400;//in milliseconds
        outputEndTime = Math.max(outputEndTime, startTime + duration);
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                while (System.currentTimeMillis() < outputEndTime) {
                    Thread.yield();
                }
                outputPanel.setVisible(false);
                outputPanel.erase();
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Calculates the Field of View and marks the maps spots seen appropriately.
     */
    private void doFOV() {
        boolean[][] walls = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                walls[x][y] = map[x][y].isOpaque();
            }
        }
        fov.calculateFOV(walls, player.x, player.y, Math.min(width, height) / 3);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y].seen = fov.isLit(x, y);
            }
        }
    }

    private void createMap() {
        map = new MapCell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                map[x][y] = new MapCell(Terrain.DIRT);
                if (rng.nextDouble() < 0.1) {
                    map[x][y].feature = TerrainFeature.TREE;
                } else if (rng.nextDouble() < 0.105) {
                    map[x][y].terrain = Terrain.makeElementalPool(Element.getRandomElement());
                    map[x][y].terrain.lake = true;
                }
            }
        }

        for (int i = 0; i < 20; i++) {
            placeWallChunk(Terrain.GRASS, TerrainFeature.BUSH);
        }
        for (int i = 0; i < 20; i++) {
            placeWallChunk(Terrain.GRASS, null);
        }
        for (int i = 0; i < 20; i++) {
            placeWallChunk(Terrain.STONE, null);
        }

        String[] mapDrawing = new String[]{
            "############..####",
            "#....##..........#",
            "#....##..........#",
            "#.........####....",
            "#.........####....",
            "..........####...#",
            "#................#",
            "#####..###########"
        };

        int x = (width / 2) - 2;
        int y = (height / 2) - 4;
        for (String s : mapDrawing) {
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '#':
                        map[x][y] = new MapCell(Terrain.STONE, TerrainFeature.STONE_WALL);
                        break;
                    case '.':
                        map[x][y] = new MapCell(Terrain.STONE);
                        break;
                }
                x++;
            }
            y++;
            x = (width / 2) - 2;
        }

        for (int i = 0; i < 20; i++) {
            Creature creature = Creature.getRandomMonster();
            placeMonster(creature);
            monsters.add(creature);
        }

        player.x = width / 2;
        player.y = height / 2;
        MapCell cell = map[player.x][player.y];
        cell.creature = player;
        cell.terrain = Terrain.DIRT;
        cell.feature = null;
        cell.item = null;
    }

    /**
     * Randomly places a group of walls in the map. This replaces whatever was in that location
     * previously.
     */
    private void placeWallChunk(Terrain t, TerrainFeature tf) {
        int spread = 5;
        int centerX = rng.nextInt(width);
        int centerY = rng.nextInt(height);

        for (int placeX = centerX - spread; placeX < centerX + spread; placeX++) {
            for (int placeY = centerY - spread; placeY < centerY + spread; placeY++) {
                if (rng.nextDouble() < 0.2 && placeX > 0 && placeX < width - 1 && placeY > 0 && placeY < height - 1) {
                    map[placeX][placeY] = new MapCell(t, tf);
                }
            }
        }
    }

    /**
     * Places the provided monster into an open tile space.
     *
     * @param monster
     */
    private void placeMonster(Creature monster) {
        int x;
        int y;
        do {

            x = rng.nextInt(width - 2) + 1;
            y = rng.nextInt(height - 2) + 1;
        } while (map[x][y].isBlocking() || map[x][y].creature != null || (x > width * 0.3 && x < width * 0.6 && y > height * 0.3 && y < height * 0.6));

        map[x][y].creature = monster;
        monster.x = x;
        monster.y = y;
    }

    public Point getClosestWaypoint(Point from, Point to) {
        Queue<Point> line = Bresenham.line2D(from, to);
        if (line.size() < 2) {
            return null;
        }
        line.poll();
        return line.poll();

//        LinkedList<Vertex> path = graph.getDijkstraPath(from, to);
//        if (path == null) {
//            return to;//no waypoints between targets
//        }
//
//        Point previous = path.get(0).point;
//        for (Vertex v : path) {
//            if (los.isReachable(sightBlocking, from.x, from.y, v.point.x, v.point.y)) {
//                previous = v.point;
//            } else {
//                return previous;//go for the one furthest away that's visible
//            }
//        }
//        return previous;
    }

    /**
     * Moves the monster given if possible. Monsters will not move into walls, other monsters, or
     * the player.
     *
     * @param monster
     */
    private void moveMonster(Creature monster) {
//        Direction dir = Direction.CARDINALS[rng.nextInt(Direction.CARDINALS.length)];//get a random direction
        Point p = getClosestWaypoint(new Point(monster.x, monster.y), new Point(player.x, player.y));
        Direction dir;
        dir = SCollections.getRandomElement(Arrays.asList(Direction.values()));
        if (p != null) {
            dir = Direction.getDirection(p.x - monster.x, p.y - monster.y);
            if (map[p.x][p.y].isBlocking()) {
                dir = SCollections.getRandomElement(Arrays.asList(Direction.values()));
            }
        }
        while (dir == Direction.NONE) {
            dir = SCollections.getRandomElement(Arrays.asList(Direction.values()));
        }
//        Direction dir = Direction.getDirection(player.x - monster.x, player.y - monster.y);
        if (monster.x + dir.deltaX < 0 || monster.x + dir.deltaX >= width || monster.y + dir.deltaY < 0 || monster.y + dir.deltaY >= height) {
            return;//trying to leave map so do nothing
        }

        MapCell tile = map[monster.x + dir.deltaX][monster.y + dir.deltaY];
        if (!tile.isBlocking() && tile.creature == null) {
            map[monster.x][monster.y].creature = null;
            monster.x += dir.deltaX;
            monster.y += dir.deltaY;
            map[monster.x][monster.y].creature = monster;
        } else if (tile.creature == player) {
            hurtPlayer(monster.name);
        }

        if (nowFishing) {
            for (Direction d : Direction.CARDINALS) {
                if (d.deltaX + monster.x >= 0 & d.deltaX + monster.x < width && d.deltaY + monster.y >= 0 && d.deltaY + monster.y < height) {
                    if (d.deltaX + monster.x == player.x && d.deltaY + monster.y == player.y) {
                        printOut("A monster is next to you!   Right-click to stop fishing.");
                    }
                }
            }
        }
    }

    private void hurtPlayer(String cause) {
        player.health--;
        printOut("The " + cause + " hurt you!  You have " + player.health + " health now.");
        if (player.health <= 0) {
            die("The " + cause + " finished you off.");
        }
        updateFishInventoryPanel();
    }

    private void checkForReactions() {

    }

    /**
     * Moves all the monsters, one at a time.
     */
    private void moveAllMonsters() {
        for (Creature monster : monsters) {
            moveMonster(monster);
        }
    }

    /**
     * Sets up the frame for display and keyboard input.
     */
    private void initializeFrame() {
        frame = new JFrame("Assault Fish");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exiting();
            }
        });
        try {
            frame.setIconImage(ImageIO.read(new File("./icon.png")));
        } catch (IOException ex) {
            //don't do anything if it failed, the default Java icon will be used
        }
        frame.setBackground(SColor.BLACK);
        frame.setUndecorated(true);

        layers = new JLayeredPane();
        frame.add(layers, BorderLayout.WEST);

//        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
//        int screenWidth = gd.getDisplayMode().getWidth();
//        int screenHeight = gd.getDisplayMode().getHeight();
//        int bufferVertical = 30;//guess at system tray and jframe bar
//        int bufferHorizontal = 2;//guess at side width
        Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int screenWidth = winSize.width;
        int screenHeight = winSize.height;
        int bufferHorizontal = 0;
        int bufferVertical = 0;

        mapPanel = new SwingPane(width, height, font);
        mapPanel.setDefaultBackground(SColor.BLACK);
        while (mapPanel.getCellWidth() * (mapPanel.getGridWidth()) >= screenWidth + bufferHorizontal
                || mapPanel.getCellHeight() * (mapPanel.getGridHeight() + 6) >= screenHeight + bufferVertical) {
            mapPanel = new SwingPane(width, height, font);
            mapPanel.setDefaultBackground(SColor.BLACK);
            font = new Font(font.getName(), font.getStyle(), font.getSize() - 1);
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                mapPanel.clearCell(x, y);
            }
        }
        mapPanel.setDefaultForeground(SColor.CREAM);

        textFactory = mapPanel.getTextCellFactory();
        textFactory.setAntialias(true);
        for (Size s : Size.values()) {
            textFactory.addFit(Fish.symbol(s));
        }

        textFactory.initializeBySize(mapPanel.getCellWidth(), mapPanel.getCellHeight(), font);
//        mapPanel.placeHorizontalString(width / 2 - 4, height / 2, "Loading");
        mapPanel.refresh();
        layers.setPreferredSize(mapPanel.getPreferredSize());
        layers.setLayer(mapPanel, JLayeredPane.DEFAULT_LAYER);
        layers.add(mapPanel);

        overlayPanel = new SwingPane(mapPanel.getGridWidth(), mapPanel.getGridHeight(), textFactory);
        overlayPanel.refresh();
        layers.setLayer(overlayPanel, JLayeredPane.PALETTE_LAYER);
        layers.add(overlayPanel);

        outputPanel = new SwingPane(mapPanel.getGridWidth(), mapPanel.getGridHeight(), textFactory);
        outputPanel.setDefaultBackground(SColor.BLACK_KITE);
        outputPanel.setDefaultForeground(SColor.TEA_GREEN);
        outputPanel.refresh();
        layers.setLayer(outputPanel, JLayeredPane.POPUP_LAYER);
        layers.add(outputPanel);

        fishThrowingPanel = new SwingPane(mapPanel.getGridWidth(), mapPanel.getGridHeight(), textFactory);
        fishThrowingPanel.setDefaultBackground(SColor.TRANSPARENT);
        fishThrowingPanel.setDefaultForeground(SColor.LOQUAT_BROWN);
        fishThrowingPanel.refresh();
        layers.setLayer(fishThrowingPanel, JLayeredPane.POPUP_LAYER);
        layers.add(fishThrowingPanel);

        fishText = new TextCellFactory(textFactory);
        fishText.setPadding(0, 0, 0, 1);
        fishText.initializeBySize(mapPanel.getCellWidth(), mapPanel.getCellHeight(), font);
        fishInventoryPanel = new SwingPane(mapPanel.getGridWidth(), 6, fishText);
        fishInventoryPanel.setDefaultBackground(SColor.BLACK);
        frame.add(fishInventoryPanel, BorderLayout.SOUTH);

        frame.pack();

        fishInventoryPanel.addMouseListener(new MenuMouse(fishInventoryPanel.getCellWidth(), fishInventoryPanel.getCellHeight()));
        mapMouse = new MapMouse(mapPanel.getCellWidth(), mapPanel.getCellHeight());
        inventoryMouse = new FishInventoryMouse(fishInventoryPanel.getCellWidth(), fishInventoryPanel.getCellHeight());

        //add invisibly the fishing panels
        fishMouse = new FishMouse();
        initFishingGUI();
    }

    private void flipMouseControl(boolean mapMode) {
        if (mapMode) {
            mapPanel.addMouseListener(mapMouse);
            mapPanel.addMouseMotionListener(mapMouse);
            mapPanel.addMouseWheelListener(mapMouse);
            fishInventoryPanel.addMouseListener(inventoryMouse);
            frame.removeMouseListener(fishMouse);
        } else {
            mapPanel.removeMouseListener(mapMouse);
            mapPanel.removeMouseMotionListener(mapMouse);
            mapPanel.removeMouseWheelListener(mapMouse);
            fishInventoryPanel.removeMouseListener(inventoryMouse);
            frame.addMouseListener(fishMouse);
        }
    }

    private void initializeFishInventory() {
        SwingPane p = fishInventoryPanel;
        for (int x = 0; x < p.getGridWidth(); x++) {
            for (int y = 0; y < p.getGridHeight(); y++) {
                p.clearCell(x, y);
            }
        }

        int x = 1;//start off with a bit of padding
        for (Element e : Element.values()) {
            p.placeHorizontalString(x, 0, e.name, e.color, SColor.BLACK);
            x += maxFish + 1;
        }

        p.placeHorizontalString(healthX, 1, "Health", SColor.BLOOD, p.getBackground());

        p.placeHorizontalString(helpIconLocation.x, helpIconLocation.y, "HELP", SColor.CREAM, p.getBackground());
        p.placeHorizontalString(muteIconLocation.x, muteIconLocation.y, "MUTE", SColor.SAFETY_ORANGE, p.getBackground());
        p.placeHorizontalString(exitIconLocation.x, exitIconLocation.y, "EXIT", SColor.BRILLIANT_ROSE, p.getBackground());
        //TODO -- add text for menu once menu is available

        updateFishInventoryPanel();
    }

    private class MenuMouse extends MouseInputAdapter {

        private final int cellWidth;
        private final int cellHeight;

        public MenuMouse(int cellWidth, int cellHeight) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
        }

        private MouseEvent translateToGrid(MouseEvent e) {
            int x = e.getX() / cellWidth;
            int y = e.getY() / cellHeight;
            return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx(), x, y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            e = translateToGrid(e);
            if (helpIconLocation.contains(e.getX(), e.getY()) && helpPane.getParent() == null) {
                showHelp();
            } else if (exitIconLocation.contains(e.getX(), e.getY())) {
                exiting();
            } else if (muteIconLocation.contains(e.getX(), e.getY())) {
                if (sound != null) {
                    double volume = sound.musicVolume;
                    String on;
                    if (volume > 0) {
                        sound.setMusicVolume(0);
                        on = "off";
                    } else {
                        sound.setMusicVolume(sound.maxMusicVolume);
                        on = "on";
                    }
                    // Set the value of the preference
                    prefs.put(SOUND_PREF, on);
                }
            }
        }

    }

    private class FishInventoryMouse extends MouseInputAdapter {

        private final int cellWidth;
        private final int cellHeight;

        public FishInventoryMouse(int cellWidth, int cellHeight) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
        }

        private MouseEvent translateToGrid(MouseEvent e) {
            int x = e.getX() / cellWidth;
            int y = e.getY() / cellHeight;
            return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx(), x, y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (canClick) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    e = translateToGrid(e);

                    int x = 1;//start off with a bit of padding
                    check:
                    for (Element element : Element.values()) {
                        int y = 1;
                        for (Size s : Size.values()) {
                            if (e.getX() >= x && e.getX() < x + maxFish && e.getY() == y) {
                                if (fishInventory.get(element).get(s) > 0) {
                                    if (selectedFish != null && selectedFish.element == element && selectedFish.size == s) {
                                        selectedFish = null;
                                    } else {
                                        selectedFish = new Fish(s, element);
                                    }
                                }
                                break check;//desired selection not available
                            }
                            y++;
                        }
                        x += maxFish + 1;
                    }

                    updateFishInventoryPanel();
                    updateOverlay();
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    e = translateToGrid(e);

                    int x = 1;//start off with a bit of padding
                    check:
                    for (Element element : Element.values()) {
                        int y = 1;
                        for (Size s : Size.values()) {
                            if (e.getX() >= x && e.getX() < x + maxFish && e.getY() == y) {
                                if (fishInventory.get(element).get(s) > 0) {
                                    Fish dummy = new Fish(s, element);
                                    printOut(dummy.name);
                                }
                                break check;//found selection
                            }
                            y++;
                        }
                        x += maxFish + 1;
                    }
                }
            }
        }
    }

    private class MapMouse extends MouseInputAdapter {

        private final int cellWidth;
        private final int cellHeight;

        public MapMouse(int cellWidth, int cellHeight) {
            this.cellWidth = cellWidth;
            this.cellHeight = cellHeight;
        }

        private MouseEvent translateToGrid(MouseEvent e) {
            int x = e.getX() / cellWidth;
            int y = e.getY() / cellHeight;
            return new MouseEvent(e.getComponent(), e.getID(), e.getWhen(), e.getModifiersEx(), x, y, e.getClickCount(), e.isPopupTrigger(), e.getButton());
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (canClick) {
                if (SwingUtilities.isMiddleMouseButton(e) || (SwingUtilities.isLeftMouseButton(e) && e.isControlDown())) {
                    e = translateToGrid(e);
                    String description = "";
                    MapCell tile = map[e.getX()][e.getY()];
                    description += "Terrain: " + tile.terrain.name;
                    if (tile.terrain.lake) {
                        description += ", it can be fished.";
                    }
                    if (tile.feature != null) {
                        description += "  Feature: " + tile.feature.name + ".";
                    }
                    if (tile.creature != null) {
                        description += "  Creature: " + tile.creature.name + ".";
                    }
                    if (tile.item != null) {
                        description += "  Item: " + tile.item.name + ".";
                    }

                    printOut(description);
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    e = translateToGrid(e);
                    if (selectedFish == null) {
                        canClick = false;
                        workClick(e.getX(), e.getY());
                        canClick = true;
                    } else {
                        final int x = e.getX();
                        final int y = e.getY();
                        Thread thread = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                canClick = false;
                                throwFish(x, y);
                                canClick = true;
                            }
                        });
                        thread.start();
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    if (selectedFish != null) {
                        selectedFish = null;
                        updateFishInventoryPanel();
                    }
                }
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            overlayPanel.setVisible(false);
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            overlayPanel.setVisible(true);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            e = translateToGrid(e);
            if (overlayLocation.x != e.getX() || overlayLocation.y != e.getY()) {
                overlayLocation.x = e.getX();
                overlayLocation.y = e.getY();
                updateOverlay();
            }
        }

    }

    private void initFishingGUI() {
        JLayeredPane fishingLayers = new JLayeredPane();

        fishingMasterPanel = new JPanel();
        fishingMasterPanel.setBackground(SColor.BLACK);
        fishingMasterPanel.setPreferredSize(mapPanel.getPreferredSize());
        fishingMasterPanel.setSize(mapPanel.getSize());
        fishingMasterPanel.setLayout(new BorderLayout());
        fishingMasterPanel.add(fishingLayers, BorderLayout.NORTH);

        TextCellFactory fishingFactory = new TextCellFactory(textFactory);

        fishingFactory.initializeBySize((int) (mapPanel.getCellWidth() / widthScale), (int) (mapPanel.getCellHeight() / heightScale), new Font(font.getFontName(), Font.BOLD, font.getSize() + 2));//a little extra size in case the switch away from bold matters
        fishingViewPanel = new SwingPane(fishWidth, fishHeight, fishingFactory);
        fishingLayers.setLayer(fishingViewPanel, JLayeredPane.DEFAULT_LAYER);
        fishingLayers.add(fishingViewPanel);

        fishPane = new SwingPane(fishWidth, fishHeight, fishingFactory);
        fishingLayers.setLayer(fishPane, JLayeredPane.PALETTE_LAYER);//set just above the regular map layer
        fishingLayers.add(fishPane);

        TextCellFactory largeFactory = new TextCellFactory();
        largeFactory.setAntialias(true);
        largeFactory.setFitCharacters("@");
        largeFactory.initializeBySize(fishingViewPanel.getCellWidth() * largeTextScale, fishingViewPanel.getCellHeight() * largeTextScale, new Font(font.getFontName(), Font.BOLD, 192));
        largeTextPane = new SwingPane(fishWidth / largeTextScale, fishHeight / largeTextScale, largeFactory);
        fishingLayers.setLayer(largeTextPane, JLayeredPane.MODAL_LAYER);
        fishingLayers.add(largeTextPane);

        fishingLayers.setPreferredSize(fishingViewPanel.getPreferredSize());
        fishingLayers.setSize(fishingViewPanel.getSize());

        meterPanel = new SwingPane(width, 3, textFactory);
        meterPanel.setDefaultBackground(SColor.BLACK);
        initMeter();
        fishingMasterPanel.add(meterPanel, BorderLayout.SOUTH);

        layers.setLayer(fishingMasterPanel, JLayeredPane.MODAL_LAYER);
//        frame.addMouseListener(new FishMouse());
    }

    private void dropHook() {
        fishingViewPanel.placeCharacter(bobberLocation.x, bobberLocation.y + 1, hook, hookColor);
        fishingViewPanel.refresh();
        int x = bobberLocation.x;
        int y;
        for (y = bobberLocation.y + 2; y <= bed(x); y++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            fishingViewPanel.placeCharacter(x, y - 1, line(UP), lineColor);
            fishingViewPanel.placeCharacter(x, y, hook, hookColor);
            fishingViewPanel.refresh();
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }

        Fish fish = null;
        do {
            fishingViewPanel.clearCell(x, y);

            y--;
            fishingViewPanel.placeCharacter(x, y, hook, hookColor);
            if (fish != null) {
                fishingViewPanel.placeCharacter(x, y, fish.symbol.charAt(0), fish.color);
            } else if (fishMap[x][y] != null) {
                fish = fishMap[x][y];
                fishes.remove(fish);
                fishMap[x][y] = null;
                fishPane.clearCell(x, y);
                fishPane.refresh();
            }
            fishingViewPanel.refresh();

            try {
                Thread.sleep(40);
            } catch (InterruptedException ex) {
            }
        } while (y > bobberLocation.y + 1);

        if (fish != null) {
            addFish(fish);
        }
    }

    private void throwBobber() {
        int targetX = (int) (castingStrength * (fishWidth - 1 - terrainWidth * 2) + terrainWidth * 2 + 1);//finds drop target based on strength percent
        int startY = largeTextScale * 2 + 1;//start at the guy's head
        int startX = terrainWidth - 2;//start at the shoreline

        BallisticsSolver solver = new BallisticsSolver(startX, startY, targetX, liquidHeight - 2, wind, gravity);
        int solveHeight = fishWidth / (targetX + 5);
        solveHeight = Math.min(solveHeight, largeTextScale * 2 - 2);
        solveHeight = Math.max(solveHeight, 1);
        solver.solveByHeight(solveHeight);

        int lastX = -1, lastY = -1, bobberX = -2, bobberY = -2;
        double trueTime = solver.getTime();
        int targetTime = targetX * 20;//in milliseconds
        long lastTime;
        boolean goingDown = false;
        for (double time = 0; time <= targetTime; time += System.currentTimeMillis() - lastTime) {
            lastTime = System.currentTimeMillis();
            double solverTime = trueTime * time / targetTime;
            bobberX = solver.x(solverTime);
            bobberY = solver.y(solverTime);
            if (lastX != bobberX) {
                fishingViewPanel.placeCharacter(lastX, lastY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
                fishingViewPanel.placeCharacter(bobberX, bobberY, bobber, bobberColor);
                fishingViewPanel.refresh();
                lastX = bobberX;
                lastY = bobberY;
                goingDown = false;
            } else if (Math.abs(bobberY - lastY) > 1 || (goingDown && bobberY != lastY)) {
                fishingViewPanel.placeCharacter(lastX, lastY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
                fishingViewPanel.placeCharacter(bobberX, bobberY, bobber, bobberColor);
                fishingViewPanel.refresh();
                lastX = bobberX;
                lastY = bobberY;
                goingDown = true;
            } else if (bobberY != lastY) {
                fishingViewPanel.clearCell(lastX, lastY);
                goingDown = false;
            }

            Thread.yield();
        }

        fishingViewPanel.placeCharacter(bobberX, bobberY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
        fishingViewPanel.placeCharacter(bobberX, bobberY + 1, bobber, bobberColor);
        bobberLocation = new Point(bobberX, bobberY + 1);
        fishingViewPanel.refresh();

        for (int x = 1; x < meterPanel.getGridWidth() - 1; x++) {
            meterPanel.placeCharacter(x, 1, ' ');
        }
        meterPanel.refresh();
    }

    /**
     * Returns the y position of the last space before the terrain bed.
     *
     * To allow for bounds safety, this method will return 0 as the result if the bed reaches the
     * top rather than -1.
     *
     * @param x
     * @return
     */
    private int bed(int x) {
        for (int y = 1; y < fishHeight; y++) {
            if (terrainMap[x][y]) {
                return y - 1;
            }
        }
        return fishHeight - 1;
    }

    private void initFishingDisplay() {
        for (int x = 0; x < fishWidth; x++) {
            for (int y = 0; y < fishHeight; y++) {
                if (terrainMap[x][y]) {
                    fishingViewPanel.clearCell(x, y, getTerrainColor(x, y));
                } else if (liquidMap[x][y]) {
                    fishingViewPanel.clearCell(x, y, getLiquidColor(x, y));
                } else {
                    fishingViewPanel.clearCell(x, y, getSkyColor(x, y));
                }
            }
        }

//        fishPane.erase();
        for (Fish f : fishes) {
            fishPane.placeCharacter(f.x, f.y, f.symbol.charAt(0), f.color);
        }

        fishPane.refresh();
        fishingViewPanel.refresh();
        largeTextPane.refresh();
    }

    private SColor getTerrainColor(int x, int y) {
        return SColorFactory.blend(terrain.color, SColorFactory.dim(terrain.color), PerlinNoise.noise(y, x));
    }

    private SColor getLiquidColor(int x, int y) {
        return SColorFactory.blend(SColorFactory.blend(element.color, SColorFactory.dim(element.color), PerlinNoise.noise(x, y)), SColorFactory.dimmest(element.color), y / (double) (fishHeight - liquidHeight));
    }

    private SColor getSkyColor(int x, int y) {
        return SColorFactory.blend(SColorFactory.lightest(skyColor), SColorFactory.dim(skyColor), y / (double) liquidHeight);
    }

    private void initFish() {
        fishMap = new Fish[fishWidth][fishHeight];

        for (Fish fish : fishes) {
            boolean placed = false;
            while (!placed) {
                int x = rng.between(terrainWidth * 2 + 1, fishWidth);
                if (bed(x) > liquidHeight + 1) {
                    int y = rng.between(liquidHeight + 1, bed(x));
                    if (fishMap[x][y] == null) {
                        fishMap[x][y] = fish;
                        fish.x = x;
                        fish.y = y;
                        placed = true;
                    }
                }
            }
        }
    }

    private void initFishingMap() {
        terrainMap = new boolean[fishWidth][fishHeight];
        liquidMap = new boolean[fishWidth][fishHeight];

        //fill in standing edge
        for (int x = 0; x < terrainWidth; x++) {
            for (int y = liquidHeight - largeTextScale - 1; y < fishHeight; y++) {
                terrainMap[x][y] = true;
            }
        }

        //fill in slope to liquid
        int lastHeight = liquidHeight - largeTextScale + 1;
        int nextHeight = liquidHeight + 1;
        for (int x = terrainWidth; x < terrainWidth * 2; x++) {
            int offset = rng.between(-1, 2);
            offset *= Math.signum(nextHeight - lastHeight);
            int terrainHeight = lastHeight + offset;
            terrainHeight = Math.min(terrainHeight, Math.max(lastHeight, nextHeight));
            terrainHeight = Math.max(terrainHeight, Math.min(lastHeight, nextHeight));
            lastHeight = terrainHeight;
            if (lastHeight == nextHeight) {
                nextHeight = rng.between(liquidHeight + 4, fishHeight - 1);
            }
            for (int y = liquidHeight; y < terrainHeight; y++) {
                liquidMap[x][y] = true;
            }
            for (int y = terrainHeight; y < fishHeight; y++) {
                terrainMap[x][y] = true;
            }
        }

        //fill in rest of terrain and liquid
        lastHeight = liquidHeight + 2;
        nextHeight = rng.between(liquidHeight + 8, fishHeight - 1);
        for (int x = terrainWidth * 2; x < fishWidth; x++) {
            int offset = rng.between(-1, 3);
            offset *= Math.signum(nextHeight - lastHeight);
            int terrainHeight = lastHeight + offset;
            terrainHeight = Math.min(terrainHeight, fishHeight - 1);
            terrainHeight = Math.max(terrainHeight, liquidHeight + 12);
            lastHeight = terrainHeight;
            if (lastHeight == nextHeight) {
                nextHeight = rng.between(liquidHeight + 14, fishHeight - 1);
            }
            for (int y = liquidHeight; y < terrainHeight; y++) {
                liquidMap[x][y] = true;
            }
            for (int y = terrainHeight; y < fishHeight; y++) {
                terrainMap[x][y] = true;
            }
        }

        //place player
        largeTextPane.placeCharacter(1, 2, '@', playerColor);
    }

    private char line(Direction dir) {
        switch (dir) {//╱╲─╭╮
            case LEFT:
            case RIGHT:
                return '─';
            case UP:
            case DOWN:
                return '|';
            case UP_LEFT:
            case DOWN_RIGHT:
                return '╲';
            case UP_RIGHT:
            case DOWN_LEFT:
                return '╱';
            default:
                return ' ';
        }
    }

    private void initMeter() {
        meterPanel.placeHorizontalString((meterPanel.getGridWidth() - "Cast Strength".length() - 1) / 2, 0, "Cast Strength");
        meterPanel.placeHorizontalString(2, 0, "None");
        meterPanel.placeHorizontalString(meterPanel.getGridWidth() - 3 - "Max".length(), 0, "Max");
        meterPanel.refresh();
    }

    private class MeterListener implements ActionListener {

        double timeStep = 1000;//how many milliseconds per time step
        int meterOffset = 3;
        int meterSize;
        long time, lastTime;

        public void reset() {
            for (int i = 0; i < meterPanel.getGridWidth(); i++) {
                meterPanel.clearCell(i, 1);
            }
            meterPanel.refresh();
        }

        public void initialize() {
            meterSize = width - meterOffset * 2;
            time = (long) (1000 * Math.PI / 2.0);
            lastTime = System.currentTimeMillis();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            castingStrength = 1 - Math.abs(Math.sin(time / timeStep));
            int drawX = (int) (castingStrength * meterSize);
            drawX = Math.min(drawX, meterSize);//make sure rare case of strength 1 doesn't cause problems
            for (int i = 0; i < meterSize; i++) {
                if (i < drawX) {
                    meterPanel.placeCharacter(i + meterOffset, 1, '●', SColorFactory.fromPallet("meter", i / (float) (meterSize)));
                } else {
                    meterPanel.clearCell(i + meterOffset, 1);
                }
            }
            meterPanel.refresh();

            time += System.currentTimeMillis() - lastTime;
            lastTime = System.currentTimeMillis();
        }

    }

    private class FishMouse extends MouseInputAdapter {

        private Timer timer;

        public FishMouse() {
            meterListener = new MeterListener();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (casting) {
                    if (canCast) {
                        canCast = false;
                        timer.stop();
                        Thread thread = new Thread(new Runnable() {

                            @Override
                            public void run() {
                                throwBobber();
                                dropHook();
                                runTurn();//each cast counts as a move
                                meterListener.reset();
                                canCast = true;
                            }

                        });
                        thread.start();
                        casting = false;
                    }
                } else if (canCast) {
                    initFishingDisplay();
                    meterListener.reset();
                    meterListener.initialize();
                    timer = new Timer(10, meterListener);
                    timer.start();
                    casting = true;
                }
            } else if (SwingUtilities.isRightMouseButton(e) && !casting && canCast) {
                layers.remove(fishingMasterPanel);
                nowFishing = false;
                flipMouseControl(true);
            }
        }

    }

}
