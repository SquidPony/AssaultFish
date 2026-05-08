package assaultfish;

import assaultfish.mapping.MapCell;
import assaultfish.physical.*;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import squidpony.squidgrid.Direction;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidmath.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Queue;
import java.util.TreeMap;

import static java.awt.event.KeyEvent.*;
import static squidpony.squidgrid.Direction.*;

/**
 * This class starts up the game.
 *
 * @author Eben Howard
 */
public class AssaultFish {

    private static final String version = "2.0.0";

    private static final double widthScale = 1.2,
            heightScale = 1.2;
    private static final int gridWidth = 80,
            gridHeight = 40,
            fishHeight = (int) ((gridHeight - 3) * heightScale),
            fishWidth = (int) (gridWidth * widthScale),
            largeTextScale = 4,
            liquidHeight = largeTextScale * 4,
            terrainWidth = largeTextScale * 2 + 1,
            maxHealth = 6,
            healthX = gridWidth - 15, 
            cellWidth = 18,
            cellHeight = 24,
            fishCellWidth = 15,
            fishCellHeight = 20;
    private static final int maxFish = 6;
    private static final int overlayAlpha = 100;
    private static BitmapFont font;
    private static long outputEndTime;
    private static final Rectangle helpIconLocation = new Rectangle(gridWidth - 5, 1, 4, 1),
            muteIconLocation = new Rectangle(gridWidth - 5, 2, 4, 1),
            exitIconLocation = new Rectangle(gridWidth - 5, 3, 4, 1);

//    private static final String fishingPole = "🎣",//fishing pole and fish
//            whale = "🐋",//whale
//            octopus = "🐙",//octopus
//            //fish = "🐟",//fish
//            tropicalFish = "🐠",//tropical fish
//            blowfish = "🐡",//blowfish
//            spoutingWhale = "🐳",//spouting whale
//            gemstone = "💎",//gemstone
//            moneyBag = "💰";//money bag
    private final FOV fov = new FOV(FOV.SHADOW);
    private final GWTRNG rng = new GWTRNG(0x31337BEEFCA77L);

    private TextCellFactory textFactory;
    private TextCellFactory fishText;

    private SparseLayers mapPanel, outputPanel, meterPanel,
            fishingLayers, helpPane, fishThrowingPanel,
            winPane, diePane;
    private MeterListener meterListener;
    private FishMouse fishMouse;
    private MapMouse mapMouse;
    private MapKeys mapKeys;
    private FishInventoryMouse inventoryMouse;

    private Creature player;

    private ArrayList<Creature> monsters = new ArrayList<>();
    //private ArrayList<Treasure> treasuresFound = new ArrayList<>();
    private MapCell[][] map;

    private Fish selectedFish = null;
    private SparseLayers overlayPanel;
    private Coord overlayLocation = Coord.get(-1, -1);

    private final TreeMap<Element, TreeMap<Size, Integer>> fishInventory = new TreeMap<>();
    private SparseLayers fishInventoryPanel;

    private boolean casting = false, canCast = false, canClick = true;
    private double castingStrength = 0.4;

    /* ------------------ From FishingPanel ------------------------- */
    private boolean[][] terrainMap;
    private boolean[][] liquidMap;
    private Fish[][] fishMap;
    private OrderedSet<Fish> fishes = new OrderedSet<>();
    private double wind = 10, gravity = 20;
    private Terrain terrain;
    private Element element;
    private char bobber = '●',//O•☉✆✇♁┢Ø∅∮⊕⊖⊗⊘⊙⊚⊛⊜⊝Ⓧ◍◎●◐◑◒◓◔◕☯☮☻☺☹✪➊➋➌➍➎➏➐➑➒➓〄〇〶
            hook = 'J',
            wall = '#';
    private Coord bobberLocation;
    private SColor lineColor = SColor.BURNT_BAMBOO,
            bobberColor = SColor.SCARLET,
            hookColor = SColor.BRASS,
            playerColor = SColor.BETEL_NUT_DYE;
    private float skyColor = SColor.ALICE_BLUE.toFloatBits();
    private ArrayList<Color> meterPalette;
    private boolean nowFishing = false;
    
