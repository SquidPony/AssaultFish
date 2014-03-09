package assaultfish.old.physical;

import assaultfish.old.ux.Sounds;
import java.util.HashMap;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;
import squidpony.squidmath.RNG;

/**
 * Creates and stores Creature templates.
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class CreatureFactory {

    private HashMap<String, Creature> creatures = new HashMap<>();//a caching map for holding creatures
    RNG rng = new RNG();

    /**
     * Creates templates for all of the default creatures.
     */
    public void instantiateDefaultCreatures() {
        //Make a husk
        Creature creature = new Creature();
        creature.name = "husk";
        creature.description = "A husk is a human altered through Reaper technology. They typically rush straight towards enemies and attempt to overwhelm them buy jumping on them. Normally they are not much of a threat, but can be dangerous in groups.";
        creature.symbol = 'h';
        creature.color = SColor.GREYISH_DARK_GREEN;
        creature.odor = 3f;//smelly zombie stench
        creature.vision = 5;
        creature.smell = 1;
        creature.health = 4;//pretty weak
        creature.meleeDamage = 3;
        creature.deathSound = Sounds.GRAB;
        creature.idleSound = Sounds.GRAB;
        creatures.put(creature.name, creature);

        //Make a Geth Trooper
        creature = new Creature();
        creature.name = "geth trooper";
        creature.description = "Geth Troopers are front line combatants. They will advance steadily while firing their rifle.";
        creature.symbol = 'g';
        creature.color = SColorFactory.light(SColor.SILVER_GREY);
        creature.odor = 0.2f;
        creature.vision = 10;
        creature.smell = 0;
        creature.meleeAccuracy = 20;
        creature.health = 14;
        creature.meleeDamage = 3;
        creature.deathSound = Sounds.EXPLOSION_WHITE;
        creature.idleSound = Sounds.GETH_IDLE3;
        creature.weapon = Weapon.PULSE_RIFLE;
        creature.range = 8;
        creatures.put(creature.name, creature);

        //Make a Geth Hunter
        creature = new Creature();
        creature.name = "geth hunter";
        creature.description = "Geth Hunters are deadly bipedal machines that can turn invisible to mount sneak attacks on unsuspecting enemies.";
        creature.symbol = 'g';
        creature.color = SColorFactory.lightest(SColor.AQUAMARINE);
        creature.odor = 0.2f;
        creature.vision = 10;
        creature.smell = 0;
        creature.meleeAccuracy = 20;
        creature.health = 14;
        creature.shield = 20;
        creature.meleeDamage = 12;
        creature.hiding = rng.nextBoolean();
        creature.hidingModifier = 0.1f;
        creature.xp = 25;
        creature.deathSound = Sounds.EXPLOSION_PINK;
        creature.idleSound = Sounds.GETH_IDLE;
        creature.weapon = Weapon.PLASMA_SHOTGUN;
        creature.range = 3;
        creatures.put(creature.name, creature);

        //Make a Geth Prime
        creature = new Creature();
        creature.name = "geth prime";
        creature.description = "A giant metal construct. A Geth Prime's plasma cannon can drop even hardened soldiers.";
        creature.symbol = 'G';
        creature.color = SColor.CORAL_RED;
        creature.odor = 2f;//smelly zombie stench
        creature.vision = 17;
        creature.smell = 4;
        creature.meleeAccuracy = 80;
        creature.health = 50;
        creature.shield = 650;
        creature.armor = 100;
        creature.meleeDamage = 250;
        creature.xp = 325;
        creature.grabbable = false;
        creature.deathSound = Sounds.EXPLOSION_WARBLE_PINK;
        creature.idleSound = Sounds.GETH_IDLE_CLEAN;
        creature.weapon = Weapon.SIEGE_PULSE;
        creature.range = 17;
        creatures.put(creature.name, creature);

        //Make a Brute
        creature = new Creature();
        creature.name = "brute";
        creature.description = "A twisted combination of Krogan and Turian bodies. It's huge, hulking, and armored form makes for a very menacing sight.";
        creature.symbol = 'B';
        creature.color = SColor.EGG_DYE;
        creature.odor = 30f;//smelly zombie stench
        creature.vision = 7;
        creature.smell = 10;
        creature.meleeAccuracy = 80;
        creature.health = 320;
        creature.armor = 520;
        creature.meleeDamage = 250;
        creature.xp = 325;
        creature.grabbable = false;
        creature.deathSound = Sounds.BRUTE_DEATH;
        creature.idleSound = Sounds.EXPLOSION_DEEP;
        creatures.put(creature.name, creature);
    }

    /**
     * Returns a new Creature based of the template with the matching name.
     *
     * If there is no such template then null is returned.
     *
     * @param name
     * @return
     */
    public Creature getCreature(String name) {
        return creatures.get(name);
    }
}
