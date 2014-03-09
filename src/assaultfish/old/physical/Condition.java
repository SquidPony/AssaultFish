package assaultfish.old.physical;

import assaultfish.physical.Element;


/**
 * Something that affects a Creature and has a time limit.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Condition {

    public int turns = 1;//how many turns until it expires. the turn of expiration it is still in effect
    public Element type;//the nature of the condition
    public int damage = 1;//how much damage is done per turn, if this is negative then it is healing
    public Condition causes;
    public float causeChance;//percent chance per turn that the given second condition is caused. a condition can be caused only once
    public boolean caused = false;//once this condition has caused another, it will not do so again
    public boolean stuns = false;//a condition that stuns keeps the creature from being able to take action until it expires
}
