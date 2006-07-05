//
// $Id$

package com.threerings.bang.data;

import com.threerings.util.Name;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;

import com.threerings.bang.avatar.data.Look;

/**
 * Extends the {@link BodyObject} with custom bits needed by Bang!.
 */
public class PlayerObject extends BodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playerId</code> field. */
    public static final String PLAYER_ID = "playerId";

    /** The field name of the <code>handle</code> field. */
    public static final String HANDLE = "handle";

    /** The field name of the <code>isMale</code> field. */
    public static final String IS_MALE = "isMale";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";

    /** The field name of the <code>inventory</code> field. */
    public static final String INVENTORY = "inventory";

    /** The field name of the <code>townId</code> field. */
    public static final String TOWN_ID = "townId";

    /** The field name of the <code>scrip</code> field. */
    public static final String SCRIP = "scrip";

    /** The field name of the <code>coins</code> field. */
    public static final String COINS = "coins";

    /** The field name of the <code>stats</code> field. */
    public static final String STATS = "stats";

    /** The field name of the <code>ratings</code> field. */
    public static final String RATINGS = "ratings";

    /** The field name of the <code>poses</code> field. */
    public static final String POSES = "poses";

    /** The field name of the <code>looks</code> field. */
    public static final String LOOKS = "looks";

    /** The field name of the <code>pardners</code> field. */
    public static final String PARDNERS = "pardners";
    // AUTO-GENERATED: FIELDS END

    /** This user's persistent unique id. */
    public int playerId;

    /** This user's cowboy handle (in-game name). */
    public Handle handle;

    /** Whether this character is male or female. */
    public boolean isMale;

    /** Indicates which access control tokens are held by this user. */
    public BangTokenRing tokens;

    /** Contains all items held by this user. */
    public DSet<Item> inventory;

    /** Indicates which town this user currently occupies. */
    public String townId;

    /** The amount of game currency this player is carrying. */
    public int scrip;

    /** The amount of "hard" currency this player is carrying. */
    public int coins;

    /** Statistics tracked for this player. */
    public StatSet stats;

    /** Contains all ratings earned by this player. */
    public DSet<Rating> ratings;

    /** This player's configured avatar poses. See {@link Look.Pose}. */
    public String[] poses;

    /** The avatar looks this player has available. */
    public DSet<Look> looks;

    /** {@link PardnerEntry}s for each of the player's pardners. */
    public DSet<PardnerEntry> pardners;
    
    /**
     * Returns the player's rating for the specified scenario. This method will
     * never return null.
     */
    public Rating getRating (String scenario)
    {
        Rating rating = (Rating)ratings.get(scenario);
        if (rating == null) {
            rating = new Rating();
            rating.scenario = scenario;
        }
        return rating;
    }

    /**
     * Returns the purse owned by this player or the default purse if the
     * player does not yet have one.
     */
    public Purse getPurse ()
    {
        for (Item item : inventory) {
            if (item instanceof Purse) {
                return (Purse)item;
            }
        }
        return Purse.DEFAULT_PURSE;
    }

    /**
     * Returns the look currently in effect for this player.
     */
    public Look getLook (Look.Pose pose)
    {
        String pstr = poses[pose.ordinal()];
        return (pstr != null && looks.containsKey(pstr)) ?
            looks.get(pstr) : looks.get("");
    }

    /**
     * Returns true if this player has at least one {@link BigShotItem} in
     * their inventory.
     */
    public boolean hasBigShot ()
    {
        for (Item item : inventory) {
            if (item instanceof BigShotItem) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this player has earned the specified badge type.
     */
    public boolean holdsBadge (Badge.Type type)
    {
        for (Item item : inventory) {
            if (item instanceof Badge && ((Badge)item).getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this player holds a pass for the specified type of unit.
     */
    public boolean holdsPass (String unit)
    {
        for (Item item : inventory) {
            if (item instanceof UnitPass &&
                ((UnitPass)item).getUnitType().equals(unit)) {
                return true;
            }
        }
        return false;
    }

    @Override // documentation inherited
    public BangTokenRing getTokens ()
    {
        return tokens;
    }

    @Override // documentation inherited
    public OccupantInfo createOccupantInfo ()
    {
        return new BangOccupantInfo(this);
    }

    @Override // documentation inherited
    public Name getVisibleName ()
    {
        return handle;
    }

    @Override // documentation inherited
    public String who ()
    {
        return "'" + handle + "' " + super.who();
    }

    /**
     * Counts the number of avatar articles in this player's inventory.
     */
    public int getDudsCount ()
    {
        int count = 0;
        for (Item item : inventory) {
            if (item instanceof Article) {
                count++;
            }
        }
        return count;
    }

    /**
     * Determines how many of this player's pardners are online.
     */
    public int getOnlinePardnerCount ()
    {
        int count = 0;
        for (PardnerEntry entry : pardners) {
            if (entry.isOnline()) {
                count++;
            }
        }
        return count;
    }
    
    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>playerId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPlayerId (int value)
    {
        int ovalue = this.playerId;
        requestAttributeChange(
            PLAYER_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.playerId = value;
    }

    /**
     * Requests that the <code>handle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setHandle (Handle value)
    {
        Handle ovalue = this.handle;
        requestAttributeChange(
            HANDLE, value, ovalue);
        this.handle = value;
    }

    /**
     * Requests that the <code>isMale</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setIsMale (boolean value)
    {
        boolean ovalue = this.isMale;
        requestAttributeChange(
            IS_MALE, Boolean.valueOf(value), Boolean.valueOf(ovalue));
        this.isMale = value;
    }

    /**
     * Requests that the <code>tokens</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTokens (BangTokenRing value)
    {
        BangTokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>inventory</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToInventory (Item elem)
    {
        requestEntryAdd(INVENTORY, inventory, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>inventory</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromInventory (Comparable key)
    {
        requestEntryRemove(INVENTORY, inventory, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>inventory</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateInventory (Item elem)
    {
        requestEntryUpdate(INVENTORY, inventory, elem);
    }

    /**
     * Requests that the <code>inventory</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setInventory (DSet<com.threerings.bang.data.Item> value)
    {
        requestAttributeChange(INVENTORY, value, this.inventory);
        this.inventory = (value == null) ? null : value.typedClone();
    }

    /**
     * Requests that the <code>townId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTownId (String value)
    {
        String ovalue = this.townId;
        requestAttributeChange(
            TOWN_ID, value, ovalue);
        this.townId = value;
    }

    /**
     * Requests that the <code>scrip</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setScrip (int value)
    {
        int ovalue = this.scrip;
        requestAttributeChange(
            SCRIP, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.scrip = value;
    }

    /**
     * Requests that the <code>coins</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setCoins (int value)
    {
        int ovalue = this.coins;
        requestAttributeChange(
            COINS, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.coins = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>stats</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToStats (Stat elem)
    {
        requestEntryAdd(STATS, stats, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>stats</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromStats (Comparable key)
    {
        requestEntryRemove(STATS, stats, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>stats</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateStats (Stat elem)
    {
        requestEntryUpdate(STATS, stats, elem);
    }

    /**
     * Requests that the <code>stats</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setStats (StatSet value)
    {
        requestAttributeChange(STATS, value, this.stats);
        this.stats = (value == null) ? null : (StatSet)value.clone();
    }

    /**
     * Requests that the specified entry be added to the
     * <code>ratings</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToRatings (Rating elem)
    {
        requestEntryAdd(RATINGS, ratings, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>ratings</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromRatings (Comparable key)
    {
        requestEntryRemove(RATINGS, ratings, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>ratings</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateRatings (Rating elem)
    {
        requestEntryUpdate(RATINGS, ratings, elem);
    }

    /**
     * Requests that the <code>ratings</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setRatings (DSet<com.threerings.bang.data.Rating> value)
    {
        requestAttributeChange(RATINGS, value, this.ratings);
        this.ratings = (value == null) ? null : value.typedClone();
    }

    /**
     * Requests that the <code>poses</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPoses (String[] value)
    {
        String[] ovalue = this.poses;
        requestAttributeChange(
            POSES, value, ovalue);
        this.poses = (value == null) ? null : (String[])value.clone();
    }

    /**
     * Requests that the <code>index</code>th element of
     * <code>poses</code> field be set to the specified value.
     * The local value will be updated immediately and an event will be
     * propagated through the system to notify all listeners that the
     * attribute did change. Proxied copies of this object (on clients)
     * will apply the value change when they received the attribute
     * changed notification.
     */
    public void setPosesAt (String value, int index)
    {
        String ovalue = this.poses[index];
        requestElementUpdate(
            POSES, index, value, ovalue);
        this.poses[index] = value;
    }

    /**
     * Requests that the specified entry be added to the
     * <code>looks</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToLooks (Look elem)
    {
        requestEntryAdd(LOOKS, looks, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>looks</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromLooks (Comparable key)
    {
        requestEntryRemove(LOOKS, looks, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>looks</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateLooks (Look elem)
    {
        requestEntryUpdate(LOOKS, looks, elem);
    }

    /**
     * Requests that the <code>looks</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setLooks (DSet<com.threerings.bang.avatar.data.Look> value)
    {
        requestAttributeChange(LOOKS, value, this.looks);
        this.looks = (value == null) ? null : value.typedClone();
    }

    /**
     * Requests that the specified entry be added to the
     * <code>pardners</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToPardners (PardnerEntry elem)
    {
        requestEntryAdd(PARDNERS, pardners, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>pardners</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromPardners (Comparable key)
    {
        requestEntryRemove(PARDNERS, pardners, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>pardners</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updatePardners (PardnerEntry elem)
    {
        requestEntryUpdate(PARDNERS, pardners, elem);
    }

    /**
     * Requests that the <code>pardners</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setPardners (DSet<com.threerings.bang.data.PardnerEntry> value)
    {
        requestAttributeChange(PARDNERS, value, this.pardners);
        this.pardners = (value == null) ? null : value.typedClone();
    }
    // AUTO-GENERATED: METHODS END
}
