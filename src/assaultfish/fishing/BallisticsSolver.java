package assaultfish.fishing;

/**
 * Solves ballistics problems given the start and end points.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
class BallisticsSolver {

    private final int startX, startY, endX, endY;
    private final double wind, gravity;
    private int minY;
    private double angle;
    private double velocity;
    private double time;

    /**
     * Builds a solver which can guarantee the endpoint will be reached.
     *
     * Positive wind values push to the left. Positive gravity values push down.
     *
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     * @param wind
     * @param gravity
     */
    public BallisticsSolver(int startX, int startY, int endX, int endY, double wind, double gravity) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.wind = wind;
        this.gravity = gravity;
    }

    /**
     * Finds an angle and velocity which guarantees that the given height is reach, and no higher.
     * In this GUI context, "higher" means smaller Y.
     *
     * There are many possible solutions, but this method will prefer ones with smaller angles.
     *
     * @param targetY
     */
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
                    maxHighAngle += 5; //undershot so need to try higher angle
                    highAngle = maxHighAngle;
                }
                lowAngle = currentAngle;
                currentAngle = (currentAngle + highAngle) / 2;
            } else {
                //minY < targetY
                highAngle = currentAngle;
                currentAngle = (currentAngle + lowAngle) / 2;
            }
            lastY = minY;
        }
//        System.out.println("");
//        System.out.println("-- Solved By Height for " + targetY);
//        System.out.println("Y: " + minY);
//        System.out.println("Angle: " + Math.toDegrees(angle));
//        System.out.println("Velocity: " + velocity);
//        System.out.println("Travel Time: " + time);
    }

    /**
     * Takes an angle in degrees and figures out a velocity that will cause the trajectory to reach
     * the target point.
     *
     * @param angle
     */
    public void solveByAngle(double angle) {
        this.angle = Math.toRadians(angle);
        double lowVelocity = 0.001;
        double highVelocity = Double.MAX_VALUE / 2;
        double timeStep = 0.01;
        velocity = 20.0;
        time = -timeStep;
        int x;
        int y;
        minY = Integer.MAX_VALUE;

        while (true) {
            time += timeStep;
            x = x(time);
            y = y(time);
            minY = Integer.min(minY, y);
            if (x == endX && y == endY) {
                break;
            } else if (x > endX) {
                //overshot
                highVelocity = velocity;
                velocity = (velocity + lowVelocity) / 2;
                time = 0;
                minY = Integer.MAX_VALUE;
            } else if (y > endY) {
                //undershot
                lowVelocity = velocity;
                velocity = (velocity + highVelocity) / 2;
                time = 0;
                minY = Integer.MAX_VALUE; 
            }
            if (lowVelocity == velocity || highVelocity == velocity) {
                timeStep *= 0.8;
                lowVelocity *= 0.8;
                highVelocity *= 1.2;
                velocity = (highVelocity + lowVelocity) / 2.0;
            }
        }
    }

    /**
     * Returns the x coordinate of the object at the given time.
     *
     * @param time
     * @return
     */
    public int x(double time) {
        return (int) (startX + velocity * Math.cos(angle) * time - 0.5 * wind * time * time);
    }

    /**
     * Returns the y coordinate of the object at the given time.
     *
     * @param time
     * @return
     */
    public int y(double time) {
        return (int) (startY + -velocity * Math.sin(angle) * time + 0.5 * gravity * time * time);
    }

    public double getAngle() {
        return angle;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getTime() {
        return time;
    }
}
