package com.squidpony.assaultfish;

import assaultfish.mapping.MapCell;
import assaultfish.physical.*;
import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import squidpony.ArrayTools;
import squidpony.FakeLanguageGen;
import squidpony.NaturalLanguageCipher;
import squidpony.StringKit;
import squidpony.squidai.DijkstraMap;
import squidpony.squidgrid.Direction;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.*;
import squidpony.squidgrid.mapping.DungeonGenerator;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.mapping.LineKit;
import squidpony.squidmath.*;

import java.util.ArrayList;
import java.util.Queue;
import java.util.TreeMap;

/**
 * This is a small, not-overly-simple demo that presents some important features of SquidLib and shows a faster,
 * cleaner, and more recently-introduced way of displaying the map and other text. Features include dungeon map
 * generation, field of view, pathfinding (to the mouse position), continuous noise (used for a wavering torch effect),
 * language generation/ciphering, a colorful glow effect, and ever-present random number generation (with a seed).
 * You can increase the size of the map on most target platforms (but GWT struggles with large... anything) by
 * changing gridHeight and gridWidth to affect the visible area or bigWidth and bigHeight to adjust the size of the
 * dungeon you can move through, with the camera following your '@' symbol.
 * <br>
 * The assets folder of this project, if it was created with SquidSetup, will contain the necessary font files (just one
 * .fnt file and one .png are needed, but many more are included by default). You should move any font files you don't
 * use out of the assets directory when you produce a release JAR, APK, or GWT build.
 */
public class AssaultFish extends ApplicationAdapter {
    SpriteBatch batch;
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
            fishCellHeight = 20, 
            bonusHeight = 5;
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
    private assaultfish.AssaultFish.MeterListener meterListener;
    private assaultfish.AssaultFish.FishMouse fishMouse;
    private assaultfish.AssaultFish.MapMouse mapMouse;
    private assaultfish.AssaultFish.MapKeys mapKeys;
    private assaultfish.AssaultFish.FishInventoryMouse inventoryMouse;

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
            skyColor = SColor.ALICE_BLUE,
            playerColor = SColor.BETEL_NUT_DYE;
    private ArrayList<Color> meterPalette;
    private boolean nowFishing = false;

    private static final String SOUND_PREF = "Sound Pref";

    private SquidInput input;
    private Color bgColor;
    private Stage stage, languageStage;
    private double[][] resistance;
    private double[][] visible;
    // GreasedRegion is a hard-to-explain class, but it's an incredibly useful one for map generation and many other
    // tasks; it stores a region of "on" cells where everything not in that region is considered "off," and can be used
    // as a Collection of Coord points. However, it's more than that! Because of how it is implemented, it can perform
    // bulk operations on as many as 64 points at a time, and can efficiently do things like expanding the "on" area to
    // cover adjacent cells that were "off", retracting the "on" area away from "off" cells to shrink it, getting the
    // surface ("on" cells that are adjacent to "off" cells) or fringe ("off" cells that are adjacent to "on" cells),
    // and generally useful things like picking a random point from all "on" cells.
    // Here, we use a GreasedRegion to store all floors that the player can walk on, a small rim of cells just beyond
    // the player's vision that blocks pathfinding to areas we can't see a path to, and we also store all cells that we
    // have seen in the past in a GreasedRegion (in most roguelikes, there would be one of these per dungeon floor).
    private GreasedRegion floors, blockage, seen;
    // a Glyph is a kind of scene2d Actor that only holds one char in a specific color, but is drawn using the behavior
    // of TextCellFactory (which most text in SquidLib is drawn with) instead of the different and not-very-compatible
    // rules of Label, which older SquidLib code used when it needed text in an Actor. Glyphs are also lighter-weight in
    // memory usage and time taken to draw than Labels.
    private TextCellFactory.Glyph pg;

