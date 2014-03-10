package assaultfish.fishing;

import assaultfish.physical.Terrain;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
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
    private SwingPane pane, largeTextPane, meterPane;
    private int largeTextScale;
    private boolean[][] terrainMap;
    private boolean[][] liquidMap;
    private int width, height;
    private Terrain terrain;
    private char bobber = 'O',
            hook = 'J',
            fish = 'f',
            wall = '#';
    private Point bobberLocation;
    private Point hookLocation;
    private SColor lineColor = SColor.SILVER,
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
//        new FishingSim().go();
        new FishingSim().test();
    }

    private void test() {
        BallisticsSolver solver = new BallisticsSolver(10, 30, 50, 50);
        solver.solveByHeight(2);
    }

    private void go() {
        width = 100;
        height = 40;
        largeTextScale = 4;
        liquidHeight = largeTextScale * 4;
        terrainWidth = largeTextScale * 2 + 1;
        font = new Font("Ariel", Font.PLAIN, 14);
        initFrame();
        initMap();
        initMeter();
        displayMap();

        keys = new SGKeyListener(true, SGKeyListener.CaptureType.TYPED);
        frame.addKeyListener(keys);

        char key;
        do {
            key = keys.next().getKeyChar();
            switch (key) {
                case ' ':
                    displayMap();
                    throwBobber();
                    break;
                case 'x':
                    displayMap();
                    break;
            }
        } while (key != 'Q');
    }

    private void throwBobber() {
        keys.blockOnEmpty(false);//switch to event based

        char key = 'x';
        meterPane.placeHorizontalString((meterPane.getGridWidth() - "Cast Strength".length() - 1) / 2, 0, "Cast Strength");
        meterPane.placeHorizontalString(2, 0, "None");
        meterPane.placeHorizontalString(meterPane.getGridWidth() - 3 - "Max".length(), 0, "Max");

        int strengthMin = 3, strengthMax = 100;
        double time = 0, strength = strengthMin;
        do {
            strength = (Math.abs(Math.sin(time)) / Math.PI) * (strengthMax - strengthMin);
            strength += strengthMin;
            for (int x = 1; x < meterPane.getGridWidth() - 1; x++) {
                if (x < (strength / strengthMax) * (meterPane.getWidth() - 4) + 4) {
                    meterPane.placeCharacter(x, 1, '#');
                } else {
                    meterPane.clearCell(x, 1);
                }
            }
            meterPane.refresh();

            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
                Logger.getLogger(FishingSim.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (keys.hasNext()) {
                key = keys.next().getKeyChar();
            }

            time += 0.001;
        } while (key != ' ');

        int targetX = (int) ((strength / strengthMax) * (width - 1 - width / 3) + width / 3);
//        int targetX = rng.between(width / 3, width - 2);
        int startY = largeTextScale * 2 + 1;//start at the guy's head
        int startX = terrainWidth;//start at the shoreline

        double forceRight = targetX - startX;
        double forceUp = startY - 1;//all the way to the top then back to water level
        double gravity = (forceUp + liquidHeight - 1) / (double) (targetX - startX);//how much fall per square

        double x = startX;
        double y = startY;

        int bobberX = startX;
        int bobberY = startY;
        pane.placeCharacter(bobberX, bobberY, bobber, bobberColor);
        pane.refresh();

        while (x < targetX || y < liquidHeight - 1) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
            }

            x = Math.min(x + 1, targetX);
            y += (forceUp - gravity * (x - startX) > 0) ? -gravity : gravity;
            y = Math.min(y, liquidHeight - 1);

//            pane.clearCell(bobberX, bobberY, SColorFactory.blend(SColorFactory.light(skyColor), SColorFactory.dimmer(skyColor), bobberY / (double) liquidHeight));
//            pane.refresh();
            bobberX = (int) x;
            bobberY = (int) y;
            pane.placeCharacter(bobberX, bobberY, bobber, bobberColor);
            pane.refresh();
        }

        for (time = 0; time <= 1; time += 0.001) {
            x
                    = bobberX = (int) x;
            bobberY = (int) y;
            pane.placeCharacter(bobberX, bobberY, bobber, bobberColor);
            pane.refresh();
        }

        keys.blockOnEmpty(true);
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

        pane.refresh();
        largeTextPane.refresh();
    }

    private void initMeter() {
        for (int x = 0; x < meterPane.getGridWidth(); x++) {
            for (int y = 0; y < meterPane.getGridHeight(); y++) {
                meterPane.clearCell(x, y);
            }
        }

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
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLayeredPane layers = new JLayeredPane();
        frame.add(layers, BorderLayout.WEST);

        TextCellFactory textFactory = new TextCellFactory();
        textFactory.setAntialias(true);
        textFactory.initializeByFont(font);
        pane = new SwingPane(width, height, textFactory);
        layers.setLayer(pane, JLayeredPane.DEFAULT_LAYER);
        layers.add(pane);

        meterPane = new SwingPane(width, 3, textFactory);
        meterPane.setDefaultBackground(SColor.BLUE_VIOLET_DYE);
        meterPane.setDefaultForeground(SColor.BLOOD_RED);
        frame.add(meterPane, BorderLayout.SOUTH);

        TextCellFactory largeFactory = new TextCellFactory();
        largeFactory.setAntialias(true);
        largeFactory.initializeBySize(pane.getCellWidth() * largeTextScale, pane.getCellHeight() * largeTextScale, new Font(font.getFontName(), Font.BOLD, 92));
        largeTextPane = new SwingPane(width / largeTextScale, height / largeTextScale, largeFactory);
        largeTextPane.setDefaultBackground(SColor.TRANSPARENT);
        for (int x = 0; x < largeTextPane.getGridWidth(); x++) {
            for (int y = 0; y < largeTextPane.getGridHeight(); y++) {
                largeTextPane.clearCell(x, y);
            }
        }
        layers.setLayer(largeTextPane, JLayeredPane.MODAL_LAYER);
        layers.add(largeTextPane);

        layers.setPreferredSize(pane.getPreferredSize());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private char line(Direction dir) {
        switch (dir) {
            case LEFT:
            case RIGHT:
                return '-';
            case UP:
            case DOWN:
                return '|';
            case UP_LEFT:
            case DOWN_RIGHT:
                return '\\';
            case UP_RIGHT:
            case DOWN_LEFT:
                return '/';
            default:
                return ' ';
        }
    }

    private class BallisticsSolver {

        private int startX, startY, endX, endY, minY;
        private double angle, velocity, time;
        private double wind = 2.0, gravity = 3.0;

        public BallisticsSolver(int startX, int startY, int endX, int endY) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
        }

        public void solveByVelocity(double velocity) {

        }

        public void solveByHeight(int targetY) {
            double lowAngle = 0;
            double maxHighAngle = 20;
            double highAngle = maxHighAngle;
            double currentAngle = (lowAngle + highAngle) / 2;
            int lastY = Integer.MAX_VALUE;

            while (true) {
                solveByAngle(currentAngle);
                if (minY == targetY) {
                    break;
                } else if (minY > targetY) {
                    if (minY == lastY) {
                        maxHighAngle += 5;//undershot so need to try higher angle
                        highAngle = maxHighAngle;
                    }
                    lowAngle = currentAngle;
                    currentAngle = (currentAngle + highAngle) / 2;
                } else {//minY < targetY
                    highAngle = currentAngle;
                    currentAngle = (currentAngle + lowAngle) / 2;
                }
                lastY = minY;
            }

            System.out.println("");
            System.out.println("-- Solved By Height for " + targetY);
            System.out.println("Y: " + minY);
            System.out.println("Angle: " + Math.toDegrees(angle));
            System.out.println("Velocity: " + velocity);
            System.out.println("Travel Time: " + time);
        }

        /**
         * Takes an angle in degrees and figures out a velocity that will cause the trajectory to
         * reach the target point.
         *
         * @param angle
         */
        public void solveByAngle(double angle) {
            this.angle = Math.toRadians(angle);

            double lowVelocity = 0.001, highVelocity = Double.MAX_VALUE / 2;
            double timeStep = 0.01;
            velocity = 20.0;
            System.out.println("Testing velocity: " + velocity);
            time = -timeStep;
            int x, y;
            minY = Integer.MAX_VALUE;
            search:
            while (true) {
                time += timeStep;

                x = x(time);
                y = y(time);
                minY = Integer.min(minY, y);

                if (x == endX && y == endY) {
                    break search;
                } else if (x > endX) {//overshot
                    highVelocity = velocity;
                    velocity = (velocity + lowVelocity) / 2;
                    System.out.println("Testing velocity: " + velocity);
                    time = 0;
                } else if (y > endY) {//undershot
                    lowVelocity = velocity;
                    velocity = (velocity + highVelocity) / 2;
                    System.out.println("Testing velocity: " + velocity);
                    time = 0;
                }

                if (lowVelocity == velocity || highVelocity == velocity) {
//                    time -= timeStep;//back up one step
                    timeStep *= 0.8;
                    lowVelocity /= 2;
                    highVelocity *= 2;
                    velocity = (highVelocity + lowVelocity) / 2.0;
                }

            }

            System.out.println("Time: " + time);
            System.out.println("Y: " + minY);
        }

        public int x(double time) {
            return (int) (startX + velocity * Math.cos(angle) * time - 0.5 * -wind * time * time);
        }

        public int y(double time) {
            return (int) (startY + -velocity * Math.sin(angle) * time + 0.5 * gravity * time * time);
        }

    }
}