    private static final String SOUND_PREF = "Sound Pref";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new AssaultFish().go();
    }

    /**
     * Starts the game.
     */
    private void go() {
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("AssaultFish");
        boolean soundOn = prefs.getBoolean(SOUND_PREF, true); // "a string"

        Music music = Gdx.audio.newMusic(Gdx.files.internal("Eden.mp3"));
        if (!soundOn) {
            music.setVolume(0);
        }
        music.setLooping(true);
        music.play();
        
        SquidColorCenter scc = DefaultResources.getSCC();
        meterPalette = scc.gradient(SColor.RED, SColor.ORANGE);
        meterPalette.addAll(scc.gradient(SColor.ORANGE, SColor.YELLOW));
        meterPalette.addAll(scc.gradient(SColor.YELLOW, SColor.ELECTRIC_GREEN));
        textFactory = DefaultResources.getCrispDejaVuFont().width(cellWidth).height(cellHeight).initBySize();
        Fish.initSymbols(textFactory.font());
        //dummy up starting inventory
        for (Element e : Element.values()) {
            fishInventory.put(e, new TreeMap<Size, Integer>());
            for (Size s : Size.values()) {
                fishInventory.get(e).put(s, rng.nextInt(2));
            }
        }

        player = new Creature(Creature.PLAYER);
        player.color = SColor.CORNFLOWER_BLUE;

        initializeFrame();
        initializeFishInventory();

        showHelp();

        createMap();
        updateMap();
        flipMouseControl(true);
    }

    private void restart() {
        frame.setVisible(false);
        AssaultFish.main(new String[]{});
        frame.removeAll();
        frame = null;
    }

    private void showHelp() {
        if (helpPane == null) {
            helpPane = new SparseLayers(gridWidth, gridHeight, cellWidth, cellHeight, textFactory);
            Color fade = SColor.DARK_GRAY;
            SColor heading = SColor.RED_PIGMENT;
            Color command = SColor.SCHOOL_BUS_YELLOW;
            float sc = SColor.toEditedFloat(fade, 0f, 0f, 0f, -0.4f);
            for (int x = 0; x < helpPane.gridWidth(); x++) {
                helpPane.put(x, 0, sc);
                helpPane.put(x, 1, sc);
                helpPane.put(x, helpPane.gridHeight() - 1, sc);
                helpPane.put(x, helpPane.gridHeight() - 2, sc);
            }
            for (int y = 0; y < helpPane.gridHeight(); y++) {
                helpPane.put(0, y, sc);
                helpPane.put(1, y, sc);
                helpPane.put(helpPane.gridWidth() - 1, y, sc);
                helpPane.put(helpPane.gridWidth() - 2, y, sc);
            }
            sc = SColor.toEditedFloat(fade, 0f, 0f, 0f, -0.06f);
            for (int x = 2; x < helpPane.gridWidth() - 2; x++) {
                for (int y = 2; y < helpPane.gridHeight() - 2; y++) {
                    helpPane.put(x, y, sc);
                }
            }

            String text;
            int x;
            int y = 3;
            int left = 5;

            text = "ASSAULT FISH  v" + version;
            x = (helpPane.gridWidth() - text.length()) / 2;//centered
            helpPane.put(x, y, text, heading);
            y += 2;

            text = "Your peaceful life as a fisherman has come to an end.";
            x = left;//left justified
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "A horde of elementals has decended upon the land and it is your duty";
            x = left;//left justified
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "to fight them any way you can! And that means using the fishing skills";
            x = left;//left justified
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "of your forefathers to fish from the many local elemental pools and";
            x = left;//left justified
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "strategically throw your explosive catch at the enemy!";
            x = left;//left justified
            helpPane.put(x, y, text, SColor.WHITE);
            y += 3;

            text = "Main Map Controls";
            x = (helpPane.gridWidth() - text.length()) / 2;//centered
            helpPane.put(x, y, text, heading);
            y += 2;

            text = "Without a fish selected for throwing:";
            x = (helpPane.gridWidth() - text.length()) / 2;//centered
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Left click";
            x = left;//left justified
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - move";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Ctrl-Left click";
            x = left;
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - examine";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Left click on fish";
            x = left;//left justified
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - select fish";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 3;

            text = "With a fish selected for throwing:";
            x = (helpPane.gridWidth() - text.length()) / 2;//centered
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Left click";
            x = left;//left justified
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - throw fish";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Right click";
            x = left;
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - deselect fish";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Left click on fish";
            x = left;//left justified
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - select a new fish";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Left click on the slected fish";
            x = left;//left justified
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - deselect fish";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 3;

            text = "Fishing Controls";
            x = (helpPane.gridWidth() - text.length()) / 2;//centered
            helpPane.put(x, y, text, heading);
            y += 1;

            text = "Left click";
            x = left;//left justified
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - start casting meter / cast";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "Right click";
            x = left;//left justified
            helpPane.put(x, y, text, command);
            x += text.length();
            text = " - stop fishing";
            helpPane.put(x, y, text, SColor.WHITE);
            y += 3;

            text = "Elemental enemies are destroyed by";
            x = (helpPane.gridWidth() - text.length()) / 2;//centered
            helpPane.put(x, y, text, heading);
            y += 1;

            text = "Acid";
            x = left;//left justified
            helpPane.put(x, y, text, Element.ACID.color);
            x += text.length();
            text = " - Sand";
            helpPane.put(x, y, text, Element.SAND.color);
            y += 1;

            text = "Air";
            x = left;//left justified
            helpPane.put(x, y, text, Element.AIR.color);
            x += text.length();
            text = " - Mana";
            helpPane.put(x, y, text, Element.MANA.color);
            y += 1;

            text = "Blood";
            x = left;//left justified
            helpPane.put(x, y, text, Element.BLOOD.color);
            x += text.length();
            text = " - Tar";
            helpPane.put(x, y, text, Element.TAR.color);
            y -= 2;

            text = "Magma";
            x = left + 20;
            helpPane.put(x, y, text, Element.MAGMA.color);
            x += text.length();
            text = " - Water";
            helpPane.put(x, y, text, Element.WATER.color);
            y += 1;

            text = "Mana";
            x = left + 20;
            helpPane.put(x, y, text, Element.MANA.color);
            x += text.length();
            text = " - Blood";
            helpPane.put(x, y, text, Element.BLOOD.color);
            y += 1;

            text = "Sand";
            x = left + 20;
            helpPane.put(x, y, text, Element.SAND.color);
            x += text.length();
            text = " - Acid";
            helpPane.put(x, y, text, Element.ACID.color);
            y -= 2;

            text = "Tar";
            x = left + 40;
            helpPane.put(x, y, text, Element.TAR.color);
            x += text.length();
            text = " - Magma";
            helpPane.put(x, y, text, Element.MAGMA.color);
            y += 1;

            text = "Water";
            x = left + 40;
            helpPane.put(x, y, text, Element.WATER.color);
            x += text.length();
            text = " - Air";
            helpPane.put(x, y, text, Element.AIR.color);

            text = "--  press mouse button to continue --";
            x = (helpPane.gridWidth() - text.length()) / 2;//centered
            y = gridHeight - 3;
            helpPane.put(x, y, text, heading);
            helpPane.addMouseListener(new MouseInputAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    layers.remove(helpPane);
                    layers.repaint();
                }

            });
        }
        stage.add(helpPane);
    }

    private void goFish() {
        nowFishing = true;
        selectedFish = null;
        updateFishInventoryPanel();
        fishes.clear();
        fishPane.clear();
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
        //fishingMasterPanel.setVisible(true);
        //layers.add(fishingMasterPanel);
        stage.add(fishingLayers);
        flipMouseControl(false);
        printOut("You are now fishing from " + terrain.name + " shore into the " + element.name + " lake.");
        canCast = true;
    }

    /**
     * This is the main game loop method that takes input and process the results. Right now it doesn't loop!
     */
    private void runTurn() {
        updateMap();

        if (monsters.isEmpty()) {
            win();
        }

        checkForReactions();

        moveAllMonsters();
        updateMap();
    }

    private void workClick(final int x, final int y) {
//        Thread thread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
        boolean success = tryToMove(DirectionIntercardinal.getDirection(x - player.x, y - player.y));
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

        stage.remove(overlayPanel);

        Radius strat = Radius.CIRCLE;
        int radius = 1;
        Color c = selectedFish.color;
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
        Queue<Coord> line = Bresenham.line2D(Coord.get(player.x, player.y), Coord.get(targetX, targetY));
        do {
            Coord p = line.poll();
            targetX = p.x;
            targetY = p.y;
            fishThrowingPanel.clear();
            fishThrowingPanel.put(targetX, targetY, selectedFish.symbol.charAt(0), selectedFish.color);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        } while (!line.isEmpty()
                && (map[targetX][targetY].creature == null || map[targetX][targetY].creature == player)
                && (map[targetX][targetY].feature == null || map[targetX][targetY].feature == TerrainFeature.BUSH));
        fishThrowingPanel.clear();

        boolean[][] modified = new boolean[gridWidth][gridHeight];

        for (double i = 0; i <= 1; i += 0.1) {
            double radiusMod = (radius + 0.1) * i;
            for (int x = (int) (targetX - radiusMod); x <= targetX + radiusMod; x++) {
                for (int y = (int) (targetY - radiusMod); y <= targetY + radiusMod; y++) {
                    if (strat.radius(targetX, targetY, x, y) <= radiusMod) {
                        if (x >= 0 && x < gridWidth && y >= 0 && y < gridHeight) {
                            if (!modified[x][y]) {
                                reactToElementChange(x, y, selectedFish.element);
                                fishThrowingPanel.put(x, y, '*', selectedFish.color);
                                modified[x][y] = true;
                                mapPanel.put(x, y, map[x][y].getSymbol().charAt(0), map[x][y].foregroundColor());//, map[x][y].backgroundColor());
                            }
                        }
                    }
                }
            }
            updateMap();
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        }

        //hacky post-process to hide bugs
        monsters = new ArrayList<>();
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                Creature creature = map[x][y].creature;
                if (creature != null && creature != player) {
                    creature.x = x;
                    creature.y = y;
                    monsters.add(creature);
//                    if (!monsters.contains(creature)) {
//                        map[x][y].creature = null;//delete it if it's not in the monster list
//                    } else {
//                        if ((map[x][y].feature == null ? false : map[x][y].feature.blocking) || map[x][y].terrain.blocking) {//remove invalid creature
//                            map[x][y].creature = null;
//                            monsters.remove(creature);
//                        }
//                    }
                }
            }
        }
        for (Creature creature : monsters) {
            if (creature != map[creature.x][creature.y].creature) {
                System.err.println("Invalid creature: " + creature.name + " at " + creature.x + ", " + creature.y + "  should be " + map[creature.x][creature.y].creature);
            }
        }

        removeFish(selectedFish);
        fishThrowingPanel.clear();
        updateOverlay();
        stage.add(overlayPanel);
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
            monsters.remove(c);
            c = new Creature(c.name, c.health, c.symbol, c.element.combine(e));
            map[x][y].creature = c;
            c.x = x;
            c.y = y;
            monsters.add(c);
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
            winPane = new SparseLayers(gridWidth, gridHeight, cellWidth, cellHeight, textFactory);
            SColor fade = SColor.DARK_GRAY;
            SColor heading = SColor.RED_PIGMENT;
            SColor command = SColor.SCHOOL_BUS_YELLOW;
            float sc = SColor.translucentColor(fade.toFloatBits(), 0.6f);
            for (int x = 0; x < winPane.gridWidth(); x++) {
                winPane.put(x, 0, sc);
                winPane.put(x, 1, sc);
                winPane.put(x, winPane.gridHeight() - 1, sc);
                winPane.put(x, winPane.gridHeight() - 2, sc);
            }
            for (int y = 0; y < winPane.gridHeight(); y++) {
                winPane.put(0, y, sc);
                winPane.put(1, y, sc);
                winPane.put(winPane.gridWidth() - 1, y, sc);
                winPane.put(winPane.gridWidth() - 2, y, sc);
            }
            sc = SColor.translucentColor(sc, 0.94f);
            for (int x = 2; x < winPane.gridWidth() - 2; x++) {
                for (int y = 2; y < winPane.gridHeight() - 2; y++) {
                    winPane.put(x, y, sc);
                }
            }

            String text;
            int x;
            int y = 3;
            int left = 5;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (diePane.gridWidth() - text.length()) / 2;//centered
            diePane.put(x, y, text, SColor.ELECTRIC_GREEN);
            y += 2;

            text = "Your peaceful life as a fisherman has come to an end.";
            x = left;//left justified
            winPane.put(x, y, text, SColor.WHITE);
            y += 1;
            text = "Because you destroyed the elemental menace.";
            x = left;//left justified
            winPane.put(x, y, text, SColor.WHITE);
            y += 2;
            text = "Your fame and prowess are now legendary across the land! Your";
            x = left;//left justified
            winPane.put(x, y, text, SColor.WHITE);
            y += 1;
            text = "nights are filled with the shouts and laughter of all your";
            x = left;//left justified
            winPane.put(x, y, text, SColor.WHITE);
            y += 1;
            text = "friends and family.";
            x = left;//left justified
            winPane.put(x, y, text, SColor.WHITE);
            y += 2;
            text = "You live happily ever after.";
            x = left;//left justified
            winPane.put(x, y, text, SColor.WHITE);
            y += 1;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (winPane.gridWidth() - text.length()) / 2;//centered
            y = gridHeight - 3;
            winPane.put(x, y, text, SColor.ELECTRIC_GREEN);
            
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
            diePane = new SparseLayers(gridWidth, gridHeight, cellWidth, cellHeight, textFactory);
            SColor fade = SColor.DARK_GRAY;
            SColor heading = SColor.RED_PIGMENT;
            SColor command = SColor.SCHOOL_BUS_YELLOW;
            float sc = SColor.translucentColor(fade.toFloatBits(), 0.6f);
            for (int x = 0; x < diePane.gridWidth(); x++) {
                diePane.put(x, 0, sc);
                diePane.put(x, 1, sc);
                diePane.put(x, diePane.gridHeight() - 1, sc);
                diePane.put(x, diePane.gridHeight() - 2, sc);
            }
            for (int y = 0; y < diePane.gridHeight(); y++) {
                diePane.put(0, y, sc);
                diePane.put(1, y, sc);
                diePane.put(diePane.gridWidth() - 1, y, sc);
                diePane.put(diePane.gridWidth() - 2, y, sc);
            }
            sc = SColor.translucentColor(sc, 0.94f);
            for (int x = 2; x < diePane.gridWidth() - 2; x++) {
                for (int y = 2; y < diePane.gridHeight() - 2; y++) {
                    diePane.put(x, y, sc);
                }
            }

            String text;
            int x;
            int y = 3;
            int left = 5;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (diePane.gridWidth() - text.length()) / 2;//centered
            diePane.put(x, y, text, SColor.ELECTRIC_GREEN);
            y += 2;

            text = "Your peaceful life as a fisherman has come to an end.";
            x = left;//left justified
            diePane.put(x, y, text, SColor.WHITE);
            y += 1;
            text = "Because you died.";
            x = left;//left justified
            diePane.put(x, y, text, SColor.WHITE);
            y += 2;
            text = "It's lucky for you though, now you don't have to hear the";
            x = left;//left justified
            diePane.put(x, y, text, SColor.WHITE);
            y += 1;
            text = "screams of your friends and loved ones as they are torn apart";
            x = left;//left justified
            diePane.put(x, y, text, SColor.WHITE);
            y += 1;
            text = "by vicious elemental beings. You kinda don't mind Neumann dying though.";
            x = left;//left justified
            diePane.put(x, y, text, SColor.WHITE);
            y += 3;

            text = "Neumann stole your mail twice. That jerk. You died because:";
            x = left;//left justified
            diePane.put(x, y, text, SColor.WHITE);
            y += 1;
            x = left;//left justified
            diePane.put(x, y, reason, SColor.SAFETY_ORANGE);
            y += 1;

            text = "--  press right mouse to restart or left mouse to quit --";
            x = (diePane.gridWidth() - text.length()) / 2;//centered
            y = gridHeight - 3;
            diePane.put(x, y, text, SColor.ELECTRIC_GREEN);

            layers.setLayer(diePane, JLayeredPane.DRAG_LAYER);

            final long readTime = System.currentTimeMillis() + 200;
            diePane.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
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
        stage.addActor(diePane);
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
     * Attempts to move in the given direction. If a monster is in that direction then the player attacks the monster.
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
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                map[x][y].light = SColor.WHITE;
                mapPanel.put(x, y, map[x][y].getSymbol().charAt(0), map[x][y].foregroundColor());
            }
        }

        mapPanel.put(player.x, player.y, player.symbol.charAt(0));
    }

    private void updateFishInventoryPanel() {
        int x = 1;//start off with a bit of padding
//        fishInventoryPanel.removeHighlight();
        for (Element e : Element.values()) {
            int y = 1;
            for (Size s : Size.values()) {
                if (selectedFish != null && selectedFish.element == e && selectedFish.size == s) {
//                    fishInventoryPanel.highlight(x, y, x + maxFish - 1, y);
                }
                int n = fishInventory.get(e).get(s);
                for (int i = 0; i < maxFish; i++) {
                    if (i < n) {
                        fishInventoryPanel.put(x + i, y, Fish.symbol(s).charAt(0), e.color);
                    } else {
                        fishInventoryPanel.clear(x + i, y);
                    }
                }
                y++;
            }
            x += maxFish + 1;
        }

        for (x = 0; x < maxHealth; x++) {
            if (x < player.health) {
                fishInventoryPanel.put(x + healthX, 2, bobber, SColor.BLOOD);
            } else {
                fishInventoryPanel.clear(x + healthX, 2);
            }
        }
    }

    private void updateOverlay() {
        overlayPanel.clear();

        Radius strat = Radius.CIRCLE;

        if (selectedFish != null) {
            int radius = 1;
            Color c = selectedFish.color;
            float cf = SColor.floatGet(c.r, c.g, c.b, overlayAlpha / 255f);
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
                        overlayPanel.put(x, y, cf);
                    }
                }
            }
        } 
    }

    /**
     * Sets the output panel to show the message.
     *
     * @param message
     */
    private void printOut(String message) {

        outputPanel.erase();
        outputPanel.setVisible(true);

        outputPanel.put(outputPanel.gridWidth() - message.length() - 1, 1, message);
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
        double[][] walls = new double[gridWidth][gridHeight];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                walls[x][y] = map[x][y].isOpaque() ? 1.0 : 0.0;
            }
        }
        walls = fov.calculateFOV(walls, player.x, player.y, Math.min(gridWidth, gridHeight) / 3);
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                map[x][y].seen = walls[x][y] > 0.0;
            }
        }
    }

    private void createMap() {
        map = new MapCell[gridWidth][gridHeight];
        for (int x = 0; x < gridWidth; x++) {
            for (int y = 0; y < gridHeight; y++) {
                map[x][y] = new MapCell(Terrain.DIRT);
                if (rng.nextDouble() < 0.1) {
                    map[x][y].feature = TerrainFeature.TREE;
                } else if (rng.nextDouble() < 0.105) {
                    map[x][y].terrain = Terrain.makeElementalPool(Element.getRandomElement(rng));
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

        int x = (gridWidth / 2) - 2;
        int y = (gridHeight / 2) - 4;
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
            x = (gridWidth / 2) - 2;
        }

        for (int i = 0; i < 20; i++) {
            Creature creature = Creature.getRandomMonster();
            placeMonster(creature);
            monsters.add(creature);
        }

        player.x = gridWidth / 2;
        player.y = gridHeight / 2;
        MapCell cell = map[player.x][player.y];
        cell.creature = player;
        cell.terrain = Terrain.DIRT;
        cell.feature = null;
        cell.item = null;
    }

    /**
     * Randomly places a group of walls in the map. This replaces whatever was in that location previously.
     */
    private void placeWallChunk(Terrain t, TerrainFeature tf) {
        int spread = 5;
        int centerX = rng.nextInt(gridWidth);
        int centerY = rng.nextInt(gridHeight);

        for (int placeX = centerX - spread; placeX < centerX + spread; placeX++) {
            for (int placeY = centerY - spread; placeY < centerY + spread; placeY++) {
                if (rng.nextDouble() < 0.2 && placeX > 0 && placeX < gridWidth - 1 && placeY > 0 && placeY < gridHeight - 1) {
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

            x = rng.nextInt(gridWidth - 2) + 1;
            y = rng.nextInt(gridHeight - 2) + 1;
        } while (map[x][y].isBlocking() || map[x][y].creature != null || (x > gridWidth * 0.3 && x < gridWidth * 0.6 && y > gridHeight * 0.3 && y < gridHeight * 0.6));

        map[x][y].creature = monster;
        monster.x = x;
        monster.y = y;
    }

    public Coord getClosestWaypoint(Coord from, Coord to) {
        Coord[] line = Bresenham.line2D_(from, to);
        if (line.length < 2) {
            return null;
        }
        return line[1];
    }

    /**
     * Moves the monster given if possible. Monsters will not move into walls, other monsters, or the player.
     *
     * @param monster
     */
    private void moveMonster(Creature monster) {
//        Direction dir = Direction.CARDINALS[rng.nextInt(Direction.CARDINALS.length)];//get a random direction
        Coord p = getClosestWaypoint(Coord.get(monster.x, monster.y), Coord.get(player.x, player.y));
        Direction dir;
        dir = rng.getRandomElement(Direction.OUTWARDS);
        if (p != null) {
            dir = Direction.getDirection(p.x - monster.x, p.y - monster.y);
            if (map[p.x][p.y].isBlocking()) {
                dir = rng.getRandomElement(Direction.OUTWARDS);
            }
        }
        if (monster.x + dir.deltaX < 0 || monster.x + dir.deltaX >= gridWidth || monster.y + dir.deltaY < 0 || monster.y + dir.deltaY >= gridHeight) {
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
                if (d.deltaX + monster.x >= 0 & d.deltaX + monster.x < gridWidth && d.deltaY + monster.y >= 0 && d.deltaY + monster.y < gridHeight) {
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
        frame.getContentPane().setBackground(SColor.BLACK);
        frame.setUndecorated(true);

        layers = new JLayeredPane();
        frame.add(layers, BorderLayout.WEST);
        
        mapPanel = new SparseLayers(gridWidth, gridHeight, cellWidth, cellHeight, textFactory);

        mapPanel.setDefaultForeground(SColor.CREAM);
        layers.setPreferredSize(mapPanel.getPreferredSize());
        layers.setLayer(mapPanel, JLayeredPane.DEFAULT_LAYER);
        layers.add(mapPanel);

        overlayPanel = new SwingPane(mapPanel.gridWidth(), mapPanel.gridHeight(), textFactory, null);
        overlayPanel.refresh();
        layers.setLayer(overlayPanel, JLayeredPane.PALETTE_LAYER);
        layers.add(overlayPanel);

        outputPanel = new SwingPane(mapPanel.gridWidth(), mapPanel.gridHeight(), textFactory, null);
//        outputPanel.setDefaultBackground(SColor.BLACK_KITE);
        outputPanel.setDefaultForeground(SColor.TEA_GREEN);
        outputPanel.refresh();
        layers.setLayer(outputPanel, JLayeredPane.POPUP_LAYER);
        layers.add(outputPanel);

        fishThrowingPanel = new SwingPane(mapPanel.gridWidth(), mapPanel.gridHeight(), textFactory, null);
//        fishThrowingPanel.setDefaultBackground(SColor.TRANSPARENT);
        fishThrowingPanel.setDefaultForeground(SColor.LOQUAT_BROWN);
        fishThrowingPanel.refresh();
        layers.setLayer(fishThrowingPanel, JLayeredPane.POPUP_LAYER);
        layers.add(fishThrowingPanel);

        fishText = new TextCellFactory(font, mapPanel.cellWidth(), mapPanel.cellHeight(), true, 1, 0, 0, 0, fitting);
        fishInventoryPanel = new SwingPane(mapPanel.gridWidth(), 6, fishText, null);
//        fishInventoryPanel.setDefaultBackground(SColor.BLACK);
        frame.add(fishInventoryPanel, BorderLayout.SOUTH);

        frame.pack();

        fishInventoryPanel.addMouseListener(new MenuMouse(fishInventoryPanel.cellWidth(), fishInventoryPanel.cellHeight()));
        mapMouse = new MapMouse(mapPanel.cellWidth(), mapPanel.cellHeight());
        mapKeys = new MapKeys();
        inventoryMouse = new FishInventoryMouse(fishInventoryPanel.cellWidth(), fishInventoryPanel.cellHeight());

        //add invisibly the fishing panels
        fishMouse = new FishMouse();
        initFishingGUI();
    }

    private void flipMouseControl(boolean mapMode) {
        if (mapMode) {
            mapPanel.addMouseListener(mapMouse);
            mapPanel.addMouseMotionListener(mapMouse);
            mapPanel.addMouseWheelListener(mapMouse);
            frame.addKeyListener(mapKeys);
            fishInventoryPanel.addMouseListener(inventoryMouse);
            frame.removeMouseListener(fishMouse);
        } else {
            mapPanel.removeMouseListener(mapMouse);
            mapPanel.removeMouseMotionListener(mapMouse);
            mapPanel.removeMouseWheelListener(mapMouse);
            frame.removeKeyListener(mapKeys);
            fishInventoryPanel.removeMouseListener(inventoryMouse);
            frame.addMouseListener(fishMouse);
        }
    }

    private void initializeFishInventory() {
        SparseLayers p = fishInventoryPanel;
        for (int x = 0; x < p.gridWidth(); x++) {
            for (int y = 0; y < p.gridHeight(); y++) {
                p.clear(x, y);
            }
        }

        int x = 1;//start off with a bit of padding
        for (Element e : Element.values()) {
            p.put(x, 0, e.name, e.color);//, SColor.BLACK);
            x += maxFish + 1;
        }

        p.put(healthX, 1, "Health", SColor.BLOOD);//, p.getBackground());

        p.put((int)helpIconLocation.x, (int)helpIconLocation.y, "HELP", SColor.CREAM);//, p.getBackground());
        p.put((int)muteIconLocation.x, (int)muteIconLocation.y, "MUTE", SColor.SAFETY_ORANGE);//, p.getBackground());
        p.put((int)exitIconLocation.x, (int)exitIconLocation.y, "EXIT", SColor.BRILLIANT_ROSE);//, p.getBackground());
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
                overlayLocation = Coord.get(e.getX(), e.getY());
                updateOverlay();
            }
        }

    }

    private class MapKeys extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (canClick) {
                canClick = false;
                Direction d = getDirectionFromKey(e.getExtendedKeyCode());
                if (d != null) {
                    workClick(d.deltaX + player.x, d.deltaY + player.y);
                }
                canClick = true;
            }
        }

    }

    private Direction getDirectionFromKey(int code) {
        switch (code) {
            case VK_LEFT:
            case VK_NUMPAD4:
            case VK_H:
                return LEFT;
            case VK_RIGHT:
            case VK_NUMPAD6:
            case VK_L:
                return RIGHT;
            case VK_UP:
            case VK_NUMPAD8:
            case VK_K:
                return UP;
            case VK_DOWN:
            case VK_NUMPAD2:
            case VK_J:
                return DOWN;
            case VK_NUMPAD1:
            case VK_B:
                return DOWN_LEFT;
            case VK_NUMPAD3:
            case VK_N:
                return DOWN_RIGHT;
            case VK_NUMPAD7:
            case VK_Y:
                return UP_LEFT;
            case VK_NUMPAD9:
            case VK_U:
                return UP_RIGHT;
            case VK_PERIOD:
            case VK_NUMPAD5:
                return NONE;
            default:
                return null;
        }
    }

    private void initFishingGUI() {
        
        TextCellFactory fishingFactory = DefaultResources.getCrispDejaVuFont().width(fishCellWidth).height(fishCellHeight).initBySize();
        fishingLayers = new SparseLayers(fishWidth, fishHeight, fishCellWidth, fishCellHeight, fishingFactory);
        // layer 0 is fishing view
        fishingLayers.addLayer(); // layer 1 is "fish pane"
        fishingLayers.addLayer(); // layer 2 is for the player
        stage.add(fishingLayers);
        meterPanel = new SparseLayers(gridWidth, 3, cellWidth, cellHeight, textFactory);
        initMeter();
        stage.add(meterPanel);
    }

    private void dropHook() {
        fishingLayers.put(bobberLocation.x, bobberLocation.y + 1, hook, hookColor, null, 0);
        int x = bobberLocation.x;
        int y;
        for (y = bobberLocation.y + 2; y <= bed(x); y++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            fishingLayers.put(x, y - 1, line(UP), lineColor);
            fishingLayers.put(x, y, hook, hookColor);
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }

        Fish fish = null;
        do {
            fishingLayers.clear(x, y);

            y--;
            fishingLayers.put(x, y, hook, hookColor);
            if (fish != null) {
                fishingLayers.put(x, y, fish.symbol.charAt(0), fish.color);
            } else if (fishMap[x][y] != null) {
                fish = fishMap[x][y];
                fishes.remove(fish);
                fishMap[x][y] = null;
                fishingLayers.clear(x, y, 1);
            }

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
                fishingLayers.put(lastX, lastY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
                fishingLayers.put(bobberX, bobberY, bobber, bobberColor);
                lastX = bobberX;
                lastY = bobberY;
                goingDown = false;
            } else if (Math.abs(bobberY - lastY) > 1 || (goingDown && bobberY != lastY)) {
                fishingLayers.put(lastX, lastY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
                fishingLayers.put(bobberX, bobberY, bobber, bobberColor);
                lastX = bobberX;
                lastY = bobberY;
                goingDown = true;
            } else if (bobberY != lastY) {
                fishingLayers.clear(lastX, lastY);
                goingDown = false;
            }

            Thread.yield();
        }

        fishingLayers.put(bobberX, bobberY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
        fishingLayers.put(bobberX, bobberY + 1, bobber, bobberColor);
        bobberLocation = Coord.get(bobberX, bobberY + 1);

        for (int x = 1; x < meterPanel.gridWidth() - 1; x++) {
            meterPanel.put(x, 1, ' ');
        }
    }

    /**
     * Returns the y position of the last space before the terrain bed.
     *
     * To allow for bounds safety, this method will return 0 as the result if the bed reaches the top rather than -1.
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
                    fishingLayers.put(x, y, getTerrainColor(x, y));
                } else if (liquidMap[x][y]) {
                    fishingLayers.put(x, y, getLiquidColor(x, y));
                } else {
                    fishingLayers.put(x, y, getSkyColor(x, y));
                }
            }
        }

//        fishPane.erase();
        for (Fish f : fishes) {
            fishingLayers.put(f.x, f.y, f.symbol.charAt(0), f.color, null, 1);
        } 
    }

    private float getTerrainColor(int x, int y) {
        return SColor.lerpFloatColors(terrain.color, SColor.FLOAT_BLACK, WhirlingNoise.noiseAlt(x, y) * 0.125f + 0.125f);
    }

    private float getLiquidColor(int x, int y) {
        return SColor.lerpFloatColors(SColor.lerpFloatColors(element.floatColor, SColor.FLOAT_BLACK, WhirlingNoise.noiseAlt(x, y) * 0.125f + 0.125f),
                SColor.lerpFloatColors(element.floatColor, SColor.FLOAT_BLACK, 0.7f), y / (float) (fishHeight - liquidHeight));
    }

    private float getSkyColor(int x, int y) {
        return SColor.lerpFloatColors(SColor.lerpFloatColors(skyColor, SColor.FLOAT_WHITE, 0.6f), SColor.lerpFloatColors(skyColor, SColor.FLOAT_BLACK, 0.1f),
                y / (float) liquidHeight);
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
        fishingLayers.put(1, 2, '@', playerColor, null, 2);
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
        meterPanel.put((meterPanel.gridWidth() - "Cast Strength".length() - 1) / 2, 0, "Cast Strength", SColor.FLOAT_BLACK, 0f);
        meterPanel.put(2, 0, "None", SColor.FLOAT_BLACK, 0f);
        meterPanel.put(meterPanel.gridWidth() - 3 - "Max".length(), 0, "Max", SColor.FLOAT_BLACK, 0f);
    }

    private class MeterListener implements ActionListener {

        double timeStep = 1000;//how many milliseconds per time step
        int meterOffset = 3;
        int meterSize;
        long time, lastTime;

        public void reset() {
            for (int i = 0; i < meterPanel.gridWidth(); i++) {
                meterPanel.clear(i, 1);
            }
        }

        public void initialize() {
            meterSize = gridWidth - meterOffset * 2;
            time = (long) (1000 * Math.PI / 2.0);
            lastTime = System.currentTimeMillis();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            castingStrength = 1 - Math.abs(NumberTools.sin(time / timeStep));
            int drawX = (int) (castingStrength * meterSize);
            drawX = Math.min(drawX, meterSize);//make sure rare case of strength 1 doesn't cause problems
            for (int i = 0; i < meterSize; i++) {
                if (i < drawX) {
                    meterPanel.put(i + meterOffset, 1, '●', SColorFactory.fromPallet("meter", i / (float) (meterSize)));
                } else {
                    meterPanel.clear(i + meterOffset, 1);
                }
            }

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
                stage.remove(fishingLayers);
                nowFishing = false;
                flipMouseControl(true);
            }
        }

    }

}
