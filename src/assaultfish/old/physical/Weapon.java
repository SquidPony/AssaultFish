package assaultfish.old.physical;

import assaultfish.physical.Item;
import static assaultfish.old.physical.Weapon.WeaponClass.ASSAULT;
import static assaultfish.old.physical.Weapon.WeaponClass.PISTOL;
import static assaultfish.old.physical.Weapon.WeaponClass.SHOTGUN;
import static assaultfish.old.physical.Weapon.WeaponClass.SNIPER;
import assaultfish.old.ux.Sounds;
import squidpony.squidcolor.SColor;
import squidpony.squidcolor.SColorFactory;

/**
 *
 * @author Eben Howard - http://squidpony.com - howard@squidpony.com
 */
public class Weapon extends Item {

    public WeaponClass weaponClass;
    public int weight, capacity, //how many rounds one thermal clip provides
            load, //how many rounds in right now
            rate, damage, accuracy;//weapons fire rate per round, with each bullet having the given damage and accuracy
    public float volume = 5f;//how far away it can normally be heard
    public String soundfx;
    //
    //Built-in weapons
    public static final Weapon //
            CARNIFEX = new Weapon("M-6 Carnifex", "A heavy pistol designed to provide serious stopping power with each shot.", SColor.SILVER, PISTOL, 30, 6, 1, 50, 66, 6f, Sounds.LASER_MEDIUM),
            PREDATOR = new Weapon("M-3 Predator", "A light pistol meant for use as a secondary backup weapon.", SColorFactory.light(SColor.SILVER), PISTOL, 16, 15, 2, 16, 50, 4f, Sounds.LASER_SHORT),
            REVENANT = new Weapon("M-76 Revenant", "An assault rifle that packs a punch but isn't very accurate.", SColorFactory.dim(SColor.SILVER), ASSAULT, 54, 50, 12, 18, 10, 7f, Sounds.LASER_MEDIUM),
            PULSE_RIFLE = new Weapon("Geth Pulse Rifle", "Designed for use primarily by mechanicals, it fires very rapidly.", SColorFactory.lighter(SColor.SILVER), ASSAULT, 33, 150, 10, 2, 50, 5f, Sounds.SHOT_WHITE),
            PLASMA_SHOTGUN = new Weapon("Geth Plasma Shotgun", "A shotgun that fires three plasma disks. Very deadly.", SColorFactory.lightest(SColor.BLUE_GREEN_DYE), SHOTGUN, 58, 4, 1, 23, 60, 8f, Sounds.SHOT_PINK),
            WIDOW = new Weapon("M-98 Widow", "A very powerful and accurate sniper rifle.", SColorFactory.dimmer(SColor.SILVER), SNIPER, 99, 1, 1, 135, 85, 15f, Sounds.LASER_LONG),
            SIEGE_PULSE = new Weapon("Siege Pulse Cannon", "A light artillery weapon used by the largest Geth.", SColorFactory.dimmer(SColor.SILVER), SNIPER, 399, 3, 1, 146, 32, 20f, Sounds.WARBLE_DEEP);
    public static final Weapon[] defaults = new Weapon[]{CARNIFEX, PREDATOR, REVENANT, WIDOW};

    public Weapon() {
        symbol = '£';
    }

    /**
     * Creates a fully set up weapon with the provided parameters.
     *
     * Weapons start with a full load by default.
     *
     * @param name
     * @param description
     * @param color
     * @param weaponClass
     * @param weight
     * @param capacity
     * @param rate
     * @param damage
     * @param accuracy
     * @param volume
     * @param soundfx 
     */
    public Weapon(String name, String description, SColor color, WeaponClass weaponClass, int weight, int capacity, int rate, int damage, int accuracy, float volume, String soundfx) {
        this();
        this.name = name;
        this.description = description;
        this.color = color;
        this.weaponClass = weaponClass;
        this.weight = weight;
        this.capacity = capacity;
        this.load = capacity;
        this.rate = rate;
        this.damage = damage;
        this.accuracy = accuracy;
        this.volume = volume;
        this.soundfx = soundfx;
    }
    
    public Weapon(Weapon other){
        super(other);
        
    }

    public enum WeaponClass {

        ASSAULT, SHOTGUN, SNIPER, SMG, PISTOL, HEAVY
    };
}