    @Override
    public void create () {
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

        //Some classes in SquidLib need access to a batch to render certain things, so it's a good idea to have one.
        batch = new SpriteBatch();
        StretchViewport mainViewport = new StretchViewport(gridWidth * cellWidth, gridHeight * cellHeight),
                languageViewport = new StretchViewport(gridWidth * cellWidth, bonusHeight * cellHeight);
        mainViewport.setScreenBounds(0, 0, gridWidth * cellWidth, gridHeight * cellHeight);
        languageViewport
                .setScreenBounds(0, 0, gridWidth * cellWidth, bonusHeight * cellHeight);
        //Here we make sure our Stage, which holds any text-based grids we make, uses our Batch.
        stage = new Stage(mainViewport, batch);
        languageStage = new Stage(languageViewport, batch);
        // the font will try to load Iosevka Slab as an embedded bitmap font with a distance field effect.
        // the distance field effect allows the font to be stretched without getting blurry or grainy too easily.
        // this font is covered under the SIL Open Font License (fully free), so there's no reason it can't be used.
        // It is included in the assets folder if this project was made with SquidSetup, along with other fonts
        // Another option to consider is DefaultResources.getSlabFamily(), which uses the same font (Iosevka Slab) but
        // treats it differently, and can be used to draw bold and/or italic text at the expense of the font being
        // slightly less detailed visually and some rare glyphs being omitted. Bold and italic text are usually handled
        // with markup in text that is passed to SquidLib's GDXMarkup class; see GDXMarkup's docs for more info.
        // There are also several other distance field fonts, including two more font families like
        // DefaultResources.getSlabFamily() that allow bold/italic text. Although some BitmapFont assets are available
        // without a distance field effect, they are discouraged for most usage because they can't cleanly resize
        // without loading a different BitmapFont per size, and there's usually just one size in DefaultResources.
        display = new SparseLayers(bigWidth, bigHeight + bonusHeight, cellWidth, cellHeight,
                DefaultResources.getCrispDejaVuFont());

        // A bit of a hack to increase the text height slightly without changing the size of the cells they're in.
        // This causes a tiny bit of overlap between cells, which gets rid of an annoying gap between solid lines.
        // If you use '#' for walls instead of box drawing chars, you don't need this.
        // If you don't use DefaultResources.getStretchableSlabFont(), you may need to adjust the multipliers here.
        //display.font.tweakWidth(cellWidth * 1.125f).tweakHeight(cellHeight * 1.075f).initBySize();

        languageDisplay = new SparseLayers(gridWidth, bonusHeight - 1, cellWidth, cellHeight, display.font);
        // SparseLayers doesn't currently use the default background fields, but this isn't really a problem; we can
        // set the background colors directly as floats with the SparseLayers.backgrounds field, and it can be handy
        // to hold onto the current color we want to fill that with in the defaultPackedBackground field.
        //SparseLayers has fillBackground() and fillArea() methods for coloring all or part of the backgrounds.
        languageDisplay.defaultPackedBackground = FLOAT_LIGHTING;

        //This uses the seeded RNG we made earlier to build a procedural dungeon using a method that takes rectangular
        //sections of pre-drawn dungeon and drops them into place in a tiling pattern. It makes good winding dungeons
        //with rooms by default, but in the later call to dungeonGen.generate(), you can use a TilesetType such as
        //TilesetType.ROUND_ROOMS_DIAGONAL_CORRIDORS or TilesetType.CAVES_LIMIT_CONNECTIVITY to change the sections that
        //this will use, or just pass in a full 2D char array produced from some other generator, such as
        //SerpentMapGenerator, OrganicMapGenerator, or DenseRoomMapGenerator.
        dungeonGen = new DungeonGenerator(bigWidth, bigHeight, rng);
        //uncomment this next line to randomly add water to the dungeon in pools.
        //dungeonGen.addWater(15);
        //decoDungeon is given the dungeon with any decorations we specified. (Here, we didn't, unless you chose to add
        //water to the dungeon. In that case, decoDungeon will have different contents than bareDungeon, next.)
        decoDungeon = dungeonGen.generate();
        //getBareDungeon provides the simplest representation of the generated dungeon -- '#' for walls, '.' for floors.
        bareDungeon = dungeonGen.getBareDungeon();
        //When we draw, we may want to use a nicer representation of walls. DungeonUtility has lots of useful methods
        //for modifying char[][] dungeon grids, and this one takes each '#' and replaces it with a box-drawing char.
        //The end result looks something like this, for a smaller 60x30 map:
        //
        // ┌───┐┌──────┬──────┐┌──┬─────┐   ┌──┐    ┌──────────┬─────┐
        // │...││......│......└┘..│.....│   │..├───┐│..........│.....└┐
        // │...││......│..........├──┐..├───┤..│...└┴────......├┐.....│
        // │...││.................│┌─┘..│...│..│...............││.....│
        // │...││...........┌─────┘│....│...│..│...........┌───┴┴───..│
        // │...│└─┐....┌───┬┘      │........│..│......─────┤..........│
        // │...└─┐│....│...│       │.......................│..........│
        // │.....││........└─┐     │....│..................│.....┌────┘
        // │.....││..........│     │....├─┬───────┬─┐......│.....│
        // └┬──..└┼───┐......│   ┌─┴─..┌┘ │.......│ │.....┌┴──┐..│
        //  │.....│  ┌┴─..───┴───┘.....└┐ │.......│┌┘.....└─┐ │..│
        //  │.....└──┘..................└─┤.......││........│ │..│
        //  │.............................│.......├┘........│ │..│
        //  │.............┌──────┐........│.......│...─┐....│ │..│
        //  │...........┌─┘      └──┐.....│..─────┘....│....│ │..│
        // ┌┴─────......└─┐      ┌──┘..................│..──┴─┘..└─┐
        // │..............└──────┘.....................│...........│
        // │............................┌─┐.......│....│...........│
        // │..│..│..┌┐..................│ │.......├────┤..──┬───┐..│
        // │..│..│..│└┬──..─┬───┐......┌┘ └┐.....┌┘┌───┤....│   │..│
        // │..├──┤..│ │.....│   │......├───┘.....│ │...│....│┌──┘..└──┐
        // │..│┌─┘..└┐└┬─..─┤   │......│.........└─┘...│....││........│
        // │..││.....│ │....│   │......│...............│....││........│
        // │..││.....│ │....│   │......│..┌──┐.........├────┘│..│.....│
        // ├──┴┤...│.└─┴─..┌┘   └┐....┌┤..│  │.....│...└─────┘..│.....│
        // │...│...│.......└─────┴─..─┴┘..├──┘.....│............└─────┤
        // │...│...│......................│........│..................│
        // │.......├───┐..................│.......┌┤.......┌─┐........│
        // │.......│   └──┐..┌────┐..┌────┤..┌────┘│.......│ │..┌──┐..│
        // └───────┘      └──┘    └──┘    └──┘     └───────┘ └──┘  └──┘
        //this is also good to compare against if the map looks incorrect, and you need an example of a correct map when
        //no parameters are given to generate().
        lineDungeon = DungeonUtility.hashesToLines(decoDungeon);

        resistance = DungeonUtility.generateResistances(decoDungeon);
        visible = new double[bigWidth][bigHeight];

        //Coord is the type we use as a general 2D point, usually in a dungeon.
        //Because we know dungeons won't be incredibly huge, Coord performs best for x and y values less than 256, but
        // by default it can also handle some negative x and y values (-3 is the lowest it can efficiently store). You
        // can call Coord.expandPool() or Coord.expandPoolTo() if you need larger maps to be just as fast.
        cursor = Coord.get(-1, -1);
        // here, we need to get a random floor cell to place the player upon, without the possibility of putting him
        // inside a wall. There are a few ways to do this in SquidLib. The most straightforward way is to randomly
        // choose x and y positions until a floor is found, but particularly on dungeons with few floor cells, this can
        // have serious problems -- if it takes too long to find a floor cell, either it needs to be able to figure out
        // that random choice isn't working and instead choose the first it finds in simple iteration, or potentially
        // keep trying forever on an all-wall map. There are better ways! These involve using a kind of specific storage
        // for points or regions, getting that to store only floors, and finding a random cell from that collection of
        // floors. The two kinds of such storage used commonly in SquidLib are the "packed data" as short[] produced by
        // CoordPacker (which use very little memory, but can be slow, and are treated as unchanging by CoordPacker so
        // any change makes a new array), and GreasedRegion objects (which use slightly more memory, tend to be faster
        // on almost all operations compared to the same operations with CoordPacker, and default to changing the
        // GreasedRegion object when you call a method on it instead of making a new one). Even though CoordPacker
        // sometimes has better documentation, GreasedRegion is generally a better choice; it was added to address
        // shortcomings in CoordPacker, particularly for speed, and the worst-case scenarios for data in CoordPacker are
        // no problem whatsoever for GreasedRegion. CoordPacker is called that because it compresses the information
        // for nearby Coords into a smaller amount of memory. GreasedRegion is called that because it encodes regions,
        // but is "greasy" both in the fatty-food sense of using more space, and in the "greased lightning" sense of
        // being especially fast. Both of them can be seen as storing regions of points in 2D space as "on" and "off."

        // Here we fill a GreasedRegion so it stores the cells that contain a floor, the '.' char, as "on."
        floors = new GreasedRegion(bareDungeon, '.');
        //player is, here, just a Coord that stores his position. In a real game, you would probably have a class for
        //creatures, and possibly a subclass for the player. The singleRandom() method on GreasedRegion finds one Coord
        //in that region that is "on," or -1,-1 if there are no such cells. It takes an RNG object as a parameter, and
        //if you gave a seed to the RNG constructor, then the cell this chooses will be reliable for testing. If you
        //don't seed the RNG, any valid cell should be possible.
        player = floors.singleRandom(rng);

        //These need to have their positions set before adding any entities if there is an offset involved.
        //There is no offset used here, but it's still a good practice here to set positions early on.
        display.setPosition(0f, 0f);
        // Uses shadowcasting FOV and reuses the visible array without creating new arrays constantly.
        FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0, Radius.CIRCLE);
        // 0.0 is the upper bound (inclusive), so any Coord in visible that is more well-lit than 0.0 will _not_ be in
        // the blockage Collection, but anything 0.0 or less will be in it. This lets us use blockage to prevent access
        // to cells we can't see from the start of the move.
        blockage = new GreasedRegion(visible, 0.0);
        // Here we mark the initially seen cells as anything that wasn't included in the unseen "blocked" region.
        // We invert the copy's contents to prepare for a later step, which makes blockage contain only the cells that
        // are above 0.0, then copy it to save this step as the seen cells. We will modify seen later independently of
        // the blocked cells, so a copy is correct here. Most methods on GreasedRegion objects will modify the
        // GreasedRegion they are called on, which can greatly help efficiency on long chains of operations.
        seen = blockage.not().copy();
        // Here is one of those methods on a GreasedRegion; fringe8way takes a GreasedRegion (here, the set of cells
        // that are visible to the player), and modifies it to contain only cells that were not in the last step, but
        // were adjacent to a cell that was present in the last step. This can be visualized as taking the area just
        // beyond the border of a region, using 8-way adjacency here because we specified fringe8way instead of fringe.
        // We do this because it means pathfinding will only have to work with a small number of cells (the area just
        // out of sight, and no further) instead of all invisible cells when figuring out if something is currently
        // impossible to enter.
        blockage.fringe8way();
        
