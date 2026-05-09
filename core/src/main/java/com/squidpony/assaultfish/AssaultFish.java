package com.squidpony.assaultfish;

import assaultfish.mapping.MapCell;
import assaultfish.physical.BallisticsSolver;
import assaultfish.physical.Creature;
import assaultfish.physical.Element;
import assaultfish.physical.Fish;
import assaultfish.physical.Size;
import assaultfish.physical.Terrain;
import assaultfish.physical.TerrainFeature;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TreeMap;
import java.awt.image.BufferedImage;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import squidpony.squidgrid.Direction;
import squidpony.squidgrid.FOV;
import squidpony.squidgrid.Radius;
import squidpony.squidgrid.gui.gdx.DefaultResources;
import squidpony.squidgrid.gui.gdx.SColor;
import squidpony.squidgrid.gui.gdx.SparseLayers;
import squidpony.squidgrid.gui.gdx.SquidColorCenter;
import squidpony.squidmath.Bresenham;
import squidpony.squidmath.Coord;
import squidpony.squidmath.GWTRNG;
import squidpony.squidmath.OrderedSet;
import squidpony.squidmath.PerlinNoise;

public class AssaultFish extends ApplicationAdapter {
    private static final String VERSION = "2.0.0";
    private static final String SOUND_PREF = "Sound Pref";
    private static final DateTimeFormatter SCREENSHOT_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter RECORDING_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final int RECORDING_GIF_FRAME_DELAY_MS = 100;

    private static final double WIDTH_SCALE = 1.2;
    private static final double HEIGHT_SCALE = 1.2;
    private static final int GRID_WIDTH = 80;
    private static final int GRID_HEIGHT = 40;
    private static final int INVENTORY_ROWS = 6;
    private static final int FISH_HEIGHT = (int) ((GRID_HEIGHT - 3) * HEIGHT_SCALE);
    private static final int FISH_WIDTH = (int) (GRID_WIDTH * WIDTH_SCALE);
    private static final int LARGE_TEXT_SCALE = 4;
    private static final int LIQUID_HEIGHT = LARGE_TEXT_SCALE * 4;
    private static final int TERRAIN_WIDTH = LARGE_TEXT_SCALE * 2 + 1;
    private static final int MAX_HEALTH = 6;
    private static final int HEALTH_X = GRID_WIDTH - 15;
    private static final int CELL_WIDTH = 18;
    private static final int CELL_HEIGHT = 24;
    private static final int FISH_CELL_WIDTH = 15;
    private static final int FISH_CELL_HEIGHT = 20;
    private static final int MAX_FISH = 6;
    private static final int OVERLAY_ALPHA = 100;
    private static final long OUTPUT_DURATION_MS = 5000L;

    private static final Rectangle HELP_ICON_LOCATION = new Rectangle(GRID_WIDTH - 5, 1, 4, 1);
    private static final Rectangle MUTE_ICON_LOCATION = new Rectangle(GRID_WIDTH - 5, 2, 4, 1);
    private static final Rectangle EXIT_ICON_LOCATION = new Rectangle(GRID_WIDTH - 5, 3, 4, 1);

    private final FOV fov = new FOV(FOV.SHADOW);
    private final GWTRNG rng = new GWTRNG(0x31337BEEFCA77L);
    private final SquidColorCenter colorCenter = new SquidColorCenter();

    private SpriteBatch batch;
    private Music music;
    private Preferences preferences;

    private SparseLayers mapPanel;
    private SparseLayers outputPanel;
    private SparseLayers meterPanel;
    private SparseLayers fishingLayers;
    private SparseLayers fishingPlayerPanel;
    private SparseLayers helpPane;
    private SparseLayers fishThrowingPanel;
    private SparseLayers winPane;
    private SparseLayers diePane;
    private SparseLayers overlayPanel;
    private SparseLayers fishInventoryPanel;

    private Stage stage;

    private Creature player;
    private final ArrayList<Creature> monsters = new ArrayList<>();
    private MapCell[][] map;

    private Fish selectedFish;
    private Coord overlayLocation = Coord.get(-1, -1);

    private final TreeMap<Element, TreeMap<Size, Integer>> fishInventory = new TreeMap<>();

    private boolean canCast;
    private boolean canClick = true;
    private boolean casting;
    private boolean nowFishing;
    private boolean mapMode = true;
    private boolean soundOn;
    private double castingStrength = 0.4;
    private long castStartTime;
    private long outputEndTime;
    private boolean recordingActive;
    private Path recordingFramesDir;
    private String recordingStamp;
    private int recordingFrameIndex;
    private boolean recordingCaptureRequested;
    private boolean recordingCaptureOnlyIfChanged;
    private boolean recordingHasFrameHash;
    private int recordingLastFrameHash;

    private boolean[][] terrainMap;
    private boolean[][] liquidMap;
    private Fish[][] fishMap;
    private final OrderedSet<Fish> fishes = new OrderedSet<>();
    private Terrain terrain;
    private Element element;
    private final char bobber = 'o';
    private final char hook = 'J';
    private Coord bobberLocation;
    private Coord castPreviewLocation;
    private Coord castBobberLocation;
    private Coord castStartLocation;
    private ArrayList<Coord> castArcPath;
    private int castArcIndex;
    private int castHookY;
    private Fish hookedFish;
    private float castAnimationClock;
    private CastAnimationPhase castAnimationPhase = CastAnimationPhase.NONE;
    private final Color lineColor = SColor.BURNT_BAMBOO;
    private final Color bobberColor = SColor.SCARLET;
    private final Color hookColor = SColor.BRASS;
    private final Color skyColor = SColor.ALICE_BLUE;
    private final Color playerColor = SColor.BETEL_NUT_DYE;
    private ArrayList<Color> meterPalette;

    private enum CastAnimationPhase {
        NONE,
        ARC,
        DROP,
        BOTTOM_PAUSE,
        REEL
    }

