package assaultfish.physical;

import java.util.ArrayList;

/**
 * Lists the possible kinds of reactions that can happen.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public enum Reaction {

    EXPLODE, MELT, MOVE, NEUTRALIZE, EXPAND;

    public static ArrayList<Reaction> getReactions(Element element, Element added) {
        ArrayList<Reaction> reactions = new ArrayList<>();
        switch (element) {
            case ACID:
                switch (added) {
                    case AIR:
                        reactions.add(MOVE);
                        reactions.add(EXPAND);
                        break;
                    case MANA:
                        reactions.add(EXPAND);
                        break;
                    case SAND:
                        reactions.add(EXPLODE);
                        break;
                    case TAR:
                        reactions.add(NEUTRALIZE);
                        break;
                    case WATER:
                        reactions.add(NEUTRALIZE);
                        break;
                }
                break;
            case AIR:
                switch (added) {
                    case ACID:
                        break;
                    case MAGMA:
                        break;
                    case MANA:
                        break;
                    case SAND:
                        break;
                    case TAR:
                        break;
                    case WATER:
                        break;
                }
                break;
            case MAGMA:
                switch (added) {
                    case ACID:
                        break;
                    case AIR:
                        break;
                    case MANA:
                        break;
                    case SAND:
                        break;
                    case TAR:
                        break;
                    case WATER:
                        break;
                }
                break;
            case MANA:
                switch (added) {
                    case ACID:
                        break;
                    case AIR:
                        break;
                    case MAGMA:
                        break;
                    case SAND:
                        break;
                    case TAR:
                        break;
                    case WATER:
                        break;
                }
                break;
            case SAND:
                switch (added) {
                    case ACID:
                        break;
                    case AIR:
                        break;
                    case MAGMA:
                        break;
                    case MANA:
                        break;
                    case TAR:
                        break;
                    case WATER:
                        break;
                }
                break;
            case TAR:
                switch (added) {
                    case ACID:
                        break;
                    case AIR:
                        break;
                    case MAGMA:
                        break;
                    case MANA:
                        break;
                    case SAND:
                        break;
                    case WATER:
                        break;
                }
                break;
            case WATER:
                switch (added) {
                    case ACID:
                        break;
                    case AIR:
                        break;
                    case MAGMA:
                        break;
                    case MANA:
                        break;
                    case SAND:
                        break;
                    case TAR:
                        break;
                }
                break;
        }

        return reactions;
    }
}