        // prunedDungeon starts with the full lineDungeon, which includes features like water and grass but also stores
        // all walls as box-drawing characters. The issue with using lineDungeon as-is is that a character like '┬' may
        // be used because there are walls to the east, west, and south of it, even when the player is to the north of
        // that cell and so has never seen the southern connecting wall, and would have no reason to know it is there.
        // By calling LineKit.pruneLines(), we adjust prunedDungeon to hold a variant on lineDungeon that removes any
        // line segments that haven't ever been visible. This is called again whenever seen changes. 
        prunedDungeon = ArrayTools.copy(lineDungeon);
        // We call pruneLines with an optional parameter here, LineKit.lightAlt, which will allow prunedDungeon to use
        // the half-line chars "╴╵╶╷". These chars aren't supported by all fonts, but they are by the one we use here.
        // The default is to use LineKit.light , which will replace '╴' and '╶' with '─' and '╷' and '╵' with '│'.
        LineKit.pruneLines(lineDungeon, seen, LineKit.lightAlt, prunedDungeon);
        
        //This is used to allow clicks or taps to take the player to the desired area.
        toCursor = new ArrayList<>(200);
        //When a path is confirmed by clicking, we draw from this List to find which cell is next to move into.
        awaitedMoves = new ArrayList<>(200);
        //DijkstraMap is the pathfinding swiss-army knife we use here to find a path to the latest cursor position.
        //DijkstraMap.Measurement is an enum that determines the possibility or preference to enter diagonals. Here, the
        //MANHATTAN value is used, which means 4-way movement only, no diagonals possible. Alternatives are CHEBYSHEV,
        //which allows 8 directions of movement at the same cost for all directions, and EUCLIDEAN, which allows 8
        //directions, but will prefer orthogonal moves unless diagonal ones are clearly closer "as the crow flies."
        playerToCursor = new DijkstraMap(decoDungeon, DijkstraMap.Measurement.MANHATTAN);
        //These next two lines mark the player as something we want paths to go to or from, and get the distances to the
        // player from all walkable cells in the dungeon.
        playerToCursor.setGoal(player);
        // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
        // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
        // GreasedRegion that contains the cells just past the edge of the player's FOV area.
        playerToCursor.partialScan(13, blockage);


