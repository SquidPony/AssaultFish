package assaultfish.old.ux;


import assaultfish.mapping.Map;
import assaultfish.old.mapping.MapFactory;
import assaultfish.old.physical.ActionType;
import static assaultfish.old.physical.ActionType.CONVERSE;
import static assaultfish.old.physical.ActionType.IDLE;
import static assaultfish.old.physical.ActionType.MOVE;
import static assaultfish.old.physical.ActionType.SHOOT;
import assaultfish.old.physical.CodexEntry;
import assaultfish.old.physical.Creature;
import assaultfish.old.physical.CreatureFactory;
import assaultfish.old.physical.Furniture;
import assaultfish.physical.Item;
import assaultfish.old.physical.Weapon;
import squidpony.squidsound.SoundManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.SplashScreen;
import java.awt.Toolkit;
import static java.awt.event.KeyEvent.*;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Queue;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.event.MouseInputListener;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidgrid.gui.awt.TextCellFactory;
import squidpony.squidgrid.gui.awt.event.SGKeyListener;
import squidpony.squidgrid.gui.swing.SwingPane;
import squidpony.squidgrid.util.BasicRadiusStrategy;
import squidpony.squidgrid.util.Direction;
import static squidpony.squidgrid.util.Direction.*;
import squidpony.squidgrid.util.RadiusStrategy;
import squidpony.squidmath.Bresenham;
import squidpony.squidmath.RNG;

