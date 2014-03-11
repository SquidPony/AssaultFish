package assaultfish.fishing;

import assaultfish.physical.Terrain;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidgrid.gui.awt.TextCellFactory;
import squidpony.squidgrid.gui.awt.event.SGKeyListener;
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

    private JFrame frame;
    private SwingPane pane, largeTextPane, meterPane, fishPane;
    private int largeTextScale;
    private boolean[][] terrainMap;
    private boolean[][] liquidMap;
    private Fish[][] fishMap;
    private List<Fish> fishes = new LinkedList<>();
    private int width, height;
    private double wind = 10, gravity = 20;
    private Terrain terrain;
    private char bobber = '●',//O•☉✆✇♁┢Ø∅∮⊕⊖⊗⊘⊙⊚⊛⊜⊝Ⓧ◍◎●◐◑◒◓◔◕☯☮☻☺☹✪➊➋➌➍➎➏➐➑➒➓〄〇〶
            hook = 'J',
            wall = '#';
    private Point bobberLocation;
    private Point hookLocation;
    private SColor lineColor = SColor.BURNT_BAMBOO,
            bobberColor = SColor.SCARLET,
            hookColor = SColor.BRASS,
            terrainBackColor = SColor.BRONZE,
            terrainFrontColor = SColor.KHAKI,
            liquidColor = SColor.AZUL,
            skyColor = SColor.ALICE_BLUE,
            playerColor = SColor.BETEL_NUT_DYE;
    private Font font;
    private RNG rng = new RNG();
    private SGKeyListener keys;

    private int liquidHeight;
    private int terrainWidth;

    public static void main(String... args) {
        ArrayList<SColor> pallet = SColorFactory.asGradient(SColor.RED, SColor.ORANGE);
        pallet.addAll(SColorFactory.asGradient(SColor.ORANGE, SColor.YELLOW));
        pallet.addAll(SColorFactory.asGradient(SColor.YELLOW, SColor.ELECTRIC_GREEN));
        SColorFactory.addPallet("meter", pallet);

        new FishingSim().go();
    }

    private void go() {
        width = 100;
        height = 40;
        largeTextScale = 4;
        liquidHeight = largeTextScale * 4;
        terrainWidth = largeTextScale * 2 + 1;
        font = new Font("Arial Unicode MS", Font.BOLD, 14);
        initFrame();
        initMap();
        initFish();
        initMeter();
        displayMap();
        
        frame.setVisible(true);

        keys = new SGKeyListener(false, SGKeyListener.CaptureType.TYPED);
        frame.addKeyListener(keys);

        char key = '∅';
        do {
            if (keys.hasNext()) {
                key = keys.next().getKeyChar();
                switch (key) {
                    case ' ':
                        displayMap();
                        throwBobber();
                        dropHook();
                        keys.flush();
                        break;
                    case 'x':
                        displayMap();
                        break;
                }
            }
        } while (key != 'Q');
    }

    private Fish dropHook() {
        pane.placeCharacter(bobberLocation.x, bobberLocation.y + 1, hook, hookColor);
        pane.refresh();
        int y;
        for (y = bobberLocation.y + 2; y <= bed(bobberLocation.x); y++) {
            try {
                Thread.sleep(30);
            } catch (InterruptedException ex) {
            }
            pane.placeCharacter(bobberLocation.x, y - 1, line(UP), lineColor);
            pane.placeCharacter(bobberLocation.x, y, hook, hookColor);
            pane.refresh();
        }

        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
        }

        do {
            pane.clearCell(bobberLocation.x, y);
            y--;
            pane.placeCharacter(bobberLocation.x, y, hook, hookColor);
            pane.refresh();

            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
            }
        } while (y > bobberLocation.y + 1);

        return null;
    }

    private void throwBobber() {
        double strength = getStrength();
        int targetX = (int) (strength * (width - 1 - width / 3.0) + width / 3.0);//finds drop target based on strength percent
        int startY = largeTextScale * 2 + 1;//start at the guy's head
        int startX = terrainWidth;//start at the shoreline

        BallisticsSolver solver = new BallisticsSolver(startX, startY, targetX - 1, liquidHeight - 2, wind, gravity);
        solver.solveByHeight(2);

        int lastX = -1, lastY = -1, bobberX = -2, bobberY = -2;
        double trueTime = solver.getTime();
        int targetTime = targetX * 20;//in milliseconds
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
        long time = 0, lastTime = System.currentTimeMillis();
        do {
            strength = Math.abs(Math.sin(time / timeStep));
            int drawX = (int) (strength * (width - 3)) + 3;
            for (int x = 1; x < meterPane.getGridWidth() - 1; x++) {
                if (x < drawX) {
                    meterPane.placeCharacter(x, 1, '●', SColorFactory.fromPallet("meter", x / (float) (meterPane.getGridWidth())));
                } else {
                    meterPane.clearCell(x, 1);
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
                    pane.clearCell(x, y, SColorFactory.blend(terrainBackColor, SColorFactory.dim(terrainBackColor), PerlinNoise.noise(y, x)));
                } else if (liquidMap[x][y]) {
                    pane.clearCell(x, y, SColorFactory.blend(SColorFactory.blend(liquidColor, SColorFactory.dim(liquidColor), PerlinNoise.noise(x, y)), SColorFactory.dimmest(liquidColor), y / (double) (height - liquidHeight)));
                } else {
                    pane.clearCell(x, y, SColorFactory.blend(SColorFactory.light(skyColor), SColorFactory.dimmer(skyColor), y / (double) liquidHeight));
                }
            }
        }

        for (Fish f : fishes) {
            fishPane.placeCharacter(f.location.x, f.location.y, f.getSymbol().charAt(0), f.getColor());
        }

        fishPane.refresh();
        pane.refresh();
        largeTextPane.refresh();
    }

    private void initFish() {
        fishMap = new Fish[width][height];

        for (int i = 0; i < 50; i++) {
            Fish fish = new Fish(Fish.fishSymbols.charAt(rng.nextInt(Fish.fishSymbols.length())));
            fishes.add(fish);
            boolean placed = false;
            while (!placed) {
                int x = rng.between(terrainWidth + 1, width);
                int y = rng.between(liquidHeight, bed(x));
                if (fishMap[x][y] == null) {
                    fishMap[x][y] = fish;
                    fish.location = new Point(x, y);
                    placed = true;
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

        //fill in rest of terrain and liquid
        int lastHeight = liquidHeight + 3;
        int nextHeight = rng.between(liquidHeight + 4, height - 1);
        for (int x = terrainWidth; x < width; x++) {
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
        frame = new JFrame("Fishing Prototype");
        frame.setBackground(SColor.BLACK);
        frame.getContentPane().setBackground(SColor.BLACK);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLayeredPane layers = new JLayeredPane();
        frame.add(layers, BorderLayout.WEST);

        TextCellFactory textFactory = new TextCellFactory();
        textFactory.setAntialias(true);
        textFactory.initializeByFont(font);
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
        frame.add(meterPane, BorderLayout.SOUTH);

        TextCellFactory largeFactory = new TextCellFactory();
        largeFactory.setAntialias(true);
        largeFactory.initializeBySize(pane.getCellWidth() * largeTextScale, pane.getCellHeight() * largeTextScale, new Font(font.getFontName(), Font.BOLD, 92));
        largeTextPane = new SwingPane(width / largeTextScale, height / largeTextScale, largeFactory);
        layers.setLayer(largeTextPane, JLayeredPane.MODAL_LAYER);
        layers.add(largeTextPane);

        layers.setPreferredSize(pane.getPreferredSize());
        frame.pack();
        frame.setLocationRelativeTo(null);
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