        //The next three lines set the background color for anything we don't draw on, but also create 2D arrays of the
        //same size as decoDungeon that store the colors for the foregrounds and backgrounds of each cell as packed
        //floats (a format SparseLayers can use throughout its API), using the colors for the cell with the same x and
        //y. By changing an item in SColor.LIMITED_PALETTE, we also change the color assigned by MapUtility to floors.
        bgColor = SColor.DARK_SLATE_GRAY;
        SColor.LIMITED_PALETTE[3] = SColor.DB_GRAPHITE;
        colors = MapUtility.generateDefaultColorsFloat(decoDungeon);
        bgColors = MapUtility.generateDefaultBGColorsFloat(decoDungeon);


        //places the player as an '@' at his position in orange.
        pg = display.glyph('@', SColor.SAFETY_ORANGE.toFloatBits(), player.x, player.y);

        lang = new ArrayList<>(16);
        // StringKit has various utilities for dealing with text, including wrapping text so it fits in a specific width
        // and inserting the lines into a List of Strings, as we do here with the List lang and the text artOfWar.
        StringKit.wrap(lang, artOfWar, gridWidth - 2);
        // FakeLanguageGen.registered is an array of the hand-made languages in FakeLanguageGen, not any random ones and
        // not most mixes of multiple languages. We get a random language from it with our RNG, and use that to build
        // our current NaturalLanguageCipher. This NaturalLanguageCipher will act as an English-to-X dictionary for
        // whatever X is our randomly chosen language, and will try to follow the loose rules English follows when
        // it translates a word into an imaginary word in the fake language.
        translator = new NaturalLanguageCipher(rng.getRandomElement(FakeLanguageGen.registered));
        StringKit.wrap(lang, translator.cipher(artOfWar), gridWidth - 2);
        // the 0L here can be used to adjust the languages generated; it acts a little like a seed for an RNG.
        translator.initialize(rng.getRandomElement(FakeLanguageGen.registered), 0L);