    @Override
    public void create() {
        preferences = Gdx.app.getPreferences("AssaultFish");
        soundOn = preferences.getBoolean(SOUND_PREF, true);

        music = Gdx.audio.newMusic(Gdx.files.internal("Eden.mp3"));
        music.setLooping(true);
        music.setVolume(soundOn ? 1f : 0f);
        music.play();

        meterPalette = colorCenter.gradient(SColor.RED, SColor.ORANGE);
        meterPalette.addAll(colorCenter.gradient(SColor.ORANGE, SColor.YELLOW));
        meterPalette.addAll(colorCenter.gradient(SColor.YELLOW, SColor.ELECTRIC_GREEN));
        Fish.initSymbols(DefaultResources.getCrispDejaVuFont().width(CELL_WIDTH).height(CELL_HEIGHT).initBySize().font());

        for (Element e : Element.values()) {
            TreeMap<Size, Integer> sizeInventory = new TreeMap<>();
            for (Size s : Size.values()) {
                sizeInventory.put(s, rng.nextInt(2));
            }
            fishInventory.put(e, sizeInventory);
        }

        player = new Creature(Creature.PLAYER);
        player.health = MAX_HEALTH;
        player.color = SColor.CORNFLOWER_BLUE;

        batch = new SpriteBatch();
        stage = new Stage(new StretchViewport(GRID_WIDTH * CELL_WIDTH, (GRID_HEIGHT + INVENTORY_ROWS) * CELL_HEIGHT), batch);

        initializeFrame();
        initializeFishInventory();
        createMap();
        updateMap();
        flipMouseControl(true);
        installInputProcessor();
        showHelp();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (casting) {
            updateCastingMeter();
        }
        if (castAnimationPhase != CastAnimationPhase.NONE) {
            updateCastAnimation(Gdx.graphics.getDeltaTime());
        }

        if (outputEndTime > 0L && System.currentTimeMillis() > outputEndTime) {
            outputPanel.clear();
            outputEndTime = 0L;
        }

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
        flushRecordingCapture();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (music != null) {
            music.stop();
            music.dispose();
        }
        if (stage != null) {
            stage.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }

    private void installInputProcessor() {
        InputAdapter input = new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.P) {
                    takeScreenshot();
                    return true;
                }
                if (keycode == Input.Keys.V) {
                    toggleRecording();
                    return true;
                }
                if (keycode == Input.Keys.H) {
                    showHelp();
                    return true;
                }
                if (keycode == Input.Keys.ESCAPE) {
                    exiting();
                    return true;
                }
                if (!mapMode || !canClick) {
                    return false;
                }

                Direction direction = switch (keycode) {
                    case Input.Keys.LEFT, Input.Keys.A, Input.Keys.H -> Direction.LEFT;
                    case Input.Keys.RIGHT, Input.Keys.D, Input.Keys.L -> Direction.RIGHT;
                    case Input.Keys.UP, Input.Keys.W, Input.Keys.K -> Direction.UP;
                    case Input.Keys.DOWN, Input.Keys.S, Input.Keys.J -> Direction.DOWN;
                    default -> null;
                };
                if (direction != null) {
                    workClick(player.x + direction.deltaX, player.y + direction.deltaY);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector2 stagePoint = stage.screenToStageCoordinates(new Vector2(screenX, screenY));
                if (nowFishing) {
                    if (button == Input.Buttons.LEFT) {
                        if (casting) {
                            finishCast();
                        } else if (canCast) {
                            startCasting();
                        }
                        return true;
                    }
                    if (button == Input.Buttons.RIGHT && !casting && canCast) {
                        stopFishing();
                        return true;
                    }
                    return false;
                }

                int inventoryHeight = INVENTORY_ROWS * CELL_HEIGHT;
                if (stagePoint.y < inventoryHeight) {
                    int gridX = (int) (stagePoint.x / CELL_WIDTH);
                    int gridY = toInventoryGridY(stagePoint.y);
                    return handleInventoryClick(gridX, gridY);
                }

                int mapX = (int) (stagePoint.x / CELL_WIDTH);
                int mapY = toMapGridY(stagePoint.y);
                if (mapX < 0 || mapY < 0 || mapX >= GRID_WIDTH || mapY >= GRID_HEIGHT || !canClick) {
                    return false;
                }

                boolean examine = button == Input.Buttons.MIDDLE
                        || (button == Input.Buttons.LEFT
                        && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)));
                if (examine) {
                    describeTile(mapX, mapY);
                    return true;
                }
                if (button == Input.Buttons.RIGHT) {
                    if (selectedFish != null) {
                        selectedFish = null;
                        updateFishInventoryPanel();
                        updateOverlay();
                    }
                    return true;
                }
                if (button == Input.Buttons.LEFT) {
                    if (selectedFish == null) {
                        workClick(mapX, mapY);
                    } else {
                        throwFish(mapX, mapY);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (!mapMode) {
                    return false;
                }
                Vector2 stagePoint = stage.screenToStageCoordinates(new Vector2(screenX, screenY));
                int inventoryHeight = INVENTORY_ROWS * CELL_HEIGHT;
                if (stagePoint.y < inventoryHeight) {
                    overlayPanel.setVisible(false);
                    return false;
                }
                int mapX = (int) (stagePoint.x / CELL_WIDTH);
                int mapY = toMapGridY(stagePoint.y);
                if (mapX < 0 || mapY < 0 || mapX >= GRID_WIDTH || mapY >= GRID_HEIGHT) {
                    overlayPanel.setVisible(false);
                    return false;
                }
                overlayPanel.setVisible(true);
                overlayLocation = Coord.get(mapX, mapY);
                updateOverlay();
                return true;
            }
        };

