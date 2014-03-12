package assaultfish.fishing;

import assaultfish.physical.Element;
import assaultfish.physical.Size;
import assaultfish.physical.Terrain;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidgrid.gui.awt.TextCellFactory;
import squidpony.squidgrid.gui.awt.event.SGKeyListener;
import squidpony.squidgrid.gui.awt.event.SGMouseListener;
import squidpony.squidgrid.gui.swing.SwingPane;
import squidpony.squidgrid.util.Direction;
import static squidpony.squidgrid.util.Direction.*;
import squidpony.squidmath.PerlinNoise;
import squidpony.squidmath.RNG;

/**
 * Prototype of the fishing simulator portion of the game.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class FishingSim {

    private JComponent parent;
    private SGKeyListener keys;
    private SGMouseListener mouses;
    private SwingPane pane, largeTextPane, meterPane, fishPane;
    private int largeTextScale;
    private boolean[][] terrainMap;
    private boolean[][] liquidMap;
    private Fish[][] fishMap;
    private List<Fish> fishes = new LinkedList<>();
    private int width, height;
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
    private Font font;
    private RNG rng = new RNG();

    private int liquidHeight;
    private int terrainWidth;

    static {
        if (SColorFactory.pallet("meter") == null) {
            ArrayList<SColor> pallet = SColorFactory.asGradient(SColor.RED, SColor.ORANGE);
            pallet.addAll(SColorFactory.asGradient(SColor.ORANGE, SColor.YELLOW));
            pallet.addAll(SColorFactory.asGradient(SColor.YELLOW, SColor.ELECTRIC_GREEN));
            SColorFactory.addPallet("meter", pallet);
        }
    }

    /**
     * Starts up a stand-alone instance for testing.
     *
     * @param args
     */
    public static void main(String... args) {

        JFrame frame = new JFrame("Fishing Prototype");
        frame.setBackground(SColor.BLACK);
        frame.getContentPane().setBackground(SColor.BLACK);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setBackground(SColor.BLACK);
        panel.setPreferredSize(new Dimension(1800, 900));
        frame.getContentPane().add(panel);
        SGKeyListener listen = new SGKeyListener(false, SGKeyListener.CaptureType.TYPED);
        frame.addKeyListener(listen);

//        Element e = Element.getRandomElement();
        Element e = Element.MAGMA;
        Font font = new Font("Arial Unicode MS", Font.PLAIN, 14);
        Fish.initSymbols(font);
        List<Fish> fishes = new LinkedList<>();
        for (int i = 0; i < 100; i++) {
            Fish fish = new Fish(Size.getRandomSize(), e);
            fishes.add(fish);
        }

        FishingSim sim = new FishingSim(panel, listen, 100, 60, fishes, Terrain.STONE, e, font);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        ArrayList<Fish> creel = sim.goFish();

        System.out.println("Fish Caught");
        for (Fish f : creel) {
            System.out.println(f.name);
        }
    }

    /**
     * Builds the game into the passed in component, which should be an empty component with the
     * desired preferredSize preset. The size of the grid should be evenly divisible by 3 and 4 for
     * optimal display. The key listener is expected to be non-blocking as this sim is event driven
     * rather than modal.
     *
     * @param parent
     * @param keys
     * @param gridWidth how wide in grid cells the sim should be
     * @param gridHeight how high in grid cells the sim should be, includes the 3-cell-high meter
     * @param fishes
     * @param terrain
     * @param element
     */
    public FishingSim(JComponent parent, SGKeyListener keys, int gridWidth, int gridHeight, List<Fish> fishes, Terrain terrain, Element element, Font font) {
        this.parent = parent;
        this.keys = keys;
        this.fishes = fishes;
        this.terrain = terrain;
        this.element = element;
        this.font = font;

        width = gridWidth;
        height = gridHeight;
        largeTextScale = 4;
        liquidHeight = largeTextScale * 4;
        terrainWidth = largeTextScale * 2 + 1;

        initFrame();
        initMap();
        initFish();
        initMeter();
        displayMap();
    }

    /**
     * Starts the fishing sim and returns a list of all fish caught.
     *
     * @return
     */
    public ArrayList<Fish> goFish() {
        ArrayList<Fish> caught = new ArrayList<>();
        char key = '∅';
        do {
            if (keys.hasNext()) {
                key = keys.next().getKeyChar();
                switch (key) {
                    case ' ':
                        displayMap();
                        throwBobber();
                        Fish f = dropHook();
                        if (f != null) {
                            caught.add(f);
                        }
                        keys.flush();
                        break;
                    case 'x':
                        displayMap();
                        break;
                    case 'r':
                        initMap();
                        initFish();
                        initMeter();
                        displayMap();
                        break;
                }
            }
            Thread.yield();
        } while (key != 'Q');

        return caught;
    }

    private Fish dropHook() {
        pane.placeCharacter(bobberLocation.x, bobberLocation.y + 1, hook, hookColor);
        pane.refresh();
        int x = bobberLocation.x;
        int y;
        for (y = bobberLocation.y + 2; y <= bed(x); y++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
            }
            pane.placeCharacter(x, y - 1, line(UP), lineColor);
            pane.placeCharacter(x, y, hook, hookColor);
            pane.refresh();
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }

        Fish fish = null;
        do {
            pane.clearCell(x, y);

            y--;
            pane.placeCharacter(x, y, hook, hookColor);
            if (fish != null) {
                pane.placeCharacter(x, y, fish.symbol.charAt(0), fish.color);
            } else if (fishMap[x][y] != null) {
                fish = fishMap[x][y];
                fishes.remove(fish);
                fishMap[x][y] = null;
                fishPane.clearCell(x, y);
                fishPane.refresh();
            }
            pane.refresh();

            try {
                Thread.sleep(40);
            } catch (InterruptedException ex) {
            }
        } while (y > bobberLocation.y + 1);

        return fish;
    }

    private void throwBobber() {
        double strength = getStrength();
        int targetX = (int) (strength * (width - 1 - terrainWidth * 2) + terrainWidth * 2 + 1);//finds drop target based on strength percent
        int startY = largeTextScale * 2 + 1;//start at the guy's head
        int startX = terrainWidth;//start at the shoreline

        BallisticsSolver solver = new BallisticsSolver(startX, startY, targetX - 1, liquidHeight - 2, wind, gravity);
        int solveHeight = width / (targetX + 5);
        solveHeight = Integer.min(solveHeight, largeTextScale * 2 - 1);
        solveHeight = Integer.max(solveHeight, 1);
        System.out.println("" + solveHeight);
        solver.solveByHeight(solveHeight);

        int lastX = -1, lastY = -1, bobberX = -2, bobberY = -2;
        double trueTime = solver.getTime();
        int targetTime = targetX * 15;//in milliseconds
        long lastTime;
        for (double time = 0; time <= targetTime; time += System.currentTimeMillis() - lastTime) {
            lastTime = System.currentTimeMillis();
            double solverTime = trueTime * time / targetTime;
            bobberX = solver.x(solverTime);
            bobberY = solver.y(solverTime);
            if (lastX != bobberX) {
                pane.placeCharacter(lastX, lastY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
                pane.placeCharacter(bobberX, bobberY, bobber, bobberColor);
                pane.refresh();
                lastX = bobberX;
                lastY = bobberY;
            }

            Thread.yield();
        }

        pane.placeCharacter(bobberX, bobberY, line(getDirection(bobberX - lastX, bobberY - lastY)), lineColor);
        pane.placeCharacter(bobberX + 1, bobberY + 1, bobber, bobberColor);
        bobberLocation = new Point(bobberX + 1, bobberY + 1);
        pane.refresh();

        for (int x = 1; x < meterPane.getGridWidth() - 1; x++) {
            meterPane.placeCharacter(x, 1, ' ');
        }
        meterPane.refresh();
    }

    /**
     * Returns the percentage of max strength;
     *
     * @return
     */
    private double getStrength() {
        char key = 'x';

        double strength;
        double timeStep = 1000;//how many milliseconds per time step
        long time = (long) (1000 * Math.PI / 2.0);
        int meterOffset = 3;
        int meterSize = width - meterOffset * 2;

        long lastTime = System.currentTimeMillis();
        do {
            strength = 1 - Math.abs(Math.sin(time / timeStep));
            int drawX = (int) (strength * meterSize);
            drawX = Math.min(drawX, meterSize);//make sure rare case of strength 1 doesn't cause problems
            for (int i = 0; i < meterSize; i++) {
                if (i < drawX) {
                    meterPane.placeCharacter(i + meterOffset, 1, '●', SColorFactory.fromPallet("meter", i / (float) (meterSize)));
                } else {
                    meterPane.clearCell(i + meterOffset, 1);
                }
            }
            meterPane.refresh();

            Thread.yield();
            if (keys.hasNext()) {
                key = keys.next().getKeyChar();
            }

            time += System.currentTimeMillis() - lastTime;
            lastTime = System.currentTimeMillis();
        } while (key != ' ');

        return strength;
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
        for (int y = 1; y < height; y++) {
            if (terrainMap[x][y]) {
                return y - 1;
            }
        }
        return height - 1;
    }

    private void displayMap() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (terrainMap[x][y]) {
                    pane.clearCell(x, y, getTerrainColor(x, y));
                } else if (liquidMap[x][y]) {
                    pane.clearCell(x, y, getLiquidColor(x, y));
                } else {
                    pane.clearCell(x, y, getSkyColor(x, y));
                }
            }
        }

        for (Fish f : fishes) {
            fishPane.placeCharacter(f.x, f.y, f.symbol.charAt(0), f.color);
        }

        fishPane.refresh();
        pane.refresh();
        largeTextPane.refresh();
    }

    private SColor getTerrainColor(int x, int y) {
        return SColorFactory.blend(terrain.color, SColorFactory.dim(terrain.color), PerlinNoise.noise(y, x));
    }

    private SColor getLiquidColor(int x, int y) {
        return SColorFactory.blend(SColorFactory.blend(element.color, SColorFactory.dim(element.color), PerlinNoise.noise(x, y)), SColorFactory.dimmest(element.color), y / (double) (height - liquidHeight));
    }

    private SColor getSkyColor(int x, int y) {
        return SColorFactory.blend(SColorFactory.lightest(skyColor), SColorFactory.dim(skyColor), y / (double) liquidHeight);
    }

    private void initFish() {
        fishMap = new Fish[width][height];

        for (Fish fish : fishes) {
            boolean placed = false;
            while (!placed) {
                int x = rng.between(terrainWidth * 2 + 1, width);
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

    private void initMeter() {
        meterPane.placeHorizontalString((meterPane.getGridWidth() - "Cast Strength".length() - 1) / 2, 0, "Cast Strength");
        meterPane.placeHorizontalString(2, 0, "None");
        meterPane.placeHorizontalString(meterPane.getGridWidth() - 3 - "Max".length(), 0, "Max");
        meterPane.refresh();
    }

    private void initMap() {
        terrainMap = new boolean[width][height];
        liquidMap = new boolean[width][height];

        //fill in standing edge
        for (int x = 0; x < terrainWidth; x++) {
            for (int y = liquidHeight - largeTextScale; y < height; y++) {
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
                nextHeight = rng.between(liquidHeight + 4, height - 1);
            }
            for (int y = liquidHeight; y < terrainHeight; y++) {
                liquidMap[x][y] = true;
            }
            for (int y = terrainHeight; y < height; y++) {
                terrainMap[x][y] = true;
            }
        }

        //fill in rest of terrain and liquid
        lastHeight = liquidHeight + 2;
        nextHeight = rng.between(liquidHeight + 4, height - 1);
        for (int x = terrainWidth * 2; x < width; x++) {
            int offset = rng.between(-1, 2);
            offset *= Math.signum(nextHeight - lastHeight);
            int terrainHeight = lastHeight + offset;
            terrainHeight = Math.min(terrainHeight, Math.max(lastHeight, nextHeight));
            terrainHeight = Math.max(terrainHeight, Math.min(lastHeight, nextHeight));
            lastHeight = terrainHeight;
            if (lastHeight == nextHeight) {
                nextHeight = rng.between(liquidHeight + 4, height - 1);
            }
            for (int y = liquidHeight; y < terrainHeight; y++) {
                liquidMap[x][y] = true;
            }
            for (int y = terrainHeight; y < height; y++) {
                terrainMap[x][y] = true;
            }
        }

        //place player
        largeTextPane.placeCharacter(1, 2, '@', playerColor);
    }

    private void initFrame() {
        int cellWidth = parent.getPreferredSize().width / width;
        int cellHeight = parent.getPreferredSize().height / height;
        font = new Font(font.getFontName(), Font.BOLD, cellHeight + cellWidth);

        Fish.initSymbols(font);

        JLayeredPane layers = new JLayeredPane();
        parent.setLayout(new BorderLayout());
        parent.add(layers, BorderLayout.WEST);

        TextCellFactory textFactory = new TextCellFactory();
        textFactory.setAntialias(true);
        textFactory.initializeBySize(cellWidth, cellHeight, font);
        pane = new SwingPane(width, height, textFactory);
        layers.setLayer(pane, JLayeredPane.DEFAULT_LAYER);
        layers.add(pane);

        TextCellFactory fishTextFactory = new TextCellFactory(textFactory);
        fishTextFactory.initializeBySize(pane.getCellWidth(), pane.getCellHeight(), new Font(font.getFontName(), Font.PLAIN, font.getSize() + 2));//a little extra size in case the switch away from bold matters
        fishPane = new SwingPane(width, height, fishTextFactory);
        layers.setLayer(pane, JLayeredPane.DEFAULT_LAYER - 10);//set just above the regular map layer
        layers.add(fishPane);

        meterPane = new SwingPane(width, 3, textFactory);
        meterPane.setDefaultBackground(SColor.BLACK);
        parent.add(meterPane, BorderLayout.SOUTH);

        TextCellFactory largeFactory = new TextCellFactory();
        largeFactory.setAntialias(true);
        largeFactory.initializeBySize(pane.getCellWidth() * largeTextScale, pane.getCellHeight() * largeTextScale, new Font(font.getFontName(), Font.BOLD, 92));
        largeTextPane = new SwingPane(width / largeTextScale, height / largeTextScale, largeFactory);
        layers.setLayer(largeTextPane, JLayeredPane.MODAL_LAYER);
        layers.add(largeTextPane);

        layers.setPreferredSize(pane.getPreferredSize());
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

}