        // this is a big one.
        // SquidInput can be constructed with a KeyHandler (which just processes specific keypresses), a SquidMouse
        // (which is given an InputProcessor implementation and can handle multiple kinds of mouse move), or both.
        // keyHandler is meant to be able to handle complex, modified key input, typically for games that distinguish
        // between, say, 'q' and 'Q' for 'quaff' and 'Quip' or whatever obtuse combination you choose. The
        // implementation here handles hjkl keys (also called vi-keys), numpad, arrow keys, and wasd for 4-way movement.
        // Shifted letter keys produce capitalized chars when passed to KeyHandler.handle(), but we don't care about
        // that so we just use two case statements with the same body, i.e. one for 'A' and one for 'a'.
        // You can also set up a series of future moves by clicking within FOV range, using mouseMoved to determine the
        // path to the mouse position with a DijkstraMap (called playerToCursor), and using touchUp to actually trigger
        // the event when someone clicks.
        input = new SquidInput(new SquidInput.KeyHandler() {
            @Override
            public void handle(char key, boolean alt, boolean ctrl, boolean shift) {
                switch (key)
                {
                    case SquidInput.UP_ARROW:
                    case 'k':
                    case 'w':
                    case 'K':
                    case 'W':
                    {
                        toCursor.clear();
                        //-1 is up on the screen
                        awaitedMoves.add(player.translate(0, -1));
                        break;
                    }
                    case SquidInput.DOWN_ARROW:
                    case 'j':
                    case 's':
                    case 'J':
                    case 'S':
                    {
                        toCursor.clear();
                        //+1 is down on the screen
                        awaitedMoves.add(player.translate(0, 1));
                        break;
                    }
                    case SquidInput.LEFT_ARROW:
                    case 'h':
                    case 'a':
                    case 'H':
                    case 'A':
                    {
                        toCursor.clear();
                        awaitedMoves.add(player.translate(-1, 0));
                        break;
                    }
                    case SquidInput.RIGHT_ARROW:
                    case 'l':
                    case 'd':
                    case 'L':
                    case 'D':
                    {
                        toCursor.clear();
                        awaitedMoves.add(player.translate(1, 0));
                        break;
                    }
                    case 'Q':
                    case 'q':
                    case SquidInput.ESCAPE:
                    {
                        Gdx.app.exit();
                        break;
                    }
                }
            }
        },
                //The second parameter passed to a SquidInput can be a SquidMouse, which takes mouse or touchscreen
                //input and converts it to grid coordinates (here, a cell is 10 wide and 20 tall, so clicking at the
                // pixel position 16,51 will pass screenX as 1 (since if you divide 16 by 10 and round down you get 1),
                // and screenY as 2 (since 51 divided by 20 rounded down is 2)).
                new SquidMouse(cellWidth, cellHeight, gridWidth, gridHeight, 0, 0, new InputAdapter() {

            // if the user clicks and mouseMoved hasn't already assigned a path to toCursor, then we call mouseMoved
            // ourselves and copy toCursor over to awaitedMoves.
            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                mouseMoved(screenX, screenY);
                awaitedMoves.addAll(toCursor);
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                return mouseMoved(screenX, screenY);
            }

            // causes the path to the mouse position to become highlighted (toCursor contains a list of Coords that
            // receive highlighting). Uses DijkstraMap.findPathPreScanned() to find the path, which is rather fast.
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if(!awaitedMoves.isEmpty())
                    return false;
                // This is needed because we center the camera on the player as he moves through a dungeon that is
                // multiple screens wide and tall, but the mouse still only can receive input on one screen's worth
                // of cells. (gridWidth >> 1) halves gridWidth, pretty much, and that we use to get the centered
                // position after adding to the player's position (along with the gridHeight).
                screenX += player.x - (gridWidth >> 1);
                screenY += player.y - (gridHeight >> 1);
                // we also need to check if screenX or screenY is out of bounds.
                if(screenX < 0 || screenY < 0 || screenX >= bigWidth || screenY >= bigHeight ||
                        (cursor.x == screenX && cursor.y == screenY))
                {
                    return false;
                }
                cursor = Coord.get(screenX, screenY);
                // This uses DijkstraMap.findPathPreScannned() to get a path as a List of Coord from the current
                // player position to the position the user clicked on. The "PreScanned" part is an optimization
                // that's special to DijkstraMap; because the part of the map that is viable to move into has
                // already been fully analyzed by the DijkstraMap.partialScan() method at the start of the
                // program, and re-calculated whenever the player moves, we only need to do a fraction of the
                // work to find the best path with that info.
                toCursor = playerToCursor.findPathPreScanned(cursor);
                // findPathPreScanned includes the current cell (goal) by default, which is helpful when
                // you're finding a path to a monster or loot, and want to bump into it, but here can be
                // confusing because you would "move into yourself" as your first move without this.
                // Getting a sublist avoids potential performance issues with removing from the start of an
                // ArrayList, since it keeps the original list around and only gets a "view" of it.
                if(!toCursor.isEmpty())
                {
                    toCursor = toCursor.subList(1, toCursor.size());
                }
                return false;
            }
        }));
        //Setting the InputProcessor is ABSOLUTELY NEEDED TO HANDLE INPUT
        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
        //You might be able to get by with the next line instead of the above line, but the former is preferred.
        //Gdx.input.setInputProcessor(input);
        //we add display, our one visual component that moves, to the list of things that act in the main Stage.
        stage.addActor(display);
        //we add languageDisplay to languageStage, where it will be unchanged by camera moves in the main Stage.
        languageStage.addActor(languageDisplay);


    }
    /**
     * Move the player if he isn't bumping into a wall or trying to go off the map somehow.
     * In a fully-fledged game, this would not be organized like this, but this is a one-file demo.
     * @param xmod
     * @param ymod
     */
    private void move(int xmod, int ymod) {
        int newX = player.x + xmod, newY = player.y + ymod;
        if (newX >= 0 && newY >= 0 && newX < bigWidth && newY < bigHeight
                && bareDungeon[newX][newY] != '#')
        {
            display.slide(pg, player.x, player.y, newX, newY, 0.11f, null);
            player = player.translate(xmod, ymod);
            FOV.reuseFOV(resistance, visible, player.x, player.y, 9.0, Radius.CIRCLE);
            // This is just like the constructor used earlier, but affects an existing GreasedRegion without making
            // a new one just for this movement.
            blockage.refill(visible, 0.0);
            seen.or(blockage.not());
            blockage.fringe8way();
            // By calling LineKit.pruneLines(), we adjust prunedDungeon to hold a variant on lineDungeon that removes any
            // line segments that haven't ever been visible. This is called again whenever seen changes.
            LineKit.pruneLines(lineDungeon, seen, LineKit.lightAlt, prunedDungeon);
        }
        else
        {
            // A SparseLayers knows how to move a Glyph (like the one for the player, pg) out of its normal alignment
            // on the grid, and also how to move it back again. Using bump() will move pg quickly about a third of the
            // way into a wall, then back to its former position at normal speed.
            display.bump(pg, Direction.getRoughDirection(xmod, ymod), 0.25f);
            // PanelEffect is a type of Action (from libGDX) that can run on a SparseLayers or SquidPanel.
            // This particular kind of PanelEffect creates a purple glow around the player when he bumps into a wall.
            // Other kinds can make explosions or projectiles appear.
            display.addAction(new PanelEffect.PulseEffect(display, 1f, floors, player, 3
                    , new float[]{SColor.CW_FADED_PURPLE.toFloatBits()}
                    ));
        }
        // removes the first line displayed of the Art of War text or its translation.
        lang.remove(0);
        // if the last line reduced the number of lines we can show to less than what we try to show, we fill in more
        // lines using a randomly selected fake language to translate the same Art of War text.
        while (lang.size() < bonusHeight - 1)
        {
            StringKit.wrap(lang, translator.cipher(artOfWar), gridWidth - 2);
            translator.initialize(rng.getRandomElement(FakeLanguageGen.registered), 0L);
        }
    }

    /**
     * Draws the map, applies any highlighting for the path to the cursor, and then draws the player.
     */
    public void putMap()
    {
        //In many other situations, you would clear the drawn characters to prevent things that had been drawn in the
        //past from affecting the current frame. This isn't a problem here, but would probably be an issue if we had
        //monsters running in and out of our vision. If artifacts from previous frames show up, uncomment the next line.
        //display.clear();

        for (int x = 0; x < bigWidth; x++) {
            for (int y = 0; y < bigHeight; y++) {
                if(visible[x][y] > 0.0) {
                    // Here we use a convenience method in SparseLayers that puts a char at a specified position (the
                    // first three parameters), with a foreground color for that char (fourth parameter), as well as
                    // placing a background tile made of a one base color (fifth parameter) that is adjusted to bring it
                    // closer to FLOAT_LIGHTING (sixth parameter) based on how visible the cell is (seventh parameter,
                    // comes from the FOV calculations) in a way that fairly-quickly changes over time.
                    // This effect appears to shrink and grow in a circular area around the player, with the lightest
                    // cells around the player and dimmer ones near the edge of vision. This lighting is "consistent"
                    // because all cells at the same distance will have the same amount of lighting applied.
                    // We use prunedDungeon here so segments of walls that the player isn't aware of won't be shown.
                    display.putWithConsistentLight(x, y, prunedDungeon[x][y], colors[x][y], bgColors[x][y], FLOAT_LIGHTING, visible[x][y]);
                } else if(seen.contains(x, y))
                    display.put(x, y, prunedDungeon[x][y], colors[x][y], SColor.lerpFloatColors(bgColors[x][y], GRAY_FLOAT, 0.45f));
            }
        }

        Coord pt;
        for (int i = 0; i < toCursor.size(); i++) {
            pt = toCursor.get(i);
            // Uses a brighter light to trace the path to the cursor, mixing the background color with white.
            // putWithLight() can take mix amounts greater than 1 or less than 0 to mix with extra bias.
            display.putWithLight(pt.x, pt.y, bgColors[pt.x][pt.y], SColor.FLOAT_WHITE, 1.25f);
        }
        languageDisplay.clear(0);
        languageDisplay.fillBackground(languageDisplay.defaultPackedBackground);
        for (int i = 0; i < 6; i++) {
            languageDisplay.put(1, i, lang.get(i), SColor.DB_LEAD);
        }
    }
    @Override
    public void render () {
        // standard clear the background routine for libGDX
        Gdx.gl.glClearColor(bgColor.r / 255.0f, bgColor.g / 255.0f, bgColor.b / 255.0f, 1.0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // center the camera on the player's position
        stage.getCamera().position.x = pg.getX();
        stage.getCamera().position.y =  pg.getY();

        // need to display the map every frame, since we clear the screen to avoid artifacts.
        putMap();
        // if the user clicked, we have a list of moves to perform.
        if(!awaitedMoves.isEmpty())
        {
            // this doesn't check for input, but instead processes and removes Coords from awaitedMoves.
            if (!display.hasActiveAnimations()) {
                Coord m = awaitedMoves.remove(0);
                if(!toCursor.isEmpty())
                    toCursor.remove(0);
                move(m.x - player.x, m.y - player.y);
                // this only happens if we just removed the last Coord from awaitedMoves, and it's only then that we need to
                // re-calculate the distances from all cells to the player. We don't need to calculate this information on
                // each part of a many-cell move (just the end), nor do we need to calculate it whenever the mouse moves.
                if (awaitedMoves.isEmpty()) {
                    // the next two lines remove any lingering data needed for earlier paths
                    playerToCursor.clearGoals();
                    playerToCursor.resetMap();
                    // the next line marks the player as a "goal" cell, which seems counter-intuitive, but it works because all
                    // cells will try to find the distance between themselves and the nearest goal, and once this is found, the
                    // distances don't change as long as the goals don't change. Since the mouse will move and new paths will be
                    // found, but the player doesn't move until a cell is clicked, the "goal" is the non-changing cell, so the
                    // player's position, and the "target" of a pathfinding method like DijkstraMap.findPathPreScanned() is the
                    // currently-moused-over cell, which we only need to set where the mouse is being handled.
                    playerToCursor.setGoal(player);
                    // DijkstraMap.partialScan only finds the distance to get to a cell if that distance is less than some limit,
                    // which is 13 here. It also won't try to find distances through an impassable cell, which here is the blockage
                    // GreasedRegion that contains the cells just past the edge of the player's FOV area.
                    playerToCursor.partialScan(13, blockage);
                }
            }
        }
        // if we are waiting for the player's input and get input, process it.
        else if(input.hasNext()) {
            input.next();
        }
        // we need to do some work with viewports here so the language display (or game info messages in a real game)
        // will display in the same place even though the map view will move around. We have the language stuff set up
        // its viewport so it is in place and won't be altered by the map. Then we just tell the Stage for the language
        // texts to draw.
        languageStage.getViewport().apply(false);
        languageStage.draw();
        // certain classes that use scene2d.ui widgets need to be told to act() to process input.
        stage.act();
        // we have the main stage set itself up after the language stage has already drawn.
        stage.getViewport().apply(false);
        // stage has its own batch and must be explicitly told to draw().
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);

        // message box won't respond to clicks on the far right if the stage hasn't been updated with a larger size
        float currentZoomX = (float)width / gridWidth;
        // total new screen height in pixels divided by total number of rows on the screen
        float currentZoomY = (float)height / (gridHeight + bonusHeight);
        // message box should be given updated bounds since I don't think it will do this automatically
        languageDisplay.setBounds(0, 0, width, currentZoomY * bonusHeight);
        // SquidMouse turns screen positions to cell positions, and needs to be told that cell sizes have changed
        // a quirk of how the camera works requires the mouse to be offset by half a cell if the width or height is odd
        // (gridWidth & 1) is 1 if gridWidth is odd or 0 if it is even; it's good to know and faster than using % , plus
        // in some other cases it has useful traits (x % 2 can be 0, 1, or -1 depending on whether x is negative, while
        // x & 1 will always be 0 or 1).
        input.getMouse().reinitialize(currentZoomX, currentZoomY, gridWidth, gridHeight,
                (gridWidth & 1) * (int)(currentZoomX * -0.5f), (gridHeight & 1) * (int) (currentZoomY * -0.5f));        // the viewports are updated separately so each doesn't interfere with the other's drawn area.
        languageStage.getViewport().update(width, height, false);
        // we also set the bounds of that drawn area here for each viewport.
        languageStage.getViewport().setScreenBounds(0, 0, width, (int)languageDisplay.getHeight());
        // we did this for the language viewport, now again for the main viewport
        stage.getViewport().update(width, height, false);
        stage.getViewport().setScreenBounds(0, (int)languageDisplay.getHeight(),
                width, height - (int)languageDisplay.getHeight());
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
            helpPane.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    stage.getActors().removeValue(helpPane, true);
                }
            });
        }
        stage.addActor(helpPane);
    }

    private void goFish() {
        nowFishing = true;
        selectedFish = null;
        updateFishInventoryPanel();
        fishes.clear();
        fishingLayers.clear(1);
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
        stage.addActor(fishingLayers);
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

        stage.getActors().removeValue(overlayPanel, true);

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
        stage.addActor(overlayPanel);
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
            
            final long readTime = System.currentTimeMillis() + 200;
            
            winPane.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    if (System.currentTimeMillis() > readTime) {
                        exiting();
                    }

                }
            });
        }
        stage.addActor(winPane);
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
            
            final long readTime = System.currentTimeMillis() + 200;
            diePane.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    super.clicked(event, x, y);
                    if (System.currentTimeMillis() > readTime) {                         
                        exiting();
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

}