        Gdx.input.setInputProcessor(new InputMultiplexer(stage, input));
    }

    private boolean handleInventoryClick(int x, int y) {
        if (HELP_ICON_LOCATION.contains(x, y)) {
            showHelp();
            return true;
        }
        if (EXIT_ICON_LOCATION.contains(x, y)) {
            exiting();
            return true;
        }
        if (MUTE_ICON_LOCATION.contains(x, y)) {
            soundOn = !soundOn;
            music.setVolume(soundOn ? 1f : 0f);
            preferences.putBoolean(SOUND_PREF, soundOn);
            preferences.flush();
            initializeFishInventory();
            return true;
        }

        for (Element e : Element.values()) {
            int startX = 1 + e.ordinal() * (MAX_FISH + 1);
            if (x < startX || x >= startX + MAX_FISH) {
                continue;
            }
            if (y < 1 || y > Size.values().length) {
                continue;
            }
            Size size = Size.values()[y - 1];
            if (fishInventory.get(e).get(size) < 1) {
                return true;
            }
            if (selectedFish != null && selectedFish.element == e && selectedFish.size == size) {
                selectedFish = null;
            } else {
                selectedFish = new Fish(size, e);
            }
            updateFishInventoryPanel();
            updateOverlay();
            return true;
        }
        return false;
    }

    private int toInventoryGridY(float stageY) {
        int row = (int) (stageY / CELL_HEIGHT);
        return INVENTORY_ROWS - 1 - row;
    }

    private int toMapGridY(float stageY) {
        int inventoryHeight = INVENTORY_ROWS * CELL_HEIGHT;
        int row = (int) ((stageY - inventoryHeight) / CELL_HEIGHT);
        return GRID_HEIGHT - 1 - row;
    }

    private void describeTile(int x, int y) {
        StringBuilder description = new StringBuilder();
        MapCell tile = map[x][y];
        description.append("Terrain: ").append(tile.terrain.name);
        if (tile.terrain.lake) {
            description.append(", it can be fished.");
        }
        if (tile.feature != null) {
            description.append(" Feature: ").append(tile.feature.name).append('.');
        }
        if (tile.creature != null) {
            description.append(" Creature: ").append(tile.creature.name).append('.');
        }
        if (tile.item != null) {
            description.append(" Item: ").append(tile.item.name).append('.');
        }
        printOut(description.toString());
    }

    private void initializeFrame() {
        squidpony.squidgrid.gui.gdx.TextCellFactory textFactory = DefaultResources.getCrispDejaVuFont()
                .width(CELL_WIDTH)
                .height(CELL_HEIGHT)
                .initBySize();
        squidpony.squidgrid.gui.gdx.TextCellFactory fishTextFactory = DefaultResources.getCrispDejaVuFont()
                .width(FISH_CELL_WIDTH)
                .height(FISH_CELL_HEIGHT)
                .initBySize();
        squidpony.squidgrid.gui.gdx.TextCellFactory largeTextFactory = DefaultResources.getCrispDejaVuFont()
            .width(FISH_CELL_WIDTH * LARGE_TEXT_SCALE)
            .height(FISH_CELL_HEIGHT * LARGE_TEXT_SCALE)
            .initBySize();

        float mapY = INVENTORY_ROWS * CELL_HEIGHT;

        mapPanel = new SparseLayers(GRID_WIDTH, GRID_HEIGHT, CELL_WIDTH, CELL_HEIGHT, textFactory);
        mapPanel.setPosition(0f, mapY);
        stage.addActor(mapPanel);

        overlayPanel = new SparseLayers(GRID_WIDTH, GRID_HEIGHT, CELL_WIDTH, CELL_HEIGHT, textFactory);
        overlayPanel.setPosition(0f, mapY);
        overlayPanel.setVisible(false);
        stage.addActor(overlayPanel);

        fishThrowingPanel = new SparseLayers(GRID_WIDTH, GRID_HEIGHT, CELL_WIDTH, CELL_HEIGHT, textFactory);
        fishThrowingPanel.setPosition(0f, mapY);
        stage.addActor(fishThrowingPanel);

        outputPanel = new SparseLayers(GRID_WIDTH, GRID_HEIGHT, CELL_WIDTH, CELL_HEIGHT, textFactory);
        outputPanel.setPosition(0f, mapY);
        stage.addActor(outputPanel);

        fishInventoryPanel = new SparseLayers(GRID_WIDTH, INVENTORY_ROWS, CELL_WIDTH, CELL_HEIGHT, textFactory);
        fishInventoryPanel.setPosition(0f, 0f);
        stage.addActor(fishInventoryPanel);

        fishingLayers = new SparseLayers(FISH_WIDTH, FISH_HEIGHT, FISH_CELL_WIDTH, FISH_CELL_HEIGHT, fishTextFactory);
        fishingLayers.setPosition(0f, mapY);
        fishingLayers.setVisible(false);
        stage.addActor(fishingLayers);

        fishingPlayerPanel = new SparseLayers(FISH_WIDTH / LARGE_TEXT_SCALE, FISH_HEIGHT / LARGE_TEXT_SCALE,
            FISH_CELL_WIDTH * LARGE_TEXT_SCALE, FISH_CELL_HEIGHT * LARGE_TEXT_SCALE, largeTextFactory);
        fishingPlayerPanel.setPosition(0f, mapY);
        fishingPlayerPanel.setVisible(false);
        stage.addActor(fishingPlayerPanel);

        meterPanel = new SparseLayers(GRID_WIDTH, 3, CELL_WIDTH, CELL_HEIGHT, textFactory);
        meterPanel.setPosition(0f, mapY);
        meterPanel.setVisible(false);
        stage.addActor(meterPanel);
        initMeter();
    }

    private void flipMouseControl(boolean enableMapMode) {
        mapMode = enableMapMode;
        overlayPanel.setVisible(enableMapMode && selectedFish != null);
    }

    private void initializeFishInventory() {
        fishInventoryPanel.clear();
        int x = 1;
        for (Element e : Element.values()) {
            fishInventoryPanel.put(x, 0, e.name, e.color);
            x += MAX_FISH + 1;
        }
        fishInventoryPanel.put(HEALTH_X, 1, "Health", SColor.BLOOD);
        fishInventoryPanel.put((int) HELP_ICON_LOCATION.x, (int) HELP_ICON_LOCATION.y, "HELP", SColor.CREAM);
        fishInventoryPanel.put((int) MUTE_ICON_LOCATION.x, (int) MUTE_ICON_LOCATION.y, soundOn ? "MUTE" : "UNMT", SColor.SAFETY_ORANGE);
        fishInventoryPanel.put((int) EXIT_ICON_LOCATION.x, (int) EXIT_ICON_LOCATION.y, "EXIT", SColor.BRILLIANT_ROSE);
        updateFishInventoryPanel();
    }

    private void updateFishInventoryPanel() {
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 1; y < INVENTORY_ROWS; y++) {
                if (!HELP_ICON_LOCATION.contains(x, y) && !MUTE_ICON_LOCATION.contains(x, y) && !EXIT_ICON_LOCATION.contains(x, y)
                        && !(x >= HEALTH_X && x < HEALTH_X + MAX_HEALTH && y == 2)) {
                    fishInventoryPanel.clear(x, y);
                }
            }
        }

        int x = 1;
        for (Element e : Element.values()) {
            int y = 1;
            for (Size s : Size.values()) {
                int count = fishInventory.get(e).get(s);
                for (int i = 0; i < MAX_FISH; i++) {
                    if (i < count) {
                        fishInventoryPanel.put(x + i, y, Fish.symbol(s).charAt(0), e.color);
                    }
                }
                if (selectedFish != null && selectedFish.element == e && selectedFish.size == s) {
                    float highlight = SColor.floatGet(e.color.r, e.color.g, e.color.b, 0.25f);
                    for (int i = 0; i < MAX_FISH; i++) {
                        fishInventoryPanel.put(x + i, y, highlight);
                        if (i < count) {
                            fishInventoryPanel.put(x + i, y, Fish.symbol(s).charAt(0), e.color);
                        }
                    }
                }
                y++;
            }
            x += MAX_FISH + 1;
        }

        for (x = 0; x < MAX_HEALTH; x++) {
            if (x < player.health) {
                fishInventoryPanel.put(x + HEALTH_X, 2, bobber, SColor.BLOOD);
            } else {
                fishInventoryPanel.clear(x + HEALTH_X, 2);
            }
        }

        fishInventoryPanel.put((int) MUTE_ICON_LOCATION.x, (int) MUTE_ICON_LOCATION.y, soundOn ? "MUTE" : "UNMT", SColor.SAFETY_ORANGE);
        if (nowFishing) {
            requestFishingRecordingFrame();
        }
    }

    private void showHelp() {
        if (helpPane == null) {
            helpPane = new SparseLayers(GRID_WIDTH, GRID_HEIGHT, CELL_WIDTH, CELL_HEIGHT,
                    DefaultResources.getCrispDejaVuFont().width(CELL_WIDTH).height(CELL_HEIGHT).initBySize());
            helpPane.setPosition(0f, INVENTORY_ROWS * CELL_HEIGHT);
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

            text = "ASSAULT FISH  v" + VERSION;
            x = (helpPane.gridWidth() - text.length()) / 2;
            helpPane.put(x, y, text, heading);
            y += 2;

            text = "Your peaceful life as a fisherman has come to an end.";
            helpPane.put(left, y++, text, SColor.WHITE);
            text = "A horde of elementals has descended on the land, and you fight";
            helpPane.put(left, y++, text, SColor.WHITE);
            text = "them with the only weapon you truly know: explosive fish.";
            helpPane.put(left, y++, text, SColor.WHITE);
            y += 2;

            text = "Main Map Controls";
            x = (helpPane.gridWidth() - text.length()) / 2;
            helpPane.put(x, y, text, heading);
            y += 2;

            helpPane.put(left, y, "Left click", command);
            helpPane.put(left + 11, y++, "- move or throw a selected fish", SColor.WHITE);
            helpPane.put(left, y, "Ctrl-Left click", command);
            helpPane.put(left + 16, y++, "- examine a tile", SColor.WHITE);
            helpPane.put(left, y, "Right click", command);
            helpPane.put(left + 12, y++, "- deselect a fish", SColor.WHITE);
            helpPane.put(left, y, "H / HELP", command);
            helpPane.put(left + 10, y++, "- show this screen", SColor.WHITE);
            helpPane.put(left, y, "P", command);
            helpPane.put(left + 2, y++, "- save a screenshot", SColor.WHITE);
            helpPane.put(left, y, "V", command);
            helpPane.put(left + 2, y++, "- start or stop animated GIF recording", SColor.WHITE);
            y += 2;

            text = "Fishing Controls";
            x = (helpPane.gridWidth() - text.length()) / 2;
            helpPane.put(x, y, text, heading);
            y += 2;

            helpPane.put(left, y, "Left click", command);
            helpPane.put(left + 11, y++, "- start or release the cast meter", SColor.WHITE);
            helpPane.put(left, y, "Right click", command);
            helpPane.put(left + 12, y++, "- stop fishing", SColor.WHITE);
            y += 2;

            text = "Elemental Weaknesses";
            x = (helpPane.gridWidth() - text.length()) / 2;
            helpPane.put(x, y, text, heading);
            y += 2;
            helpPane.put(left, y, "Acid - Sand", Element.ACID.color);
            helpPane.put(left + 22, y++, "Air - Mana", Element.AIR.color);
            helpPane.put(left, y, "Blood - Tar", Element.BLOOD.color);
            helpPane.put(left + 22, y++, "Magma - Water", Element.MAGMA.color);
            helpPane.put(left, y, "Mana - Blood", Element.MANA.color);
            helpPane.put(left + 22, y++, "Sand - Acid", Element.SAND.color);
            helpPane.put(left, y, "Tar - Magma", Element.TAR.color);
            helpPane.put(left + 22, y, "Water - Air", Element.WATER.color);

            text = "-- press mouse button to continue --";
            x = (helpPane.gridWidth() - text.length()) / 2;
            y = GRID_HEIGHT - 3;
            helpPane.put(x, y, text, heading);
            helpPane.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    stage.getActors().removeValue(helpPane, true);
                }
            });
        }
        if (helpPane.getStage() == null) {
            stage.addActor(helpPane);
        }
    }

    private void goFish() {
        nowFishing = true;
        selectedFish = null;
        castPreviewLocation = null;
        bobberLocation = null;
        castAnimationPhase = CastAnimationPhase.NONE;
        castBobberLocation = null;
        castStartLocation = Coord.get(TERRAIN_WIDTH - 2, LARGE_TEXT_SCALE * 2 + 1);
        castArcPath = null;
        hookedFish = null;
        updateFishInventoryPanel();
        fishes.clear();
        fishingLayers.clear();
        fishingPlayerPanel.clear();
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
        fishingLayers.setVisible(true);
        fishingPlayerPanel.setVisible(true);
        meterPanel.setVisible(true);
        flipMouseControl(false);
        printOut("You are now fishing from " + terrain.name + " shore into the " + element.name + " lake.");
        canCast = true;
    }

    private void stopFishing() {
        casting = false;
        nowFishing = false;
        canCast = false;
        castPreviewLocation = null;
        bobberLocation = null;
        castAnimationPhase = CastAnimationPhase.NONE;
        fishingLayers.setVisible(false);
        fishingPlayerPanel.setVisible(false);
        meterPanel.setVisible(false);
        meterPanel.clear();
        initMeter();
        flipMouseControl(true);
        printOut("You stop fishing.");
    }

    private void startCasting() {
        casting = true;
        castStartTime = System.currentTimeMillis();
        castPreviewLocation = Coord.get(clampedCastTargetX(castingStrength), LIQUID_HEIGHT - 1);
        updateCastingMeter();
    }

    private void finishCast() {
        if (!canCast) {
            return;
        }
        casting = false;
        canCast = false;
        castPreviewLocation = null;
        startCastAnimation(clampedCastTargetX(castingStrength));
    }

    private void startCastAnimation(int targetX) {
        castAnimationClock = 0f;
        castArcIndex = 0;
        hookedFish = null;
        castHookY = -1;
        castStartLocation = Coord.get(TERRAIN_WIDTH - 2, LARGE_TEXT_SCALE * 2 + 1);

        BallisticsSolver solver = new BallisticsSolver(castStartLocation.x, castStartLocation.y, targetX, LIQUID_HEIGHT - 2, 10, 20);
        int solveHeight = FISH_WIDTH / (targetX + 5);
        solveHeight = Math.min(solveHeight, LARGE_TEXT_SCALE * 2 - 2);
        solveHeight = Math.max(solveHeight, 1);
        solver.solveByHeight(solveHeight);

        castArcPath = new ArrayList<>();
        double trueTime = solver.getTime();
        int targetTime = targetX * 20;
        int steps = Math.max(1, targetTime / 20);
        Coord last = null;
        for (int i = 0; i <= steps; i++) {
            double time = (double) i / steps;
            double solverTime = trueTime * time;
            int x = Math.max(0, Math.min(FISH_WIDTH - 1, solver.x(solverTime)));
            int y = Math.max(0, Math.min(FISH_HEIGHT - 1, solver.y(solverTime)));
            Coord point = Coord.get(x, y);
            if (last == null || !last.equals(point)) {
                castArcPath.add(point);
                last = point;
            }
        }

        if (castArcPath.isEmpty()) {
            castArcPath.add(Coord.get(targetX, LIQUID_HEIGHT - 1));
        }
        castAnimationPhase = CastAnimationPhase.ARC;
        castBobberLocation = castArcPath.get(0);
        initFishingDisplay();
    }

    private void updateCastAnimation(float delta) {
        castAnimationClock += delta;
        switch (castAnimationPhase) {
            case ARC -> {
                if (castAnimationClock < 0.03f) {
                    return;
                }
                castAnimationClock = 0f;
                castArcIndex = Math.min(castArcIndex + 1, castArcPath.size() - 1);
                castBobberLocation = castArcPath.get(castArcIndex);
                if (castArcIndex >= castArcPath.size() - 1) {
                    bobberLocation = Coord.get(castBobberLocation.x, Math.min(FISH_HEIGHT - 1, castBobberLocation.y + 1));
                    castHookY = Math.min(FISH_HEIGHT - 1, bobberLocation.y + 1);
                    castAnimationPhase = CastAnimationPhase.DROP;
                }
                initFishingDisplay();
            }
            case DROP -> {
                if (castAnimationClock < 0.01f) {
                    return;
                }
                castAnimationClock = 0f;
                int bedY = bed(bobberLocation.x);
                if (castHookY < bedY) {
                    castHookY++;
                    if (hookedFish == null && fishMap[bobberLocation.x][castHookY] != null) {
                        hookedFish = fishMap[bobberLocation.x][castHookY];
                        fishMap[bobberLocation.x][castHookY] = null;
                        fishes.remove(hookedFish);
                    }
                    initFishingDisplay();
                } else {
                    castAnimationPhase = CastAnimationPhase.BOTTOM_PAUSE;
                }
            }
            case BOTTOM_PAUSE -> {
                if (castAnimationClock < 0.2f) {
                    return;
                }
                castAnimationClock = 0f;
                castAnimationPhase = CastAnimationPhase.REEL;
            }
            case REEL -> {
                if (castAnimationClock < 0.04f) {
                    return;
                }
                castAnimationClock = 0f;
                if (castHookY > bobberLocation.y + 1) {
                    castHookY--;
                    initFishingDisplay();
                } else {
                    finishCastAnimation();
                }
            }
            case NONE -> {
            }
        }
    }

    private void finishCastAnimation() {
        castAnimationPhase = CastAnimationPhase.NONE;
        if (hookedFish != null) {
            addFish(hookedFish);
            hookedFish = null;
        } else {
            printOut("Nothing took the bait.");
        }
        bobberLocation = null;
        castBobberLocation = null;
        castArcPath = null;
        castHookY = -1;
        initFishingDisplay();
        runTurn();
        canCast = true;
    }

    private Fish catchFishAt(int targetX) {
        for (int offset = 0; offset < 4; offset++) {
            for (int direction : new int[]{0, -1, 1}) {
                int x = targetX + offset * direction;
                if (x <= TERRAIN_WIDTH * 2 || x >= FISH_WIDTH) {
                    continue;
                }
                for (int y = LIQUID_HEIGHT + 1; y <= bed(x); y++) {
                    Fish fish = fishMap[x][y];
                    if (fish != null) {
                        fishMap[x][y] = null;
                        fishes.remove(fish);
                        bobberLocation = Coord.get(x, y);
                        fishingLayers.put(x, y, bobber, bobberColor);
                        return fish;
                    }
                }
            }
        }
        return null;
    }

    private void updateCastingMeter() {
        meterPanel.clear();
        initMeter();
        if (!casting) {
            requestFishingRecordingFrame();
            return;
        }
        double elapsed = System.currentTimeMillis() - castStartTime;
        castingStrength = 1.0 - Math.abs(Math.sin(elapsed / 1000.0));
        int meterOffset = 3;
        int meterSize = GRID_WIDTH - meterOffset * 2;
        int drawX = Math.min((int) (castingStrength * meterSize), meterSize);
        int targetX = clampedCastTargetX(castingStrength);
        castPreviewLocation = Coord.get(targetX, LIQUID_HEIGHT - 1);
        initFishingDisplay();
        for (int i = 0; i < meterSize; i++) {
            if (i < drawX) {
                Color color = meterPalette.get(Math.min(i, meterPalette.size() - 1));
                meterPanel.put(i + meterOffset, 1, bobber, color);
            }
        }
        requestFishingRecordingFrame();
    }

    private void initMeter() {
        meterPanel.put((GRID_WIDTH - "Cast Strength".length()) / 2, 2, "Cast Strength", SColor.WHITE);
        meterPanel.put(2, 2, "None", SColor.RED);
        meterPanel.put(GRID_WIDTH - 3 - "Max".length(), 2, "Max", SColor.ELECTRIC_GREEN);
    }

    private void runTurn() {
        updateMap();
        if (monsters.isEmpty()) {
            win();
            requestMapTurnRecordingFrame();
            return;
        }
        checkForReactions();
        moveAllMonsters();
        updateMap();
        requestMapTurnRecordingFrame();
    }

    private void workClick(final int x, final int y) {
        boolean success = tryToMove(Direction.getDirection(x - player.x, y - player.y));
        if (success) {
            runTurn();
        }
    }

    private void exiting() {
        finalizeRecordingBeforeExit();
        Gdx.app.exit();
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

        overlayPanel.remove();

        int radius = switch (selectedFish.size) {
            case SMALL -> 2;
            case MEDIUM -> 3;
            case LARGE -> 5;
            case GIANT -> 9;
        };

        Queue<Coord> line = Bresenham.line2D(Coord.get(player.x, player.y), Coord.get(targetX, targetY));
        while (!line.isEmpty()) {
            Coord point = line.poll();
            targetX = point.x;
            targetY = point.y;
            fishThrowingPanel.clear();
            fishThrowingPanel.put(targetX, targetY, selectedFish.symbol.charAt(0), selectedFish.color);
            if ((map[targetX][targetY].creature != null && map[targetX][targetY].creature != player)
                    || (map[targetX][targetY].feature != null && map[targetX][targetY].feature != TerrainFeature.BUSH)) {
                break;
            }
        }

        boolean[][] modified = new boolean[GRID_WIDTH][GRID_HEIGHT];
        Radius strat = Radius.CIRCLE;
        for (int x = targetX - radius; x <= targetX + radius; x++) {
            for (int y = targetY - radius; y <= targetY + radius; y++) {
                if (x < 0 || y < 0 || x >= GRID_WIDTH || y >= GRID_HEIGHT) {
                    continue;
                }
                if (strat.radius(targetX, targetY, x, y) <= radius + 0.1 && !modified[x][y]) {
                    reactToElementChange(x, y, selectedFish.element);
                    fishThrowingPanel.put(x, y, '*', selectedFish.color);
                    modified[x][y] = true;
                }
            }
        }

        monsters.clear();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                Creature creature = map[x][y].creature;
                if (creature != null && creature != player) {
                    creature.x = x;
                    creature.y = y;
                    monsters.add(creature);
                }
            }
        }

        removeFish(selectedFish);
        fishThrowingPanel.clear();
        updateOverlay();
        if (overlayPanel.getStage() == null) {
            stage.addActor(overlayPanel);
        }
        updateMap();
        runTurn();
    }

    private void reactToElementChange(int x, int y, Element e) {
        if (map[x][y].terrain.element == null || map[x][y].terrain.element.combine(e) != map[x][y].terrain.element) {
            boolean wasLake = map[x][y].terrain.lake;
            Element combined = map[x][y].terrain.element == null ? e : map[x][y].terrain.element.combine(e);
            map[x][y].terrain = wasLake ? Terrain.makeElementalPool(combined) : Terrain.makeElementalFloor(combined, false);
        }

        Creature c = map[x][y].creature;
        if (c != null && c != player) {
            switch (c.element) {
                case ACID -> {
                    if (e == Element.SAND) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
                case AIR -> {
                    if (e == Element.MANA) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
                case BLOOD -> {
                    if (e == Element.TAR) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
                case MAGMA -> {
                    if (e == Element.WATER) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
                case MANA -> {
                    if (e == Element.BLOOD) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
                case SAND -> {
                    if (e == Element.ACID) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
                case TAR -> {
                    if (e == Element.MAGMA) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
                case WATER -> {
                    if (e == Element.AIR) {
                        printOut("The " + c.name + " has been destroyed!");
                        monsters.remove(c);
                        map[x][y].creature = null;
                    }
                }
            }
        }

        c = map[x][y].creature;
        if (c != null && c != player) {
            monsters.remove(c);
            c = new Creature(c.name, c.health, c.symbol, c.element.combine(e));
            map[x][y].creature = c;
            c.x = x;
            c.y = y;
            monsters.add(c);
        }

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
            if (e == Element.MANA && rng.nextDouble() < 0.05) {
                map[x][y].feature = TerrainFeature.TREE;
            } else if ((e == Element.WATER || e == Element.BLOOD) && rng.nextDouble() < 0.05) {
                map[x][y].feature = TerrainFeature.BUSH;
            } else if ((e == Element.TAR || e == Element.SAND) && rng.nextDouble() < 0.05) {
                map[x][y].feature = TerrainFeature.STONE_WALL;
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
            winPane = buildEndPane(
                    "You destroyed the elemental menace.",
                    new String[]{
                            "Your fame and prowess are now legendary across the land.",
                            "You live happily ever after."
                    },
                    SColor.ELECTRIC_GREEN
            );
        }
        if (winPane.getStage() == null) {
            stage.addActor(winPane);
        }
    }

    private void die(String reason) {
        canClick = false;
        if (diePane == null) {
            diePane = buildEndPane(
                    "Because you died.",
                    new String[]{
                            "It's lucky for you though, now you don't have to hear the screams",
                            "of your friends and loved ones as they are torn apart.",
                            "You died because:",
                            reason
                    },
                    SColor.SAFETY_ORANGE
            );
        }
        if (diePane.getStage() == null) {
            stage.addActor(diePane);
        }
    }

    private SparseLayers buildEndPane(String outcome, String[] body, Color accent) {
        SparseLayers pane = new SparseLayers(GRID_WIDTH, GRID_HEIGHT, CELL_WIDTH, CELL_HEIGHT,
                DefaultResources.getCrispDejaVuFont().width(CELL_WIDTH).height(CELL_HEIGHT).initBySize());
        pane.setPosition(0f, INVENTORY_ROWS * CELL_HEIGHT);
        float border = SColor.translucentColor(SColor.DARK_GRAY.toFloatBits(), 0.6f);
        float fill = SColor.translucentColor(border, 0.94f);
        for (int x = 0; x < pane.gridWidth(); x++) {
            pane.put(x, 0, border);
            pane.put(x, 1, border);
            pane.put(x, pane.gridHeight() - 1, border);
            pane.put(x, pane.gridHeight() - 2, border);
        }
        for (int y = 0; y < pane.gridHeight(); y++) {
            pane.put(0, y, border);
            pane.put(1, y, border);
            pane.put(pane.gridWidth() - 1, y, border);
            pane.put(pane.gridWidth() - 2, y, border);
        }
        for (int x = 2; x < pane.gridWidth() - 2; x++) {
            for (int y = 2; y < pane.gridHeight() - 2; y++) {
                pane.put(x, y, fill);
            }
        }

        int y = 3;
        String prompt = "-- press mouse button to quit --";
        pane.put((pane.gridWidth() - prompt.length()) / 2, y, prompt, SColor.ELECTRIC_GREEN);
        y += 2;
        pane.put(5, y++, "Your peaceful life as a fisherman has come to an end.", SColor.WHITE);
        pane.put(5, y + 1, outcome, accent);
        y += 3;
        for (String line : body) {
            pane.put(5, y++, line, SColor.WHITE);
        }
        pane.put((pane.gridWidth() - prompt.length()) / 2, GRID_HEIGHT - 3, prompt, SColor.ELECTRIC_GREEN);
        pane.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exiting();
            }
        });
        return pane;
    }

    private void addFish(Fish fish) {
        int n = fishInventory.get(fish.element).get(fish.size);
        if (n < MAX_FISH) {
            printOut("Caught a " + fish.name + "!");
            fishInventory.get(fish.element).put(fish.size, n + 1);
            updateFishInventoryPanel();
        } else {
            printOut("No more room for " + fish.name + '.');
        }
    }

    private void removeFish(Fish fish) {
        int n = fishInventory.get(fish.element).get(fish.size);
        if (n > 0) {
            selectedFish = null;
            fishInventory.get(fish.element).put(fish.size, n - 1);
            updateFishInventoryPanel();
        }
    }

    private boolean tryToMove(Direction dir) {
        int nextX = player.x + dir.deltaX;
        int nextY = player.y + dir.deltaY;
        if (nextX < 0 || nextY < 0 || nextX >= GRID_WIDTH || nextY >= GRID_HEIGHT) {
            return false;
        }

        MapCell tile = map[nextX][nextY];
        if (tile.isBlocking()) {
            if (tile.terrain.lake) {
                terrain = map[player.x][player.y].terrain;
                element = tile.terrain.element;
                goFish();
            } else {
                printOut("You can't walk through the " + (tile.feature != null && tile.feature.blocking ? tile.feature.name : tile.terrain.name) + '.');
            }
            return false;
        }

        Creature monster = tile.creature;
        if (monster == null) {
            map[player.x][player.y].creature = null;
            player.x = nextX;
            player.y = nextY;
            map[player.x][player.y].creature = player;
            return true;
        }
        if (monster != player) {
            printOut("You have no way to directly hurt the " + monster.name + '!');
            return false;
        }
        return true;
    }

    private void updateMap() {
        doFOV();
        mapPanel.clear();
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                map[x][y].light = SColor.WHITE;
                mapPanel.put(x, y, map[x][y].getSymbol().charAt(0), map[x][y].foregroundColor());
            }
        }
        mapPanel.put(player.x, player.y, player.symbol.charAt(0), player.color);
    }

    private void updateOverlay() {
        overlayPanel.clear();
        if (selectedFish == null || !mapMode || overlayLocation.x < 0 || overlayLocation.y < 0) {
            return;
        }
        int radius = switch (selectedFish.size) {
            case SMALL -> 2;
            case MEDIUM -> 3;
            case LARGE -> 5;
            case GIANT -> 9;
        };
        float cf = SColor.floatGet(selectedFish.color.r, selectedFish.color.g, selectedFish.color.b, OVERLAY_ALPHA / 255f);
        Radius strat = Radius.CIRCLE;
        for (int x = overlayLocation.x - radius; x <= overlayLocation.x + radius; x++) {
            for (int y = overlayLocation.y - radius; y <= overlayLocation.y + radius; y++) {
                if (x < 0 || y < 0 || x >= GRID_WIDTH || y >= GRID_HEIGHT) {
                    continue;
                }
                if (strat.radius(overlayLocation.x, overlayLocation.y, x, y) <= radius + 0.1) {
                    overlayPanel.put(x, y, cf);
                }
            }
        }
    }

    private void printOut(String message) {
        outputPanel.clear();
        if (message.length() > GRID_WIDTH - 2) {
            message = message.substring(0, GRID_WIDTH - 5) + "...";
        }
        outputPanel.put(1, GRID_HEIGHT - 2, message, SColor.TEA_GREEN);
        outputEndTime = System.currentTimeMillis() + OUTPUT_DURATION_MS;
        if (nowFishing) {
            requestFishingRecordingFrame();
        }
    }

    private void doFOV() {
        double[][] walls = new double[GRID_WIDTH][GRID_HEIGHT];
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                walls[x][y] = map[x][y].isOpaque() ? 1.0 : 0.0;
            }
        }
        double[][] visible = fov.calculateFOV(walls, player.x, player.y, Math.min(GRID_WIDTH, GRID_HEIGHT) / 3.0);
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
                map[x][y].seen = visible[x][y] > 0.0;
            }
        }
    }

    private void createMap() {
        monsters.clear();
        map = new MapCell[GRID_WIDTH][GRID_HEIGHT];
        for (int x = 0; x < GRID_WIDTH; x++) {
            for (int y = 0; y < GRID_HEIGHT; y++) {
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
            placeWallChunk(Terrain.GRASS, null);
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

        int startX = (GRID_WIDTH / 2) - 2;
        int startY = (GRID_HEIGHT / 2) - 4;
        int y = startY;
        for (String row : mapDrawing) {
            int x = startX;
            for (char c : row.toCharArray()) {
                map[x][y] = switch (c) {
                    case '#' -> new MapCell(Terrain.STONE, TerrainFeature.STONE_WALL);
                    default -> new MapCell(Terrain.STONE);
                };
                x++;
            }
            y++;
        }

        for (int i = 0; i < 20; i++) {
            Creature creature = Creature.getRandomMonster();
            placeMonster(creature);
            monsters.add(creature);
        }

        player.x = GRID_WIDTH / 2;
        player.y = GRID_HEIGHT / 2;
        MapCell cell = map[player.x][player.y];
        cell.creature = player;
        cell.terrain = Terrain.DIRT;
        cell.feature = null;
        cell.item = null;
    }

    private void placeWallChunk(Terrain t, TerrainFeature tf) {
        int spread = 5;
        int centerX = rng.nextInt(GRID_WIDTH);
        int centerY = rng.nextInt(GRID_HEIGHT);
        for (int placeX = centerX - spread; placeX < centerX + spread; placeX++) {
            for (int placeY = centerY - spread; placeY < centerY + spread; placeY++) {
                if (rng.nextDouble() < 0.2 && placeX > 0 && placeX < GRID_WIDTH - 1 && placeY > 0 && placeY < GRID_HEIGHT - 1) {
                    map[placeX][placeY] = new MapCell(t, tf);
                }
            }
        }
    }

    private void placeMonster(Creature monster) {
        int x;
        int y;
        do {
            x = rng.nextInt(GRID_WIDTH - 2) + 1;
            y = rng.nextInt(GRID_HEIGHT - 2) + 1;
        } while (map[x][y].isBlocking() || map[x][y].creature != null || (x > GRID_WIDTH * 0.3 && x < GRID_WIDTH * 0.6 && y > GRID_HEIGHT * 0.3 && y < GRID_HEIGHT * 0.6));
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

    private void moveMonster(Creature monster) {
        Coord point = getClosestWaypoint(Coord.get(monster.x, monster.y), Coord.get(player.x, player.y));
        Direction dir = rng.getRandomElement(Direction.OUTWARDS);
        if (point != null) {
            dir = Direction.getDirection(point.x - monster.x, point.y - monster.y);
            if (map[point.x][point.y].isBlocking()) {
                dir = rng.getRandomElement(Direction.OUTWARDS);
            }
        }
        int nextX = monster.x + dir.deltaX;
        int nextY = monster.y + dir.deltaY;
        if (nextX < 0 || nextX >= GRID_WIDTH || nextY < 0 || nextY >= GRID_HEIGHT) {
            return;
        }

        MapCell tile = map[nextX][nextY];
        if (!tile.isBlocking() && tile.creature == null) {
            map[monster.x][monster.y].creature = null;
            monster.x = nextX;
            monster.y = nextY;
            map[monster.x][monster.y].creature = monster;
        } else if (tile.creature == player) {
            hurtPlayer(monster.name);
        }

        if (nowFishing) {
            for (Direction d : Direction.CARDINALS) {
                int x = d.deltaX + monster.x;
                int y = d.deltaY + monster.y;
                if (x >= 0 && x < GRID_WIDTH && y >= 0 && y < GRID_HEIGHT && x == player.x && y == player.y) {
                    printOut("A monster is next to you! Right-click to stop fishing.");
                }
            }
        }
    }

    private void hurtPlayer(String cause) {
        player.health--;
        printOut("The " + cause + " hurt you! You have " + player.health + " health now.");
        if (player.health <= 0) {
            die("The " + cause + " finished you off.");
        }
        updateFishInventoryPanel();
    }

    private void checkForReactions() {
    }

    private void moveAllMonsters() {
        ArrayList<Creature> turnOrder = new ArrayList<>(monsters);
        for (Creature monster : turnOrder) {
            if (monsters.contains(monster)) {
                moveMonster(monster);
            }
        }
    }

    private void initFishingDisplay() {
        fishingLayers.clear();
        fishingPlayerPanel.clear();
        for (int x = 0; x < FISH_WIDTH; x++) {
            for (int y = 0; y < FISH_HEIGHT; y++) {
                Color color = terrainMap[x][y] ? getTerrainColor(x, y) : liquidMap[x][y] ? getLiquidColor(x, y) : getSkyColor(x, y);
                fishingLayers.put(x, y, color.toFloatBits());
            }
        }
        for (Fish fish : fishes) {
            fishingLayers.put(fish.x, fish.y, fish.symbol.charAt(0), fish.color);
        }
        fishingPlayerPanel.put(1, 2, '@', playerColor);
        if (castAnimationPhase == CastAnimationPhase.ARC && castArcPath != null && castArcIndex > 0) {
            Coord prev = castStartLocation;
            for (int i = 0; i <= castArcIndex; i++) {
                Coord point = castArcPath.get(i);
                fishingLayers.put(point.x, point.y, lineGlyph(point.x - prev.x, point.y - prev.y), lineColor);
                prev = point;
            }
            if (castBobberLocation != null) {
                fishingLayers.put(castBobberLocation.x, castBobberLocation.y, bobber, bobberColor);
            }
        }
        if (castPreviewLocation != null && bobberLocation == null && castAnimationPhase == CastAnimationPhase.NONE && !casting) {
            drawFishingLineTo(castPreviewLocation, bobberColor, -1);
        }
        if (bobberLocation != null && castAnimationPhase != CastAnimationPhase.ARC) {
            int lineBottomY = -1;
            if (castAnimationPhase == CastAnimationPhase.DROP
                    || castAnimationPhase == CastAnimationPhase.BOTTOM_PAUSE
                    || castAnimationPhase == CastAnimationPhase.REEL) {
                lineBottomY = castHookY;
            }
            drawFishingLineTo(bobberLocation, bobberColor, lineBottomY);
            int hookY = castAnimationPhase == CastAnimationPhase.NONE ? Math.max(0, bobberLocation.y - 1) : castHookY;
            if (hookY >= 0 && hookY < FISH_HEIGHT) {
                fishingLayers.put(bobberLocation.x, hookY, hook, hookColor);
            }
            if (hookedFish != null && hookY >= 0 && hookY < FISH_HEIGHT) {
                fishingLayers.put(bobberLocation.x, hookY, hookedFish.symbol.charAt(0), hookedFish.color);
            }
        }
        requestFishingRecordingFrame();
    }

    private void drawFishingLineTo(Coord target, Color tipColor, int lineBottomOverrideY) {
        if (target == null) {
            return;
        }
        int targetX = Math.max(0, Math.min(FISH_WIDTH - 1, target.x));
        int targetY = Math.max(0, Math.min(FISH_HEIGHT - 1, target.y));
        Coord castStart = Coord.get(TERRAIN_WIDTH - 2, LARGE_TEXT_SCALE * 2 + 1);
        Coord[] path = Bresenham.line2D_(castStart, Coord.get(targetX, targetY));
        Coord prev = castStart;
        for (Coord point : path) {
            if (point.x >= 0 && point.x < FISH_WIDTH && point.y >= 0 && point.y < FISH_HEIGHT) {
                fishingLayers.put(point.x, point.y, lineGlyph(point.x - prev.x, point.y - prev.y), lineColor);
            }
            prev = point;
        }
        fishingLayers.put(targetX, targetY, bobber, tipColor);
        int lineBottom = lineBottomOverrideY >= 0 ? lineBottomOverrideY : bed(targetX);
        lineBottom = Math.max(targetY + 1, Math.min(FISH_HEIGHT - 1, lineBottom));
        for (int y = targetY + 1; y <= lineBottom; y++) {
            fishingLayers.put(targetX, y, '|', lineColor);
        }
    }

    private char lineGlyph(int dx, int dy) {
        if (dx == 0) {
            return '|';
        }
        if (dy == 0) {
            return '─';
        }
        return (dx < 0 && dy < 0) || (dx > 0 && dy > 0) ? '╲' : '╱';
    }

    private int clampedCastTargetX(double castStrength) {
        int minX = TERRAIN_WIDTH * 2 + 1;
        int maxX = FISH_WIDTH - 1;
        int target = (int) (castStrength * (maxX - minX) + minX);
        return Math.max(minX, Math.min(maxX, target));
    }

    private Color getTerrainColor(int x, int y) {
        float mix = (float) (0.5 + 0.5 * PerlinNoise.noise(y, x));
        return colorCenter.lerp(terrain.color, colorCenter.dim(terrain.color), mix);
    }

    private Color getLiquidColor(int x, int y) {
        float wave = (float) (0.5 + 0.5 * PerlinNoise.noise(x, y));
        Color base = colorCenter.lerp(element.color, colorCenter.dim(element.color), wave);
        return colorCenter.lerp(base, colorCenter.dimmest(element.color), y / (float) Math.max(1, FISH_HEIGHT - LIQUID_HEIGHT));
    }

    private Color getSkyColor(int x, int y) {
        return colorCenter.lerp(colorCenter.lightest(skyColor), colorCenter.dim(skyColor), y / (float) Math.max(1, LIQUID_HEIGHT));
    }

    private void initFish() {
        fishMap = new Fish[FISH_WIDTH][FISH_HEIGHT];
        bobberLocation = null;
        castPreviewLocation = null;
        for (Fish fish : fishes) {
            boolean placed = false;
            while (!placed) {
                int x = rng.between(TERRAIN_WIDTH * 2 + 1, FISH_WIDTH);
                if (bed(x) > LIQUID_HEIGHT + 1) {
                    int y = rng.between(LIQUID_HEIGHT + 1, bed(x));
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
        terrainMap = new boolean[FISH_WIDTH][FISH_HEIGHT];
        liquidMap = new boolean[FISH_WIDTH][FISH_HEIGHT];

        for (int x = 0; x < TERRAIN_WIDTH; x++) {
            for (int y = LIQUID_HEIGHT - LARGE_TEXT_SCALE - 1; y < FISH_HEIGHT; y++) {
                terrainMap[x][y] = true;
            }
        }

        int lastHeight = LIQUID_HEIGHT - LARGE_TEXT_SCALE + 1;
        int nextHeight = LIQUID_HEIGHT + 1;
        for (int x = TERRAIN_WIDTH; x < TERRAIN_WIDTH * 2; x++) {
            int offset = rng.between(-1, 2);
            offset *= Math.signum(nextHeight - lastHeight);
            int terrainHeight = lastHeight + offset;
            terrainHeight = Math.min(terrainHeight, Math.max(lastHeight, nextHeight));
            terrainHeight = Math.max(terrainHeight, Math.min(lastHeight, nextHeight));
            lastHeight = terrainHeight;
            if (lastHeight == nextHeight) {
                nextHeight = rng.between(LIQUID_HEIGHT + 4, FISH_HEIGHT - 1);
            }
            for (int y = LIQUID_HEIGHT; y < terrainHeight; y++) {
                liquidMap[x][y] = true;
            }
            for (int y = terrainHeight; y < FISH_HEIGHT; y++) {
                terrainMap[x][y] = true;
            }
        }

        lastHeight = LIQUID_HEIGHT + 2;
        nextHeight = rng.between(LIQUID_HEIGHT + 8, FISH_HEIGHT - 1);
        for (int x = TERRAIN_WIDTH * 2; x < FISH_WIDTH; x++) {
            int offset = rng.between(-1, 3);
            offset *= Math.signum(nextHeight - lastHeight);
            int terrainHeight = lastHeight + offset;
            terrainHeight = Math.min(terrainHeight, FISH_HEIGHT - 1);
            terrainHeight = Math.max(terrainHeight, LIQUID_HEIGHT + 12);
            lastHeight = terrainHeight;
            if (lastHeight == nextHeight) {
                nextHeight = rng.between(LIQUID_HEIGHT + 14, FISH_HEIGHT - 1);
            }
            for (int y = LIQUID_HEIGHT; y < terrainHeight; y++) {
                liquidMap[x][y] = true;
            }
            for (int y = terrainHeight; y < FISH_HEIGHT; y++) {
                terrainMap[x][y] = true;
            }
        }
    }

    private int bed(int x) {
        x = Math.max(0, Math.min(FISH_WIDTH - 1, x));
        for (int y = 1; y < FISH_HEIGHT; y++) {
            if (terrainMap[x][y]) {
                return y - 1;
            }
        }
        return FISH_HEIGHT - 1;
    }

    private void takeScreenshot() {
        try {
            Path screenshotDir = Path.of("screenshots");
            Files.createDirectories(screenshotDir);
            String fileName = "assaultfish-" + LocalDateTime.now().format(SCREENSHOT_TIME) + ".png";
            Path target = screenshotDir.resolve(fileName);
            Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            Pixmap flipped = flipPixmapVertically(pixmap);
            try {
                PixmapIO.writePNG(Gdx.files.absolute(target.toAbsolutePath().toString()), flipped);
            } finally {
                flipped.dispose();
                pixmap.dispose();
            }
            printOut("Saved screenshot to " + target.toString());
        } catch (IOException ex) {
            printOut("Failed to save screenshot: " + ex.getMessage());
        }
    }

    private void toggleRecording() {
        if (recordingActive) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        try {
            recordingStamp = LocalDateTime.now().format(RECORDING_TIME);
            recordingFramesDir = Path.of("screenshots", "recording-" + recordingStamp + "-frames");
            Files.createDirectories(recordingFramesDir);
            recordingActive = true;
            resetRecordingCaptureState();
            printOut("Recording started. Press V again to save a GIF.");
        } catch (IOException ex) {
            recordingActive = false;
            recordingFramesDir = null;
            recordingStamp = null;
            resetRecordingCaptureState();
            printOut("Unable to start recording: " + ex.getMessage());
        }
    }

    private void stopRecording() {
        if (!recordingActive) {
            return;
        }

        flushRecordingCapture();
        recordingActive = false;
        try {
            if (recordingFramesDir == null || recordingFrameIndex == 0) {
                printOut("Recording stopped (no frames captured).");
                return;
            }

            List<Path> frameFiles = listRecordingFrames(recordingFramesDir);
            if (frameFiles.isEmpty()) {
                printOut("Recording stopped (no frames captured).");
                return;
            }

            Path screenshotDir = Path.of("screenshots");
            Files.createDirectories(screenshotDir);
            Path target = screenshotDir.resolve("assaultfish-recording-" + recordingStamp + ".gif");
            writeAnimatedGif(frameFiles, target, RECORDING_GIF_FRAME_DELAY_MS, true);
            printOut("Saved recording to " + target.toAbsolutePath());
        } catch (IOException ex) {
            printOut("Failed to save recording: " + ex.getMessage());
        } finally {
            cleanupRecordingTemp();
        }
    }

    private void finalizeRecordingBeforeExit() {
        if (recordingActive) {
            stopRecording();
        }
    }

    private void requestMapTurnRecordingFrame() {
        if (!recordingActive || !mapMode) {
            return;
        }

        requestRecordingFrame(false);
    }

    private void requestFishingRecordingFrame() {
        if (!recordingActive || !nowFishing) {
            return;
        }

        requestRecordingFrame(true);
    }

    private void requestRecordingFrame(boolean onlyIfChanged) {
        if (!recordingCaptureRequested) {
            recordingCaptureRequested = true;
            recordingCaptureOnlyIfChanged = onlyIfChanged;
            return;
        }

        recordingCaptureOnlyIfChanged &= onlyIfChanged;
    }

    private void flushRecordingCapture() {
        if (!recordingActive || !recordingCaptureRequested || recordingFramesDir == null) {
            return;
        }

        boolean onlyIfChanged = recordingCaptureOnlyIfChanged;
        recordingCaptureRequested = false;
        recordingCaptureOnlyIfChanged = false;

        try {
            Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            Pixmap flipped = flipPixmapVertically(pixmap);
            try {
                int frameHash = hashPixmap(flipped);
                if (onlyIfChanged && recordingHasFrameHash && recordingLastFrameHash == frameHash) {
                    return;
                }
                writeRecordingFrame(flipped);
                recordingLastFrameHash = frameHash;
                recordingHasFrameHash = true;
            } finally {
                flipped.dispose();
                pixmap.dispose();
            }
        } catch (IOException ex) {
            recordingActive = false;
            printOut("Recording failed: " + ex.getMessage());
            cleanupRecordingTemp();
        }
    }

    private void cleanupRecordingTemp() {
        if (recordingFramesDir != null) {
            deleteDirectory(recordingFramesDir.toFile());
        }
        recordingFramesDir = null;
        recordingStamp = null;
        resetRecordingCaptureState();
    }

    private void resetRecordingCaptureState() {
        recordingFrameIndex = 0;
        recordingCaptureRequested = false;
        recordingCaptureOnlyIfChanged = false;
        recordingHasFrameHash = false;
        recordingLastFrameHash = 0;
    }

    private void writeRecordingFrame(Pixmap frame) throws IOException {
        Files.createDirectories(recordingFramesDir);
        recordingFrameIndex++;
        Path frameFile = recordingFramesDir.resolve(String.format("frame-%06d.png", recordingFrameIndex));
        PixmapIO.writePNG(Gdx.files.absolute(frameFile.toAbsolutePath().toString()), frame);
    }

    private static int hashPixmap(Pixmap pixmap) {
        ByteBuffer pixels = pixmap.getPixels();
        int hash = 1;
        for (int i = 0, limit = pixels.limit(); i < limit; i++) {
            hash = 31 * hash + pixels.get(i);
        }
        return hash;
    }

    private static List<Path> listRecordingFrames(Path frameDir) throws IOException {
        List<Path> files = new ArrayList<>();
        if (frameDir == null || !Files.exists(frameDir) || !Files.isDirectory(frameDir)) {
            return files;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(frameDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".png"))
                    .sorted()
                    .forEach(files::add);
        }
        return files;
    }

    private static void writeAnimatedGif(List<Path> frameFiles, Path outputFile, int delayMs, boolean loopForever)
            throws IOException {
        if (frameFiles.isEmpty()) {
            return;
        }

        BufferedImage first = ImageIO.read(frameFiles.get(0).toFile());
        if (first == null) {
            throw new IOException("Unable to read first recording frame");
        }
        int imageType = first.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : first.getType();

        try (ImageOutputStream outputStream = new FileImageOutputStream(outputFile.toFile());
                GifSequenceWriter gifWriter = new GifSequenceWriter(outputStream, imageType, delayMs, loopForever)) {
            gifWriter.writeToSequence(first);
            for (int i = 1; i < frameFiles.size(); i++) {
                BufferedImage frame = ImageIO.read(frameFiles.get(i).toFile());
                if (frame != null) {
                    gifWriter.writeToSequence(frame);
                }
            }
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) {
            return;
        }
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteDirectory(child);
            }
        }
        dir.delete();
    }

    private static final class GifSequenceWriter implements AutoCloseable {
        private final ImageWriter gifWriter;
        private final ImageWriteParam imageWriteParam;
        private final IIOMetadata imageMetaData;

        private GifSequenceWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesMs,
                boolean loopContinuously) throws IOException {
            gifWriter = getWriter();
            imageWriteParam = gifWriter.getDefaultWriteParam();
            ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

            imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

            String metaFormatName = imageMetaData.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) imageMetaData.getAsTree(metaFormatName);

            IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");
            graphicsControlExtensionNode.setAttribute("disposalMethod", "none");
            graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("transparentColorFlag", "FALSE");
            graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(Math.max(1, timeBetweenFramesMs / 10)));
            graphicsControlExtensionNode.setAttribute("transparentColorIndex", "0");

            IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
            commentsNode.setAttribute("CommentExtension", "Created by AssaultFish");

            IIOMetadataNode applicationExtensionsNode = getNode(root, "ApplicationExtensions");
            IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");
            child.setAttribute("applicationID", "NETSCAPE");
            child.setAttribute("authenticationCode", "2.0");
            int loop = loopContinuously ? 0 : 1;
            child.setUserObject(new byte[] {
                    0x1,
                    (byte) (loop & 0xFF),
                    (byte) ((loop >> 8) & 0xFF)
            });
            applicationExtensionsNode.appendChild(child);

            imageMetaData.setFromTree(metaFormatName, root);

            gifWriter.setOutput(outputStream);
            gifWriter.prepareWriteSequence(null);
        }

        private void writeToSequence(BufferedImage img) throws IOException {
            gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
        }

        @Override
        public void close() throws IOException {
            gifWriter.endWriteSequence();
        }

        private static ImageWriter getWriter() throws IOException {
            Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
            if (!iter.hasNext()) {
                throw new IOException("No GIF Image Writers Exist");
            }
            return iter.next();
        }

        private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName) {
            int nNodes = rootNode.getLength();
            for (int i = 0; i < nNodes; i++) {
                if (rootNode.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                    return (IIOMetadataNode) rootNode.item(i);
                }
            }
            IIOMetadataNode node = new IIOMetadataNode(nodeName);
            rootNode.appendChild(node);
            return node;
        }
    }

    private Pixmap flipPixmapVertically(Pixmap source) {
        Pixmap flipped = new Pixmap(source.getWidth(), source.getHeight(), source.getFormat());
        int width = source.getWidth();
        int height = source.getHeight();
        for (int y = 0; y < height; y++) {
            int sourceY = height - 1 - y;
            for (int x = 0; x < width; x++) {
                flipped.drawPixel(x, y, source.getPixel(x, sourceY));
            }
        }
        return flipped;
    }
}