/**
 * The primary GUI for the game.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class GUI {

    private static final double VERSION = 1.5;
    private JFrame frame;
    private JLayeredPane layers;
    private SwingPane view, output, stats;
    private double widthAdjust = 0.9, heightAdjust = 0.9;//how much to adjust compared to the available screen size
    private int width = 60, height = 30, statWidth = 20, defaultStatWidth = 20, statHeight, outputHeight = 2;
    private Map map;
    private SGKeyListener keys = new SGKeyListener(true, SGKeyListener.CaptureType.DOWN);
    private MouseInputListener mouse;
    private SoundManager sound;
    private MapFactory mapFactory;
    private Font viewFont, outputFont, statFont, eggFont, arimoFont, adoreFont;
    private int fontSize = 34;
    private Creature player = new Creature();
    private static ObjectMapper mapper = new ObjectMapper();
    private int turn = 1;
    private RNG rng = new RNG();
    private Creature lastShot;
    private HashMap<String, CodexEntry> codex = new HashMap<>();
    private RadiusStrategy hearingStrategy = BasicRadiusStrategy.SQUARE;
    private static final SColor ARMOR = SColor.CORN, SHIELD = SColor.CORNFLOWER_BLUE, HEALTH = SColor.CORAL_RED, BARRIER = SColor.HELIOTROPE;
    private int playerXP = 150, playerMoney = 0, wave = 0;
    private HashMap<Character, Character> cadReplace, arimoReplace, replacements;

    public GUI() {
        sound = new SoundManager();
        sound.playMusic(Sounds.TITLE_MUSIC);

        //set up replacement characters that are font dependant
        cadReplace = new HashMap<>();
        cadReplace.put('#', '©');
        arimoReplace = new HashMap<>();
        arimoReplace.put('u', '╥');
        arimoReplace.put('U', '╦');
        arimoReplace.put('©', 'ᴓ');

        //load fonts
        eggFont = loadFont(new File("./assets/CadmiumEgg.ttf"));//TODO -- Add thanks to Brandon Schoech at Tepid Monkey for the Cadmium Egg font
        if (eggFont == null) {
            viewFont = new Font("Arial", Font.PLAIN, fontSize);
            replacements = arimoReplace;
        } else {
            viewFont = eggFont;
            replacements = cadReplace;
        }
        adoreFont = loadFont(new File("./assets/Adore64.ttf"));
        if (adoreFont == null) {
            outputFont = new Font("Arial", Font.PLAIN, fontSize);
        } else {
            outputFont = adoreFont;
        }
        arimoFont = loadFont(new File("./assets/Arimo.ttf"));
        if (arimoFont == null) {
            statFont = new Font("Arial", Font.PLAIN, fontSize);
        } else {
            statFont = adoreFont;
        }

        //set up the map
        mapFactory = new MapFactory();
        map = mapFactory.defaultMap(MapFactory.FIREBASE_WHITE);
        player.location = map.playerStart;
        map.map[player.location.x][player.location.y].creature = player;
        width = map.width;
        height = map.height;

        frame = new JFrame("Attack The Geth   v" + VERSION);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(SColor.BLACK);
        try {
            frame.setIconImage(ImageIO.read(new File("./assets/icon.png")));
        } catch (IOException ex) {
        }
        frame.addKeyListener(keys);

        layers = new JLayeredPane();
        view = new SwingPane();
        output = new SwingPane();
        stats = new SwingPane();

        initDisplay();
        initPlayer();

        SplashScreen splash = SplashScreen.getSplashScreen();
        if (splash != null) {
            splash.close();
        }

        mainMenu(false);
    }

    private void initDisplay() {
//        frame.setVisible(false);//hide while resizing
        layers.removeAll();
//        frame.remove(layers);

        TextCellFactory textFactory = view.getTextCellFactory();
        textFactory.setPadding(0, 0, 3, 0);//add a bit of vertical padding
        textFactory.setAntialias(true);//make sure characters are smooth

        view.initialize(width + defaultStatWidth, height + outputHeight, new Font(viewFont.getFamily(), Font.BOLD, fontSize));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        view.setMaxDisplaySize((int) (screenSize.width * widthAdjust), (int) (screenSize.height * heightAdjust));
        layers.add(view);
        layers.setLayer(view, JLayeredPane.DEFAULT_LAYER);

        output.getTextCellFactory().setPadding(0, 0, 3, 0);
        output.getTextCellFactory().setAntialias(false);
        output.initialize(view.getCellWidth(), view.getCellHeight(), view.getGridWidth(), outputHeight, new Font(outputFont.getFamily(), Font.BOLD, fontSize));
        output.setLocation(0, height * view.getCellHeight());
        layers.add(output);
        layers.setLayer(output, JLayeredPane.MODAL_LAYER);

        statWidth = (int) (defaultStatWidth * 3.0 / 2.0);
        statHeight = (int) (height * 3.0 / 2.0);
        stats.getTextCellFactory().setPadding(0, 0, 1, 0);
        stats.getTextCellFactory().setAntialias(true);
        stats.initialize((int) (view.getCellWidth() * 2.0 / 3.0), (int) (view.getCellHeight() * 2.0 / 3.0), statWidth, statHeight, new Font(statFont.getFamily(), Font.BOLD, fontSize));
        stats.setLocation(width * view.getCellWidth(), 0);
        layers.add(stats);
        layers.setLayer(stats, JLayeredPane.MODAL_LAYER);

        layers.setPreferredSize(view.getPreferredSize());

        frame.getContentPane().add(layers);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.requestFocus();
    }

    /**
     * Main game loop
     */
    private void runGame() {
        sound.fadeOut();
        while (sound.isFading()) {
        }//spin lock until done fading out
        try {
            Thread.sleep(20);
        } catch (InterruptedException ex) {
        }
//        sound.maxMusicVolume = 0.5f;
        sound.setMusicVolume(sound.maxMusicVolume);
        sound.playMusic(Sounds.GAME_MUSIC);

        output("Boot up complete, ready for direct control.");
        output("Press F1 for options and help.", true);

        player.location = map.playerStart;
        map.map[player.location.x][player.location.y].creature = player;

        sound.playSoundFX(player.male ? Sounds.STARTUP_MALE : Sounds.STARTUP_FEMALE);

        boolean run = true;
        boolean lastSeen = false;
        while (run) {
            runFOV();
            updateDisplay();

            //check for wave completion
            if (map.creatures.isEmpty()) {
                output("All enemies eliminated.");
                switch (menu("Wave Complete", new String[]{"Next Wave", "Choose New Robot"})) {
                    case "Next Wave":
                        doSpawn();
                        runFOV();
                        updateDisplay();
                        break;
                    case "Choose New Robot":
                        Point loc = player.location;
                        chooseRobot();
                        player.location = loc;
                        map.map[loc.x][loc.y].creature = player;
                        doSpawn();
                        runFOV();
                        updateDisplay();
                        break;
                }
            } else if (map.creatures.size() == 1 && !lastSeen) {
                output("One enemy still detected.");
                output("  Detection booster utilized.", true);
                Creature c = map.creatures.get(0);
                if (!c.heard) {
                    c.heard = true;
                    sound.playSoundFX(c.idleSound);
                    float volume = c.getVolume() * player.hearing;//volume as far as the player is concerned
                    ping(c.location.x, c.location.y, volume, 50);
                }
                lastSeen = true;
            }

            int key = keys.next().getKeyCode();
            output("");
            Direction dir = getDirection(key);
            if (dir != null) {
                moveCreature(player, dir);
            } else {
                switch (key) {
                    case VK_F1:
                    case VK_SLASH:
                    case VK_ESCAPE:
                        mainMenu(true);
                        break;
                    case VK_F:
                        playerRangedAttack();
                        break;
                    case VK_R:
                        reload();
                        break;
                    case VK_A:
                    case VK_S:
                        listen();
                        break;
                    case VK_E:
                        examine();
                        break;
                    default:
                        output("Error: input not recognized.");
                        output("  Press F1 for options and help.", true);
                }
            }
        }
    }

    private void doSpawn() {
        CreatureFactory fact = mapFactory.creatureFactory;
        ArrayList<Creature> creatures = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            if (i + wave < 9) {
                creatures.add(fact.getCreature("geth trooper"));
            } else if (i + wave < 14) {
                creatures.add(fact.getCreature("geth hunter"));
            } else {
                creatures.add(fact.getCreature("geth prime"));
            }
        }
        wave++;
        mapFactory.spawn(map, creatures.toArray(new Creature[]{}), player.location);
    }

    /**
     * Runs a single game turn for everything except the player.
     */
    private void runTurn() {
        turn++;
        runFOV();
        updateDisplay();
        Collections.shuffle(map.creatures);//give them a random action order
        for (Creature c : map.creatures) {
            if (!c.aware) {//only check if not yet aware of player
                float volume = player.getVolume() * c.hearing;//volume as far as the player is concerned
                if (volume >= hearingStrategy.radius(c.location.x, c.location.y, player.location.x, player.location.y)) {
                    c.aware = true;//heard the player
                }
            }
            ActionType at = c.getAction(map, player);
            switch (at) {
                case IDLE:
                    //do nothing
                    break;
                case CONVERSE:
                    if (!c.heard) {
                        float volume = c.conversation * player.hearing;
                        if (hearingStrategy.radius(c.location.x, c.location.y, player.location.x, player.location.y) <= volume) {
                            c.heard = true;
                            ping(c.location.x, c.location.y, volume, 15);
                        }
                    }
                    break;
                case MOVE:
                    Point p = map.getClosestWaypoint(c.location, player.location);

                    //dumb movement AI
                    int dx = p.x - c.location.x;
                    int dy = p.y - c.location.y;
                    dx = (int) Math.signum(dx);
                    dy = (int) Math.signum(dy);
                    Direction dir = Direction.getDirection(dx, dy);
                    moveCreature(c, dir);//won't do anything if it can't move there
                    break;
                case SHOOT:
                    rangedAttack(c, player);
                    break;
            }
        }
    }

    private void reload() {
        output("Weapon reload function activated.");
        if (player.weapon == null) {
            output("  Error: weapon not found.", true);
        } else if (player.weapon.load == player.weapon.capacity) {
            output("  Notice: maximum thermal clip load already in place.", true);
        } else if (player.thermalClips <= 0) {
            output("  Error: thermal clips not found.", true);
        } else {
            sound.playSoundFX(Sounds.RELOAD);
            output("  " + player.weapon.name + " has been reloaded. " + player.weapon.load + " shots wasted.", true);
            player.weapon.load = player.weapon.capacity;
            player.thermalClips--;
            runTurn();
        }
    }

    private void listen() {
        output("Activating sonic detection device.");
        sound.playSoundFX(player.male ? Sounds.SCANNING_MALE : Sounds.SCANNING_FEMALE);
        for (Creature c : map.creatures) {
            float volume = c.getVolume() * player.hearing;//volume as far as the player is concerned
            if (volume >= hearingStrategy.radius(c.location.x, c.location.y, player.location.x, player.location.y)) {
                c.heard = true;
                sound.playSoundFX(c.idleSound);
                ping(c.location.x, c.location.y, volume, 50);
            } else {
                c.heard = false;
            }
        }
        updateDisplay();
    }

    private void newGame() {
        initPlayer();

        map = mapFactory.defaultMap(MapFactory.FIREBASE_WHITE);

        player.location = map.playerStart;
        map.map[player.location.x][player.location.y].creature = player;
        width = map.map.length;
        height = map.map[0].length;
        initDisplay();
        wave = 0;
        turn = 0;

        doSpawn();
        chooseRobot();

        runGame();
    }

    private void chooseRobot() {
        if (player.name == "") {//make sure there's some base information
            initPlayer();
        }
        switch (menu("Robot Builder", new String[]{"BOB 1872-1 (Soldier)", "ES-150 (Shielded)", "SP Mark III (Barrier)", "S.T.E.A.L.T.H. 4 (Sniper)", "Samurai 7 (Blind Melee)", "Custom"})) {
            case "SP Mark III (Barrier)":
                player.name = "SP Mark III";
                player.description = "A biotically enhanced mechanical unit.";
                player.color = SColor.WISTERIA;
                player.health = 100;
                player.barrier = 300;
                player.weapon = Weapon.CARNIFEX.clone();
                break;
            case "ES-150 (Shielded)":
                player.name = "ES-150";
                player.description = "A shielded robot.";
                player.color = SColor.CORNFLOWER_BLUE;
                player.health = 200;
                player.shield = 300;
                player.weapon = Weapon.PREDATOR.clone();
                player.male = false;
                break;
            case "BOB 1872-1 (Soldier)":
                player.name = "BOB 1872-1";
                player.description = "Heavy mechanical unit built for direct conflict.";
                player.color = SColor.FLAX;
                player.shield = 50;
                player.armor = 150;
                player.health = 100;
                player.weapon = Weapon.REVENANT.clone();
                break;
            case "S.T.E.A.L.T.H. 4 (Sniper)":
                player.name = "S.T.E.A.L.T.H. 4";
                player.description = "Relatively fragile robot equipped with advanced sniping capabilities.";
                player.color = SColor.DEEP_CHESTNUT;
                player.health = 100;
                player.shield = 25;
                player.vision = 30;
                player.hearing = 5;
                player.movement = 0.2f;
                player.weapon = Weapon.WIDOW.clone();
                player.thermalClips = 15;
                player.male = false;
                break;
            case "Samurai 7 (Blind Melee)":
                player.name = "Samurai 7";
                player.description = "Powerful punches and hearing compensate for very poor visual sensors.";
                player.color = SColor.SCARLET;
                player.vision = 1;
                player.meleeAccuracy = 135;
                player.meleeDamage = 125;
                player.hearing = 25;
                player.health = 400;
                player.shield = 250;
                player.barrier = 120;
                player.armor = 200;
                player.weapon = Weapon.PREDATOR.clone();
                player.thermalClips = 2;
                break;
            case "Custom":
            default:
                buildCustomRobot();
                break;
        }

        addToCodex(player);
        addToCodex(player.weapon);
    }

    private void mainMenu(boolean inGame) {
        boolean done = false;
        while (!done) {//stay in menu until choice takes it completely out
            String[] choices;
            if (inGame) {
                choices = new String[]{"New Game", "Help", "Codex", "Options", "Credits", "Return To Game", "Exit Game"};
            } else {
                choices = new String[]{"New Game", "Help", "Codex", "Options", "Credits", "Exit Game"};
            }
            switch (menu("Main Menu", choices)) {
                case "New Game":
                    newGame();
                    break;
                case "Help":
                    showHelp();
                    break;
                case "Codex":
                    lookAtCodex();
                    break;
                case "Options":
                    optionsMenu();
                    break;
                case "Credits":
                    showCredits();
                    break;
                case "Return To Game":
                    return;
                case "Exit Game":
                    done = true;
                    break;
                case "":
                    if (!inGame) {//cancelling out of the menu only exits if not in game
                        done = true;
                    } else {
                        return;
                    }
            }
        }
        exit();

    }

    private void optionsMenu() {
        while (true) {
            switch (menu("Options", new String[]{"Music Volume", "Sound FX Volume", "Change Font", "Back"})) {
                case "Music Volume":
                    switch (menu("Music Volume", new String[]{"Maximum", "High", "Normal", "Low", "Very Low", "Off"})) {
                        case "Maximum":
                            sound.maxMusicVolume = 1f;
                            break;
                        case "High":
                            sound.maxMusicVolume = 0.8f;
                            break;
                        case "Normal":
                            sound.maxMusicVolume = 0.7f;
                            break;
                        case "Low":
                            sound.maxMusicVolume = 0.4f;
                            break;
                        case "Very Low":
                            sound.maxMusicVolume = 0.1f;
                            break;
                        case "Off":
                            sound.maxMusicVolume = 0;
                            break;
                    }
                    sound.setMusicVolume(sound.maxMusicVolume);
                    break;
                case "Sound FX Volume":
                    switch (menu("Sound FX Volume", new String[]{"Maximum", "High", "Normal", "Low", "Very Low", "Off"})) {
                        case "Maximum":
                            sound.soundfxVolume = 1f;
                            break;
                        case "High":
                            sound.soundfxVolume = 0.8f;
                            break;
                        case "Normal":
                            sound.soundfxVolume = 0.7f;
                            break;
                        case "Low":
                            sound.soundfxVolume = 0.4f;
                            break;
                        case "Very Low":
                            sound.soundfxVolume = 0.1f;
                            break;
                        case "Off":
                            sound.soundfxVolume = 0;
                            break;
                    }
                    sound.setMusicVolume(sound.maxMusicVolume);
                    break;
                case "Change Font":
                    switch (menu("Main Display Font", new String[]{"Cadmium Egg (Default)", "Arimo"})) {
                        case "Arimo":
                            viewFont = arimoFont;
                            replacements = arimoReplace;
                            break;
                        default:
                            viewFont = eggFont;
                            replacements = cadReplace;
                    }
                    if (viewFont == null) {
                        viewFont = new Font("Arial", Font.PLAIN, fontSize);
                        replacements = arimoReplace;
                    }
                    initDisplay();
                    break;
                case "":
                case "Back":
                    return;
            }
        }
    }

    private void showCredits() {
        SwingPane guide = new SwingPane();

        guide.getTextCellFactory().setPadding(0, 0, 3, 0);
        guide.getTextCellFactory().setAntialias(false);
        guide.initialize(view.getCellWidth(), view.getCellHeight(), view.getGridWidth(), view.getGridHeight(), view.getFont());
        guide.setLocation(0, 0);

        layers.add(guide);
        layers.setLayer(guide, JLayeredPane.POPUP_LAYER);

        String words = "CREDIT WHERE CREDIT IS DUE";
        guide.placeVerticalString(1, (view.getGridHeight() - words.length()) / 2, words, SColor.ALICE_BLUE, SColor.BLACK);
        guide.placeVerticalString(view.getGridWidth() - 2, (view.getGridHeight() - words.length()) / 2, words, SColor.ALICE_BLUE, SColor.BLACK);

        int y = 1;
        words = "Eben Howard";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ALICE_BLUE, SColor.BLACK_CHESTNUT_OAK);
        y += 2;
        words = "Game & UX Design";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 2;
        words = "Programming";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 2;
        words = "Sound Engineering";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 2;
        words = "Male Robot Voice";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 5;

        words = "Christy Montgomery";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ALICE_BLUE, SColor.BLACK_CHESTNUT_OAK);
        y += 2;
        words = "Female Robot Voice";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 5;

        words = "Torley Wong";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ALICE_BLUE, SColor.BLACK_CHESTNUT_OAK);
        y += 2;
        words = "Music via Creative Commons License";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 5;

        words = "Libraries & Tools Used";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ALICE_BLUE, SColor.BLACK_CHESTNUT_OAK);
        y += 2;
        words = "SquidLib - User Interface";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 2;
        words = "jlwgl - Sound";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 2;
        words = "Bfxr - Sound FX Creation";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y += 2;
        words = "NetBeans - IDE of The Gods";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ULTRAMARINE_DYE, SColor.BLACK);

        guide.refresh();
        keys.flush();
        keys.next();//wait for any keyboard input
        layers.remove(guide);
    }

    private void showHelp() {
        SwingPane guide = new SwingPane();

        guide.getTextCellFactory().setPadding(0, 0, 3, 0);
        guide.getTextCellFactory().setAntialias(false);
        guide.initialize(view.getCellWidth(), view.getCellHeight(), view.getGridWidth(), view.getGridHeight(), view.getFont());
        guide.setLocation(0, 0);

        String words = "Story";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, 1, words, SColor.ALICE_BLUE, SColor.ARMY_GREEN);
        int y = 2;
        String[] story = new String[]{
            "You control a remotely operated robot, which you",
            "use to defeat enemy Geth, a sentient race of robots",
            "bent on the destruction of all organics.",
            "",
            "Pay no attention to the irony of using a non-sentient",
            "robot to kill sentient ones.",
            "",
            "You can use experience gained to create new robots,",
            "even between waves!"
        };

        for (int i = 0; i < story.length; i++) {
            words = story[i];
            words = " " + words;
            for (int j = 1; j < view.getGridWidth() - 1; j++) {
                guide.clearCell(j, i + y, SColor.ALOEWOOD);
            }
            guide.placeHorizontalString(1, i + y, words, SColor.BRIGHT_GOLDEN_YELLOW, SColor.ALOEWOOD);
        }
        y += story.length + 2;


        words = "Controls";
        guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, y, words, SColor.ALICE_BLUE, SColor.ARMY_GREEN);
        guide.refresh();
        y += 1;

        layers.add(guide);
        layers.setLayer(guide, JLayeredPane.POPUP_LAYER);

        String[] controlScheme = new String[]{
            "Depending on the robot's configureation, the following tasks",
            "can be performed through your keyboard interface.",
            "",
            "Move with NumPad, arrow, or VI (hjklyubn) keys",
            "  Moving into an enemy will initiate a melee attack.",
            "  Moving into low cover will hop the cover, or perform a",
            "  grab on an enemy directly opposite the cover, killing it.",
            "",
            ". or NumPad 5 - Skip a turn.",
            "",
            "f - Fire weapon.",
            "",
            "r - Reload weapon (if thermal clip available).",
            "    Any ammunition left in the weapon will be lost!",
            "",
            "a or s - Active scan for enemies with auditory sensors.",
            "",
            "e - Examine surroundings and add discoveries to codex.",
            "    Enemies examined show current stats and health",
            "    This is the only way to add entries to the codex.",
            "",
            "F1 or ? - Bring up Options Menu.",
            "ESC - Exit any menu."
        };

        for (int i = 0; i < controlScheme.length; i++) {
            words = controlScheme[i];
            words = " " + words;
            for (int j = 1; j < view.getGridWidth() - 1; j++) {
                guide.clearCell(j, i + y, SColor.ALOEWOOD);
            }
            guide.placeHorizontalString(1, i + y, words, SColor.BRIGHT_GOLDEN_YELLOW, SColor.ALOEWOOD);
        }

        guide.refresh();

        keys.flush();
        keys.next();//wait for any keyboard input
        layers.remove(guide);
    }

    private void runFOV() {
        float[][] resistMap = new float[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                resistMap[x][y] = map.map[x][y].furniture.sightBlocking ? 1f : 0f;
            }
        }
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (map.isVisible(player.location.x, player.location.y, x, y) && player.vision >= hearingStrategy.radius(player.location.x, player.location.y, x, y)) {
                    map.map[x][y].seen = true;
                    map.map[x][y].light = SColorFactory.blend(SColor.ALICE_BLUE, SColorFactory.lightest(SColor.WHITE), hearingStrategy.radius(player.location.x, player.location.y, x, y) / player.vision);
                } else {
                    if (!map.map[x][y].light.equals(SColor.BLACK)) {
                        map.map[x][y].light = SColor.BLACK;
                    }
                }
            }
        }
    }

    private void moveCreature(Creature creature, Direction dir) {
        if (dir == Direction.NONE) {
            if (creature == player) {
                output("Stratigically delaying action.");
                runTurn();
            }
            return;//don't do anything
        }

        int targetx = creature.location.x + dir.deltaX;
        int targety = creature.location.y + dir.deltaY;
        float volume = creature.getVolume() * player.hearing;
        if (targetx < 0 || targety < 0 || targetx >= width || targety >= width) {
            return;//no movement
        }
        if (map.map[targetx][targety].creature != null) {
            if (creature == player) {
                meleeAttack(player, map.map[targetx][targety].creature);
                runTurn();
            } else if (map.map[targetx][targety].creature == player) {//only attack the player
                meleeAttack(creature, player);
            }
        } else if (!map.map[targetx][targety].furniture.movementBlocking) {
            map.map[creature.location.x][creature.location.y].creature = null;
            creature.location.x = targetx;
            creature.location.y = targety;
            map.map[creature.location.x][creature.location.y].creature = creature;
            updateDisplay();
            if (creature == player) {
                sound.playSoundFX((rng.nextBoolean() ? Sounds.WALK1 : Sounds.WALK2));
            } else {
                //check to see if player can hear the movement
                if (hearingStrategy.radius(creature.location.x, creature.location.y, player.location.x, player.location.y) <= volume) {
                    if (!creature.heard && !creature.smelled && !map.map[targetx][targety].seen) {//only ping if a new target
                        sound.playSoundFX(creature.idleSound);
                        ping(targetx, targety, volume, 20);
                    }
                    creature.heard = true;
                } else {
                    creature.heard = false;
                }
            }
            if (creature == player) {
                runTurn();
            }
        } else {
            targetx += dir.deltaX;
            targety += dir.deltaY;
            if (targetx >= 0 && targety >= 0 && targetx < width && targety < height && !map.map[targetx][targety].furniture.movementBlocking && map.map[targetx - dir.deltaX][targety - dir.deltaY].furniture.hoppable) {
                if (creature == player && map.map[targetx][targety].creature != null) {//only the player can perform a grab
                    if (map.map[targetx][targety].creature.grabbable) {
                        grab(map.map[targetx][targety].creature);
                    } else {
                        output("Enemy too large to grab.");
                        return;
                    }
                } else if (map.map[targetx][targety].creature != null) {
                    return;//enemies stand idle when blocked
                } else {
                    map.map[creature.location.x][creature.location.y].creature = null;
                    map.map[targetx][targety].creature = creature;
                    creature.location.x = targetx;
                    creature.location.y = targety;
                    updateDisplay();
                    if (creature == player) {
                        output("Activing mini-boosters.");
                        sound.playSoundFX(Sounds.JUMP);
//                        ping(targetx, targety, volume, 20);
                        output("  Hurdle hurdling heuristics successful.", true);
                        runTurn();
                    } else {
                        //check to see if player can hear the movement
                        if (hearingStrategy.radius(creature.location.x, creature.location.y, player.location.x, player.location.y) <= volume) {
                            if (!creature.heard && !creature.smelled && !map.map[targetx][targety].seen) {//only ping if a new target
                                sound.playSoundFX(creature.idleSound);
                                ping(targetx, targety, volume, 20);
                            }
                            creature.heard = true;
                        } else {
                            creature.heard = false;
                        }
                    }
                }
            } else if (creature == player) {
                output("Crunch! Error: solid object detected.");
                sound.playSoundFX(player.male ? Sounds.ERROR_MALE : Sounds.ERROR_FEMALE);
            } else {
                moveCreature(creature, Direction.values()[rng.nextInt(Direction.values().length)]);//creature tries another random move //TODO -- replace this with waypoint pathing
            }
        }
    }

    private void meleeAttack(Creature attacker, Creature enemy) {
        if (attacker == player) {
            output("Activating physical force interaction subroutine.");
        } else {
            output("Defending against physical force application.");
        }

        float hitChance = attacker.vision * enemy.getVisibility();
        hitChance += attacker.smell * enemy.odor;
        hitChance += attacker.hearing * enemy.getVolume();

        if (rng.nextInt(100) < hitChance) {//a hit
            sound.playSoundFX(Sounds.MELEE);
            applyDamage(attacker.meleeDamage, enemy);
            if (attacker == player) {
                if (enemy.health <= 0) {
                    map.creatures.remove(enemy);
                    map.map[enemy.location.x][enemy.location.y].creature = null;
                    output("  Target enemy's existance discontinued.", true);
                    playerXP += enemy.xp;
                } else {
                    output("  Enemy sustained " + attacker.meleeDamage + " units of damage.", true);
                }
            } else {
                output("Sustained " + attacker.meleeDamage + " damage.", true);
            }
        } else {//a miss
            sound.playSoundFX(Sounds.WARNING2);
            flash(attacker.location.x, attacker.location.y, '!', SColor.CINNAMON, 150);
            if (attacker == player) {
                output("  Error in distance calculation: target missed.", true);
            }
        }
    }

    private void grab(Creature enemy) {
        output("Grab mode initiated.");
        map.map[enemy.location.x][enemy.location.y].creature = null;
        map.creatures.remove(enemy);
        sound.playSoundFX(Sounds.GRAB);
        flash(enemy.location.x, enemy.location.y, 'X', SColor.BRIGHT_PINK, 180);
        sound.playSoundFX(enemy.deathSound);
        output("  Enemy " + enemy.name + " terminated.", true);
        playerXP += enemy.xp * 3;//triple points for grabs!
        runTurn();
    }

    private void playerRangedAttack() {
        if (player.weapon.load <= 0) {
            output("Weapon status: overheating.");
            sound.playSoundFX(player.male ? Sounds.ERROR_MALE : Sounds.ERROR_FEMALE);
            output("  Can not fire unless thermal clip replaced.", true);
            flash(player.location.x, player.location.y, '!', SColor.CINNAMON, 150);
            return;
        }

        output("Interface procedure for " + player.weapon.name + " running.");
        output("  Target selection mode activated.", true);
        Point aim = findClosestVisibleEnemy();
        aim = getTarget(aim);
        if (aim == null) {
            return;//process canceled
        }

        Creature creature = map.map[aim.x][ aim.y].creature;
        lastShot = creature;

        if (creature != null && (map.map[aim.x][aim.y].seen || creature.heard || creature.smelled)) {
            if (creature == player) {
                output("Target invalid. Can not inflict self harm.");
                sound.playSoundFX(Sounds.WARNING2);
                flash(player.location.x, player.location.y, '!', SColor.CINNAMON, 150);
            } else {
                output("Target identified: " + map.map[aim.x][ aim.y].creature.name);
                rangedAttack(player, map.map[aim.x][ aim.y].creature);
                runTurn();
            }
        } else {
            output("No target identified, aborting procedure.");
            sound.playSoundFX(Sounds.WARNING2);
            flash(player.location.x, player.location.y, '!', SColor.CINNAMON, 150);
        }
    }

    private void rangedAttack(Creature attacker, Creature enemy) {
        if (attacker.weapon.load <= 0) {
            if (attacker == player) {
                sound.playSoundFX(Sounds.WARNING2);
                flash(player.location.x, player.location.y, '!', SColor.CINNAMON, 150);
                output("  Weapon overheating, replace thermal clip.", true);
                return;
            } else {
                attacker.weapon.load = attacker.weapon.capacity;
                return;
            }
        }

        if (attacker == player) {
            for (Creature c : map.creatures) {//check to see if they notice the attacker shooting
                if (!c.aware) {//only check if not yet aware of attacker
                    float volume = attacker.weapon.volume * c.hearing;//volume as far as the attacker is concerned
                    if (volume >= hearingStrategy.radius(c.location.x, c.location.y, attacker.location.x, attacker.location.y)) {
                        c.aware = true;//heard the attacker
                    }
                } else if (c == enemy) {
                    c.aware = true;
                }
            }
        } else {//check if attacker can hear
            float volume = attacker.weapon.volume * enemy.hearing;//volume as far as the attacker is concerned
            if (volume >= hearingStrategy.radius(enemy.location.x, enemy.location.y, attacker.location.x, attacker.location.y)) {
                attacker.heard = true;
            }
        }

        if (!map.isVisible(attacker.location.x, attacker.location.y, enemy.location.x, enemy.location.y)) {
            if (attacker == player) {
                attacker.weapon.load--;
                sound.playSoundFX(attacker.weapon.soundfx);
                updateStats(attacker);
                ping(attacker.location.x, attacker.location.y, attacker.weapon.volume, 25);
                drawShot(attacker.location.x, attacker.location.y, enemy.location.x, enemy.location.y);
                output("  Obstruction deflected attack.", true);
            }
            return;
        }

        float hitChance = attacker.vision * enemy.size * (enemy.hiding ? enemy.hidingModifier : 1) * enemy.opacity;//basic vision
        hitChance += attacker.smell * enemy.odor;
        hitChance += attacker.hearing * (enemy.movement * (enemy.hurrying ? enemy.hurryingModifyer : 1) + (enemy.sneaking ? enemy.sneakingModifyer : 1) * enemy.conversation);
        hitChance += attacker.weapon.accuracy;

        int totalDamage = 0;
        boolean hit = false;
        ping(attacker.location.x, attacker.location.y, attacker.weapon.volume, 25);
        for (int i = 0; i < attacker.weapon.rate && attacker.weapon.load > 0; i++) {
            attacker.weapon.load--;
            sound.playSoundFX(attacker.weapon.soundfx);
            drawShot(attacker.location.x, attacker.location.y, enemy.location.x, enemy.location.y);
            if (attacker == player) {
                updateStats(attacker);
            }
            if (rng.nextInt(100) < hitChance) {//a hit
                applyDamage(attacker.weapon.damage, enemy);
                hit = true;
                totalDamage += attacker.weapon.damage;
                if (enemy.health <= 0) {
                    break;//jump out once it's dead
                }
            }
        }

        if (enemy.health <= 0) {//can only get hear if enemy not the player
            map.creatures.remove(enemy);
            map.map[enemy.location.x][enemy.location.y].creature = null;
            lastShot = null;
            output("  " + totalDamage + " damage. Target eliminated.", true);
            playerXP += enemy.xp;
        } else if (player == attacker) {
            if (hit) {
                output("  Enemy sustained " + totalDamage + " units of damage.", true);
            } else {//a miss
                sound.playSoundFX(Sounds.WARNING2);
                flash(attacker.location.x, attacker.location.y, '!', SColor.CINNAMON, 150);
                if (attacker.weapon.load <= 0) {
                    output("  Weapon overheating, replace thermal clip.", true);
                } else {
                    output("  Error in aiming routine. Target not affected.", true);
                }
            }
        }
    }

    /**
     * Damage rolls over, but the rollover damage is halved from whatever's
     * left.
     *
     * @param amount
     * @param target
     */
    private void applyDamage(int amount, Creature target) {
        int remainder = amount;
        if (target.shield > 0) {
            flash(target.location.x, target.location.y, '©', SHIELD, 180);
            target.shield -= remainder;
            remainder = -target.shield / 2;//spill over
            target.shield = Math.max(target.shield, 0);
            if (target == player) {
                updateStats(player);
            }
            if (remainder <= 0) {
                return;
            }
        }
        if (target.barrier > 0) {
            flash(target.location.x, target.location.y, '©', BARRIER, 180);
            target.barrier -= remainder;
            remainder = -target.barrier / 2;//spill over
            target.barrier = Math.max(target.barrier, 0);
            if (target == player) {
                updateStats(player);
            }
            if (remainder <= 0) {
                return;
            }
        }
        if (target.armor > 0) {
            flash(target.location.x, target.location.y, '©', ARMOR, 180);
            target.armor -= remainder;
            remainder = -target.armor / 2;//spill over
            target.armor = Math.max(target.armor, 0);
            if (target == player) {
                updateStats(player);
            }
            if (remainder <= 0) {
                return;
            }
        }
        if (target.health > 0) {
            flash(target.location.x, target.location.y, '©', HEALTH, 180);
            target.health -= remainder;
            if (target == player) {
                updateStats(player);
            }
        }

        if (target.health <= 0) {
            sound.playSoundFX(target.deathSound);

            if (target == player) {
                output("Critical system struck. Explosion imminent.");
                output("  Systems failing...    (Press any key to exit)", true);
                keys.flush();
                keys.next();

                menu("Robot Terminated", new String[]{"Return To Main Interface"});
                mainMenu(false);
            }
        }
    }

    private void examine() {
        output("Sensors activated. All systems nominal.");
        sound.playSoundFX(player.male ? Sounds.SCANNING_MALE : Sounds.SCANNING_FEMALE);
        output("  Use movement keys to examine surroundings.", true);

        Point look = new Point(player.location.x, player.location.y);

        keys.flush();
        int key = -1;
        while (key != VK_ENTER && key != VK_X && key != VK_ESCAPE) {
            view.highlight(look.x, look.y);
            view.refresh();
            key = keys.next().getKeyCode();
            Direction dir = getDirection(key);
            if (dir != null) {
                look.x += dir.deltaX;
                look.x = Math.max(look.x, 0);
                look.x = Math.min(look.x, width - 1);
                look.y += dir.deltaY;
                look.y = Math.max(look.y, 0);
                look.y = Math.min(look.y, height - 1);

                if (map.map[look.x][look.y].seen) {
                    Item item = map.map[look.x][look.y].furniture;
                    String words = "  Terrain: " + item.name;
                    addToCodex(item);
                    item = map.map[look.x][look.y].item;
                    if (item != null) {
                        words += "  Item: " + item.name;
                        addToCodex(item);
                    }
                    Creature creature = map.map[look.x][look.y].creature;
                    if (creature != null) {
                        words += "  Target: " + creature.name;
                        addToCodex(creature);
                        updateStats(creature);
                        if (creature.weapon != null) {
                            addToCodex(creature.weapon);
                        }
                    }
                    output("Scanning area. Sensors reporting.");
                    output(words, true);
                } else {
                    Creature creature = map.map[look.x][look.y].creature;
                    String words = "";
                    if (creature != null) {
                        if (creature.heard) {
                            words += "  Target: " + creature.name + " heard";
                            addToCodex(creature);
                        }
                        if (creature.smelled) {
                            if (words == "") {
                                words += "  Target: " + creature.name + " has been smelled";
                            } else {
                                words += " and smelled";
                            }
                            addToCodex(creature);
                        }
                    }

                    if (words == "") {
                        output("Unable to scan area. Falling back to memory.");
                        output("  Location memory not available.", true);
                    } else {
                        output("Memory access in progress.");
                        output("  " + words + ".", true);
                        if (creature != null) {
                            updateStats(creature);
                        }
                    }
                }
            }
        }

        view.removeHighlight();
    }

    private void addToCodex(Item item) {
        String codeName = item.symbol + item.name;
        if (codex.containsKey(codeName)) {
            return;//already seen and added
        }
        codex.put(codeName, new CodexEntry(item.symbol, item.color, item.name, item.description, item.getClass(), true));
    }

    private void lookAtCodex() {
        SwingPane guide = new SwingPane();

        guide.getTextCellFactory().setPadding(0, 0, 3, 0);
        guide.getTextCellFactory().setAntialias(false);
        guide.initialize(view.getCellWidth(), view.getCellHeight(), view.getGridWidth(), view.getGridHeight(), view.getFont());
        guide.setLocation(0, 0);

        int offset = 0;
        boolean done = false;

        while (!done) {
            for (int x = 0; x < guide.getGridWidth(); x++) {
                for (int y = 0; y < guide.getGridHeight(); y++) {
                    guide.clearCell(x, y);
                }
            }

            String words = " Codex ";
            guide.placeHorizontalString((view.getGridWidth() - words.length()) / 2, 1 + offset, words, SColor.ALICE_BLUE, SColor.ARMY_GREEN);
            guide.refresh();

            layers.add(guide);
            layers.setLayer(guide, JLayeredPane.POPUP_LAYER);

            //sort entries by type
            ArrayList<CodexEntry> furnitureEntries = new ArrayList<>(), creatureEntries = new ArrayList<>(), weaponEntries = new ArrayList<>(), otherEntries = new ArrayList<>();
            for (CodexEntry ce : codex.values()) {
                if (ce.type == Weapon.class) {
                    weaponEntries.add(ce);
                } else if (ce.type == Creature.class) {
                    creatureEntries.add(ce);
                } else if (ce.type == Furniture.class) {
                    furnitureEntries.add(ce);
                } else {
                    otherEntries.add(ce);
                }
            }

            int y = 3;
            if (!weaponEntries.isEmpty()) {
                guide.placeHorizontalString(1, y + offset, " Weapons ", SColor.BLACK_KITE, SColor.ORANGE_RED);
                y++;
                guide.refresh();
                for (int i = 0; i < weaponEntries.size(); i++) {
                    Item item = weaponEntries.get(i);
                    guide.placeCharacter(2, y + offset, item.symbol, item.color);
                    guide.placeHorizontalString(3, y + offset, " - " + item.name, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    ArrayList<String> dstrings = new ArrayList<>();
                    Scanner scan = new Scanner(item.description);
                    String temp = "", in;
                    while (scan.hasNext()) {
                        in = scan.next();
                        if (temp.length() + in.length() > guide.getGridWidth() - 3) {
                            dstrings.add(temp);
                            temp = "";
                        }
                        temp += in + " ";
                    }
                    dstrings.add(temp);
                    for (String s : dstrings) {
                        y++;
                        guide.placeHorizontalString(2, y + offset, s, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    }
                    y += 2;
                }
            }

            if (!creatureEntries.isEmpty()) {
                guide.placeHorizontalString(1, y + offset, " Creatures ", SColor.BLACK_KITE, SColor.ORANGE_RED);
                y++;
                guide.refresh();
                for (int i = 0; i < creatureEntries.size(); i++) {
                    Item item = creatureEntries.get(i);
                    guide.placeCharacter(2, y + offset, item.symbol, item.color);
                    guide.placeHorizontalString(3, y + offset, " - " + item.name, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    ArrayList<String> dstrings = new ArrayList<>();
                    Scanner scan = new Scanner(item.description);
                    String temp = "", in;
                    while (scan.hasNext()) {
                        in = scan.next();
                        if (temp.length() + in.length() > guide.getGridWidth() - 3) {
                            dstrings.add(temp);
                            temp = "";
                        }
                        temp += in + " ";
                    }
                    dstrings.add(temp);
                    for (String s : dstrings) {
                        y++;
                        guide.placeHorizontalString(2, y + offset, s, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    }
                    y += 2;
                }
            }

            if (!furnitureEntries.isEmpty()) {
                guide.placeHorizontalString(1, y + offset, " Terrain ", SColor.BLACK_KITE, SColor.ORANGE_RED);
                y++;
                guide.refresh();
                for (int i = 0; i < furnitureEntries.size(); i++) {
                    Item item = furnitureEntries.get(i);
                    guide.placeCharacter(2, y + offset, item.symbol, item.color);
                    guide.placeHorizontalString(3, y + offset, " - " + item.name, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    ArrayList<String> dstrings = new ArrayList<>();
                    Scanner scan = new Scanner(item.description);
                    String temp = "", in;
                    while (scan.hasNext()) {
                        in = scan.next();
                        if (temp.length() + in.length() > guide.getGridWidth() - 3) {
                            dstrings.add(temp);
                            temp = "";
                        }
                        temp += in + " ";
                    }
                    dstrings.add(temp);
                    for (String s : dstrings) {
                        y++;
                        guide.placeHorizontalString(2, y + offset, s, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    }
                    y += 2;
                }
            }

            if (!otherEntries.isEmpty()) {
                guide.placeHorizontalString(1, y + offset, " Items ", SColor.BLACK_KITE, SColor.ORANGE_RED);
                y++;
                guide.refresh();
                for (int i = 0; i < otherEntries.size(); i++) {
                    Item item = otherEntries.get(i);
                    guide.placeCharacter(2, y + offset, item.symbol, item.color);
                    guide.placeHorizontalString(3, y + offset, " - " + item.name, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    ArrayList<String> dstrings = new ArrayList<>();
                    Scanner scan = new Scanner(item.description);
                    String temp = "", in;
                    while (scan.hasNext()) {
                        in = scan.next();
                        if (temp.length() + in.length() > guide.getGridWidth() - 3) {
                            dstrings.add(temp);
                            temp = "";
                        }
                        temp += in + " ";
                    }
                    dstrings.add(temp);
                    for (String s : dstrings) {
                        y++;
                        guide.placeHorizontalString(2, y + offset, s, SColor.BRIGHT_GOLDEN_YELLOW, SColor.BLACK);
                    }
                    y += 2;
                }
            }

            guide.refresh();

            keys.flush();
            int key = keys.next().getKeyCode();//wait for any keyboard input

            Direction dir = getDirection(key);
            if (dir != null) {
                if (dir.deltaY == 1) {
                    if (y >= guide.getGridHeight()) {
                        offset--;
                    }
                    offset = Math.max(offset, -(y - guide.getGridHeight()));//don't let it scroll off the screen
                } else if (dir.deltaY == -1) {
                    offset++;
                    offset = Math.min(0, offset);
                }
            } else {
                done = true;
            }

            guide.refresh();
        }

        layers.remove(guide);
    }

    /**
     * Flashes the square at the provided location the given color.
     *
     * @param x
     * @param y
     * @param color
     */
    private void flash(int x, int y, char c, SColor color, int time) {
        Character rep = replacements.get(c);
        if (rep != null) {
            c = rep;
        }
        SwingPane flash = new SwingPane(view.getCellWidth(), view.getCellHeight(), 1, 1, new Font(viewFont.getFamily(), Font.BOLD, fontSize));
        flash.setTextCellFactory(view.getTextCellFactory());
        flash.placeCharacter(0, 0, c, color);
        flash.setLocation(x * view.getCellWidth(), y * view.getCellHeight());
        flash.refresh();
        layers.setLayer(flash, JLayeredPane.POPUP_LAYER);
        layers.add(flash);

        flash.setVisible(true);
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
        }

        layers.remove(flash);
    }

    /**
     * Finds the closest enemy to the player that is currently visible. If the
     * passed in starting point contains a creature that is visible, that point
     * is returned.
     *
     * @param start
     * @return
     */
    private Point findClosestVisibleEnemy() {
        Point p = new Point();
        if (lastShot == null || !map.creatures.contains(lastShot) || map.isVisible(player.location.x, player.location.y, lastShot.location.x, lastShot.location.y)) {//check if last shot creture still alive
            float distance = Float.MAX_VALUE;
            Creature closest = null;
            for (Creature c : map.creatures) {
                if (map.isVisible(player.location.x, player.location.y, c.location.x, c.location.y) && map.map[c.location.x][c.location.y].seen) {//possible targets
                    float r = BasicRadiusStrategy.CIRCLE.radius(player.location.x, player.location.y, c.location.x, c.location.y);
                    if (r < distance) {
                        distance = r;
                        closest = c;
                    }
                }
            }
            if (closest != null) {
                p.x = closest.location.x;
                p.y = closest.location.y;
                lastShot = closest;
            } else {
                p.x = player.location.x;
                p.y = player.location.y;
            }
        } else {
            p.x = lastShot.location.x;
            p.y = lastShot.location.y;
        }
        return p;
    }

    /**
     * Starts an interactive process where the player can pick a location. Any
     * key besides the direction keys will return the point at the highlight.
     *
     * If the process is canceled by the escape key, a null is returned.
     *
     * @param start
     * @return
     */
    private Point getTarget(Point start) {
        Point p = new Point(start);

        keys.flush();
        int key;
        Direction dir;
        do {
            view.highlight(start.x, start.y);
            view.refresh();
            key = keys.next().getKeyCode();
            if (key == VK_ESCAPE) {
                output("Targeting interface discontinued.");
                view.removeHighlight();
                view.refresh();
                return null;
            }
            dir = getDirection(key);
            if (dir != null) {
                start.x += dir.deltaX;
                start.x = Math.max(start.x, 0);
                start.x = Math.min(start.x, width - 1);
                start.y += dir.deltaY;
                start.y = Math.max(start.y, 0);
                start.y = Math.min(start.y, height - 1);
            }
            view.refresh();
        } while (dir != null);

        view.removeHighlight();
        return p;
    }

    private void drawShot(int startx, int starty, int endx, int endy) {
        SwingPane shot = new SwingPane(view.getCellWidth(), view.getCellHeight(), view.getGridWidth(), view.getGridHeight(), new Font(viewFont.getFamily(), Font.BOLD, fontSize));
        shot.setTextCellFactory(view.getTextCellFactory());
        shot.setLocation(0, 0);
        layers.setLayer(shot, JLayeredPane.getLayer(view) + 10);
        for (int x = 0; x < shot.getGridWidth(); x++) {
            for (int y = 0; y < shot.getGridHeight(); y++) {
                shot.placeImage(x, y, view.getCellImage(x, y));
            }
        }
        shot.refresh();
        layers.add(shot);

        try {
            Queue<Point> q = Bresenham.line2D(startx, starty, endx, endy);
            Point previous = null;
            while (!q.isEmpty()) {
                Point p = q.poll();
                shot.placeCharacter(p.x, p.y, '*', SColor.ALICE_BLUE);
                if (previous != null) {
                    shot.placeImage(previous.x, previous.y, view.getCellImage(previous.x, previous.y));
                }
                shot.refresh();
                Thread.sleep(15);
            }
        } catch (InterruptedException ex) {
        }

        layers.remove(shot);
        view.refresh();
    }

    /**
     * Creates an enlarging concentric ring animation centered at x, y
     *
     * @param startx
     * @param starty
     * @param volume the distance the sound spreads
     * @param pause how many milliseconds to pause when drawing
     */
    private void ping(int startx, int starty, float volume, int pause) {
        SwingPane ping = new SwingPane(view.getCellWidth(), view.getCellHeight(), (int) (volume * 2) + 1, (int) (volume * 2) + 1, new Font(viewFont.getFamily(), Font.BOLD, fontSize));
        ping.setTextCellFactory(view.getTextCellFactory());
        ping.setLocation((int) ((startx - volume) * view.getCellWidth()), (int) ((starty - volume) * view.getCellHeight()));
        layers.setLayer(ping, JLayeredPane.getLayer(view) + 10);
        RadiusStrategy rstrat = BasicRadiusStrategy.CIRCLE;
        int v = (int) volume;
        for (int x = 0; x <= v; x++) {
            for (int y = 0; y <= v; y++) {
                for (Direction dir : Direction.DIAGONALS) {
                    int x2 = v + x * dir.deltaX;
                    int y2 = v + y * dir.deltaY;
                    int x3 = startx + x * dir.deltaX;
                    int y3 = starty + y * dir.deltaY;
                    if (x2 >= 0 && x2 < ping.getGridWidth() && y2 >= 0 && y2 < ping.getGridHeight()
                            && x3 >= 0 && x3 < view.getGridWidth() && y3 >= 0 && y3 < view.getGridHeight()) {
                        ping.placeImage(x2, y2, view.getCellImage(x3, y3));
                    }
                }
            }
        }
        ping.refresh();
        layers.add(ping);

        try {
            for (int i = 0; i <= v; i += 2) {
                for (int x = 0; x <= v; x++) {
                    for (int y = 0; y <= v; y++) {
                        for (Direction dir : Direction.DIAGONALS) {
                            int x2 = v + x * dir.deltaX;
                            int y2 = v + y * dir.deltaY;
                            int x3 = startx + x * dir.deltaX;
                            int y3 = starty + y * dir.deltaY;
                            if (x2 >= 0 && x2 < ping.getGridWidth() && y2 >= 0 && y2 < ping.getGridHeight()
                                    && x3 >= 0 && x3 < view.getGridWidth() && y3 >= 0 && y3 < view.getGridHeight()) {
                                if ((int) rstrat.radius(x, y) == i) {
                                    ping.placeCharacter(x2, y2, '|', SColor.IBIS_WING);
                                } else if ((int) rstrat.radius(x, y) == i - 2) {
                                    ping.placeCharacter(x2, y2, '|', SColor.AQUAMARINE);
                                } else {
                                    ping.placeImage(x2, y2, view.getCellImage(x3, y3));
                                }
                            }
                        }
                    }
                }
//                ping.placeCharacter(v, v, '?', SColor.AMBER);
                ping.refresh();
                Thread.sleep(pause);
            }
        } catch (InterruptedException ex) {
        }

        layers.remove(ping);
        view.refresh();
    }

    /**
     * Displays a menu with the given items and title. Returns the text of the
     * chosen item.
     *
     * Will return an empty string if menu exited with Escape key.
     *
     * @param title
     * @param items
     * @return
     */
    private String menu(String title, String[] items) {
        view.refresh();
        int menuWidth = 0;
        for (String s : items) {
            menuWidth = Math.max(menuWidth, s.length());
        }
        menuWidth += 2;
        int menuHeight = items.length * 2 + 4;

        SwingPane menuPane = new SwingPane(view.getCellWidth(), view.getCellHeight(), Math.max(title.length(), menuWidth) + 2, menuHeight, new Font(viewFont.getFamily(), Font.BOLD, fontSize));
        layers.add(menuPane);
        layers.setLayer(menuPane, JLayeredPane.POPUP_LAYER);
        menuPane.setLocation((layers.getWidth() - menuPane.getWidth()) / 2, (layers.getHeight() - menuPane.getHeight()) / 2);
        menuPane.placeHorizontalString(1, 1, title);

        String background = "";
        for (int i = 0; i < menuWidth; i++) {
            background += " ";
        }

        keys.flush();//make sure there's not a backlog of input
        boolean selecting = true;
        int menuYoffset = 3;
        for (int y = 0; y < items.length; y++) {
//            menuPane.placeHorizontalString((menuWidth - items[y].length()) / 2, menuYoffset + y * 2, items[y], SColor.BLOOD_RED, SColor.BLUE_VIOLET_DYE);//centers the item
            menuPane.placeHorizontalString(1, menuYoffset + y * 2, background, SColor.BLOOD_RED, SColor.BLUE_VIOLET_DYE);
            menuPane.placeHorizontalString(2, menuYoffset + y * 2, items[y], SColor.BLOOD_RED, SColor.BLUE_VIOLET_DYE);
        }
        int highlightItem = 0;

        while (selecting) {
            int highlightY = menuYoffset + highlightItem * 2;
            menuPane.highlight(1, highlightY, menuWidth, highlightY);
            menuPane.refresh();

            int key = keys.next().getKeyCode();
            if (key == VK_ESCAPE) {
                layers.remove(menuPane);
                return "";
            } else if (key == VK_ENTER) {
                break;//selected
            }
            Direction dir = getDirection(key);
            if (dir == Direction.DOWN) {
                highlightItem = (highlightItem + 1) % items.length;
            } else if (dir == Direction.UP) {
                highlightItem--;
                if (highlightItem < 0) {
                    highlightItem = items.length - 1;
                }
            }
        }

        layers.remove(menuPane);

        return items[highlightItem];
    }

    private void buildCustomRobot() {
        boolean done = false;
        while (!done) {
            updateStats(player);
            switch (menu("Creation Points " + playerXP, new String[]{"Name", "Weapon",
                "Add Health (40)", "Add Shields (40)", "Add Barrier (40)", "Add Armor (40)", "Add Thermal Clips (10)", "Add Thermal Clips x 10 (90)", "Done"})) {
                case "Name":
                    player.name = menu("Choose Name", new String[]{"X-1", "Number 5", "Tortoise", "NCC-1701-D", "THX-1138"});
                    if (player.name == "") {
                        player.name = "N.U.L.L.";
                    }
                    break;
                case "Weapon":
                    HashMap<String, Weapon> weapons = new HashMap<>();
                    weapons.put(Weapon.PREDATOR.name + " (" + Weapon.PREDATOR.weight + ")", Weapon.PREDATOR);
                    weapons.put(Weapon.CARNIFEX.name + " (" + Weapon.CARNIFEX.weight + ")", Weapon.CARNIFEX);
                    weapons.put(Weapon.REVENANT.name + " (" + Weapon.REVENANT.weight + ")", Weapon.REVENANT);
                    weapons.put(Weapon.WIDOW.name + " (" + Weapon.WIDOW.weight + ")", Weapon.WIDOW);
                    String answer = menu("Choose A Weapon", weapons.keySet().toArray(new String[]{}));
                    weapons.put("", Weapon.PREDATOR);
                    Weapon choice = weapons.get(answer);
                    playerXP += player.weapon.weight;
                    player.weapon = choice.clone();
                    playerXP -= player.weapon.weight;
                    break;
                case "Add Health (40)":
                    if (playerXP >= 40) {
                        player.health += 100;
                        playerXP -= 40;
                    }
                    break;
                case "Add Shields (40)":
                    if (playerXP >= 40) {
                        player.shield += 100;
                        playerXP -= 40;
                    }
                    break;
                case "Add Barrier (40)":
                    if (playerXP >= 40) {
                        player.barrier += 100;
                        playerXP -= 40;
                    }
                    break;
                case "Add Armor (40)":
                    if (playerXP >= 40) {
                        player.armor += 100;
                        playerXP -= 40;
                    }
                    break;
                case "Add Thermal Clips (10)":
                    if (playerXP >= 10) {
                        player.thermalClips++;
                        playerXP -= 10;
                    }
                    break;
                case "Add Thermal Clips x 10 (90)":
                    if (playerXP >= 90) {
                        player.thermalClips += 10;
                        playerXP -= 90;
                    }
                    break;
                case "":
                case "Done":
                    if (player.name == "") {
                        player.name = "N.U.L.L.";
                    }
                    done = true;
                    break;
            }
        }
    }

    private void updateDisplay() {
        updateStats(player);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                char disp = map.map[x][y].getSymbol();
                Character rep = replacements.get(disp);
                if (rep != null) {
                    disp = rep;
                }
                view.placeCharacter(x, y, disp, map.map[x][y].foregroundColor());

                Creature c = map.map[x][y].creature;//player can hear creature when it first comes range and not yet heard
                if (c != null && c != player && !c.heard) {
                    float volume = c.getVolume() * player.hearing;//volume as far as the player is concerned
                    if (volume >= hearingStrategy.radius(c.location.x, c.location.y, player.location.x, player.location.y)) {
                        c.heard = true;
                        sound.playSoundFX(c.idleSound);
                        ping(c.location.x, c.location.y, volume, 18);
                    }
                }
            }
        }
        view.refresh();
        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
        }
    }

    /**
     * Shows the stats for the passed in creature.
     *
     * @param creature
     */
    private void updateStats(Creature creature) {
        for (int x = 0; x < stats.getGridWidth(); x++) {
            stats.clearCell(x, 0, SColor.LIGHT_BLUE);
        }
        for (int x = 0; x < stats.getGridWidth(); x++) {
            for (int y = 1; y < stats.getGridHeight(); y++) {
                stats.clearCell(x, y);
            }
        }
        String words = " Turn " + turn + "   Wave " + wave;
        stats.placeHorizontalString((statWidth - words.length()) / 2, stats.getGridHeight() - 1, words, SColor.ALICE_BLUE, SColor.BLACK_CHESTNUT_OAK);

        if (creature == player) {
            words = "Money: " + playerMoney;
            stats.placeHorizontalString((statWidth - words.length()) / 2, stats.getGridHeight() - 3, words, SColor.GOLDEN, SColor.BLACK);
            words = "Experience: " + playerXP;
            stats.placeHorizontalString((statWidth - words.length()) / 2, stats.getGridHeight() - 5, words, SColor.WISTERIA, SColor.BLACK);
        }

        words = "STATS";
        stats.placeHorizontalString((statWidth - words.length()) / 2, 0, words, SColor.RED, SColor.LIGHT_BLUE);
        stats.placeHorizontalString((statWidth - creature.name.length()) / 2, 1, creature.name, creature.color, SColor.BLACK);

        stats.placeHorizontalString(0, 3, "Health: " + creature.health);
        double fraction = (double) creature.health / 100;
        for (int x = 0; x < stats.getGridWidth(); x++) {
            stats.placeCharacter(x, 4, '☻', (fraction > 0 && x <= stats.getGridWidth() * fraction) ? HEALTH : SColor.BLACK_CHESTNUT_OAK, SColor.BLACK_CHESTNUT_OAK);
        }
        stats.placeHorizontalString(0, 6, "Shields: " + creature.shield);
        fraction = (double) creature.shield / 100;
        for (int x = 0; x < stats.getGridWidth(); x++) {
            stats.placeCharacter(x, 7, '☻', (fraction > 0 && x <= stats.getGridWidth() * fraction) ? SHIELD : SColor.BLACK_CHESTNUT_OAK, SColor.BLACK_CHESTNUT_OAK);
        }
        stats.placeHorizontalString(0, 9, "Barrier: " + creature.barrier);
        fraction = (double) creature.barrier / 100;
        for (int x = 0; x < stats.getGridWidth(); x++) {
            stats.placeCharacter(x, 10, '☻', (fraction > 0 && x <= stats.getGridWidth() * fraction) ? BARRIER : SColor.BLACK_CHESTNUT_OAK, SColor.BLACK_CHESTNUT_OAK);
        }
        stats.placeHorizontalString(0, 12, "Armor: " + creature.armor);
        fraction = (double) creature.armor / 100;
        for (int x = 0; x < stats.getGridWidth(); x++) {
            stats.placeCharacter(x, 13, '☻', (fraction > 0 && x <= stats.getGridWidth() * fraction) ? ARMOR : SColor.BLACK_CHESTNUT_OAK, SColor.BLACK_CHESTNUT_OAK);
        }

        int y = 14;

        if (creature.weapon != null) {
            words = "Weapon: ";
            y = 16;
            stats.placeHorizontalString(0, y, words);
            stats.placeHorizontalString(words.length(), y, creature.weapon.name, creature.weapon.color, SColor.BLACK);

            words = "  Rounds: ";
            y++;
            stats.placeHorizontalString(0, y, words);
            stats.placeHorizontalString(words.length(), y, "" + creature.weapon.load, creature.weapon.load == 0 ? SColor.SCARLET : SColor.SCHOOL_BUS_YELLOW, SColor.BLACK);
            words += creature.weapon.load;
            stats.placeHorizontalString(words.length(), y, " / ");
            words += " / ";
            stats.placeHorizontalString(words.length(), y, "" + creature.weapon.capacity, SColor.ULTRAMARINE_DYE, SColor.BLACK);
            y++;
            fraction = (double) creature.weapon.load / creature.weapon.capacity;
            for (int x = 2; x < stats.getGridWidth() - 2; x++) {
                stats.placeCharacter(x, y, '☻', (fraction > 0 && x <= (stats.getGridWidth() - 2) * fraction) ? SColor.JADE : SColor.BLACK_CHESTNUT_OAK, SColor.BLACK_CHESTNUT_OAK);
            }

            words = "  Damage: ";
            y++;
            stats.placeHorizontalString(0, y, words);
            stats.placeHorizontalString(words.length(), y, "" + creature.weapon.damage, SColor.SAFETY_ORANGE, SColor.BLACK);
            words += creature.weapon.damage;
            stats.placeHorizontalString(words.length(), y, "  Accuracy: ");
            words += "  Accuracy: ";
            stats.placeHorizontalString(words.length(), y, "" + creature.weapon.accuracy, SColor.ULTRAMARINE_DYE, SColor.BLACK);

            words = "  Rate: ";
            y++;
            stats.placeHorizontalString(0, y, words);
            stats.placeHorizontalString(words.length(), y, "" + creature.weapon.rate, SColor.ULTRAMARINE_DYE, SColor.BLACK);
            words += creature.weapon.rate;
            stats.placeHorizontalString(words.length(), y, "  Loudness: ");
            words += "  Loudness: ";
            stats.placeHorizontalString(words.length(), y, "" + creature.weapon.volume, SColor.ULTRAMARINE_DYE, SColor.BLACK);

            y += 2;
            stats.placeHorizontalString(0, y, "Thermal Clips: " + creature.thermalClips);
            fraction = (double) creature.thermalClips / 100;
            y++;
            for (int x = 0; x < stats.getGridWidth(); x++) {
                stats.placeCharacter(x, y, '☻', (fraction > 0 && x <= stats.getGridWidth() * fraction) ? SColor.JADE : SColor.BLACK_CHESTNUT_OAK, SColor.BLACK_CHESTNUT_OAK);
            }
        }

        if (creature != player) {
            y += 2;
            words = "Attack Range: ";
            stats.placeHorizontalString(0, y, words);
            stats.placeHorizontalString(words.length(), y, "" + creature.range, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        }

        y += 2;
        words = "Punch Damage: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.meleeDamage, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        words += creature.meleeDamage;
        stats.placeHorizontalString(words.length(), y, "  Accuracy: ");
        words += "  Accuracy: ";
        stats.placeHorizontalString(words.length(), y, "" + creature.meleeAccuracy, SColor.ULTRAMARINE_DYE, SColor.BLACK);

        y += 2;
        words = "Senses";
        stats.placeHorizontalString(0, y, words);
        y++;
        words = "  Vision: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.vision, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y++;
        words = "  Sense of Smell: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.smell, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y++;
        words = "  Hearing: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.hearing, SColor.ULTRAMARINE_DYE, SColor.BLACK);

        y += 2;
        words = "Visibility";
        stats.placeHorizontalString(0, y, words);
        y++;
        words = "  Size: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.size, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y++;
        words = "  Opacity: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.opacity, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y++;
        words = "  Hiding Modifier: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.hidingModifier, creature.hiding ? SColor.SAFETY_ORANGE : SColor.ULTRAMARINE_DYE, SColor.BLACK);

        y += 2;
        words = "Sound";
        stats.placeHorizontalString(0, y, words);
        y++;
        words = "  Movmement: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.movement, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y++;
        words = "  Idle: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.conversation, SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y++;
        words = "  Sneaking Modifier: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.sneakingModifyer, creature.sneaking ? SColor.SAFETY_ORANGE : SColor.ULTRAMARINE_DYE, SColor.BLACK);
        y++;
        words = "  Hurrying Modifier: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.hurryingModifyer, creature.hurrying ? SColor.SAFETY_ORANGE : SColor.ULTRAMARINE_DYE, SColor.BLACK);

        y += 2;
        words = "Scent";
        stats.placeHorizontalString(0, y, words);
        y++;
        words = "  Odor: ";
        stats.placeHorizontalString(0, y, words);
        stats.placeHorizontalString(words.length(), y, "" + creature.odor, SColor.ULTRAMARINE_DYE, SColor.BLACK);

        stats.refresh();
        //TODO -- add section for ongoing effects
        try {
            Thread.sleep(1);
        } catch (InterruptedException ex) {
        }
    }

    private void output(String message) {
        output(message, false);
    }

    private void output(String message, boolean secondLine) {
        if (!secondLine) {
            for (int x = 0; x < output.getGridWidth(); x++) {
                output.clearCell(x, output.getGridHeight() - 1, SColor.IRON_STORAGE);
                output.clearCell(x, output.getGridHeight() - 2, SColor.IRON_STORAGE);
            }
            output.placeHorizontalString(0, 0, message);
        } else {
            output.placeHorizontalString(0, 1, message);
        }
        output.refresh();
    }

    /**
     * A simple input listener.
     */
    private class ViewMouseListener implements MouseInputListener {

        int startx, starty;
        boolean dragged = false;

        @Override
        public void mouseClicked(MouseEvent e) {
            dragged = false;
        }

        @Override
        public void mousePressed(MouseEvent e) {
            dragged = false;
            startx = e.getX();
            starty = e.getY();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (dragged && (startx != e.getX() || starty != e.getY())) {//only do if a drag even preceeded letting go of the button
            }
            dragged = false;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            //nothing special happens
        }

        @Override
        public void mouseExited(MouseEvent e) {
            //nothing special happens
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            dragged = true;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            //nothing special happens
        }
    }

    private void initPlayer() {
        player = new Creature();
        player.name = "";
        player.description = "A custom synthetic. Ready to fight for the cause!";
        player.color = SColor.COSMIC_LATTE;
        player.weapon = Weapon.PREDATOR.clone();
        player.symbol = '@';
        player.thermalClips = 5;
        player.meleeDamage = 10;
        player.hearing = 10;
        player.health = 100;
        player.deathSound = Sounds.PLAYER_DEATH;
    }

    private Direction getDirection(int code) {
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

    /**
     * Loads and registers the font in the given file or returns null if
     * something goes wrong.
     *
     * @param file
     * @return
     */
    private Font loadFont(File file) {
        try {
            Font tempFont = Font.createFont(Font.TRUETYPE_FONT, file);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(tempFont);
            return tempFont;
        } catch (FontFormatException | IOException ex) {
            return null;
        }
    }

    public void saveToFile(File file) {
        try {
            final FileOutputStream stream = new FileOutputStream(file, false);
            configureMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(stream, this);
        } catch (IOException ex) {
        }
    }

    public static GUI loadFromFile(File file) throws IOException {
        configureMapper();
        return mapper.readValue(file, GUI.class);
    }

    private static void configureMapper() {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    private void exit() {
        System.exit(0);
    }
}
