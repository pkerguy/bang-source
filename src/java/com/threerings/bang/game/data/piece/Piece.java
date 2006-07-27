//
// $Id$

package com.threerings.bang.game.data.piece;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;
import com.threerings.media.util.AStarPathUtil;
import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.DSet;

import com.threerings.bang.data.TerrainConfig;
import com.threerings.bang.game.client.sprite.PieceSprite;
import com.threerings.bang.game.data.BangBoard;
import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.effect.Effect;
import com.threerings.bang.game.data.effect.ShotEffect;
import com.threerings.bang.game.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Does something extraordinary.
 */
public abstract class Piece extends SimpleStreamableObject
    implements Cloneable, DSet.Entry, PieceCodes
{
    /** Uniquely identifies each piece in the game. */
    public int pieceId;

    /** The player index of the owner of this piece or -1 if it is not an
     * owned piece. */
    public int owner = -1;

    /** The tick on which this piece last acted. */
    public short lastActed;

    /** The current x location of this piece's segments. */
    public short x;

    /** The current y location of this piece's segments. */
    public short y;

    /** This piece's orientation. */
    public short orientation;

    /** The percentage damage this piece has taken. */
    public int damage;

    /** The scenarioId for this piece (of null for all). */
    public String scenId;

    /** The piece's last occupied location. */
    public transient short lastX, lastY;
    
    /**
     * Combines the supplied x and y coordintes into a single integer.
     */
    public static int coord (int x, int y)
    {
        return (x << 16) | y;
    }
    
    /**
     * Returns the cost to purchase this piece.
     */
    public int getCost ()
    {
        return 100;
    }

    /**
     * Returns true if this piece is still active and playable.
     */
    public boolean isAlive ()
    {
        return (damage < 100);
    }

    /**
     * Returns the number of ticks that must elapse before this piece can
     * again be moved.
     */
    public short ticksUntilMovable (short tick)
    {
        return (short)Math.max(0, getTicksPerMove() - (tick-lastActed));
    }

    /**
     * Called on every tick to allow a unit to lose hit points or regenerate
     * hit points automatically.
     *
     * @param tick the current game tick.
     * @param board the current board.
     * @param pieces all the pieces on the board in easily accessible form.
     *
     * @return a list of effects to apply to the unit as a result of having 
     * been ticked or null.
     */
    public ArrayList<Effect> tick (short tick, BangBoard board, Piece[] pieces)
    {
        return null;
    }

    /**
     * Called on a piece when it has been maximally damaged.
     */
    public void wasKilled (short tick)
    {
        lastActed = tick;
    }

    /**
     * Returns whether or not this piece should be removed from the board
     * when maximally damaged.
     */
    public boolean removeWhenDead ()
    {
        return false;
    }

    /**
     * By default we expire wreckage after some number of turns.
     */
    public boolean expireWreckage (short tick)
    {
        return (tick - lastActed > WRECKAGE_EXPIRY);
    }

    /**
     * Returns true if the specified coordinates intersect this piece.
     */
    public boolean intersects (int tx, int ty)
    {
        return (x == tx) && (y == ty);
    }

    /**
     * Returns true if this piece intersects the specified region.
     */
    public boolean intersects (Rectangle bounds)
    {
        return bounds.contains(x, y);
    }

    /**
     * Returns true if these two pieces intersect at their current
     * coordinates.
     */
    public boolean intersects (Piece other)
    {
        return other.intersects(x, y);
    }

    /**
     * Returns our combined x and y coordinate.
     */
    public int getCoord ()
    {
        return coord(x, y);
    }

    /** Returns the width of this piece in tiles. */
    public int getWidth ()
    {
        return 1;
    }

    /** Returns the length of this piece in tiles. */
    public int getLength ()
    {
        return 1;
    }

    /** Returns the height of this piece in tiles. */
    public float getHeight ()
    {
        return 1f;
    }

    /** Returns true if this piece is valid for this scenario. */
    public boolean isValidScenario (String scenarioId)
    {
        return (scenId == null || scenarioId == null || 
                scenId.equals(scenarioId));
    }
    
    /** Returns the elevation of this piece in the board's elevation units. */
    public int computeElevation (BangBoard board, int tx, int ty)
    {
        return computeElevation(board, tx, ty, false);
    }

    /** 
     * Returns the elevation of this piece in the board's elevation units.
     *
     * @param moving is true if this elevation is part of a movement path
     */
    public int computeElevation (
            BangBoard board, int tx, int ty, boolean moving)
    {
        return board.getHeightfieldElevation(tx, ty);
    }
    
    /** Returns the number of tiles that this piece can "see". */
    public int getSightDistance ()
    {
        return 5;
    }

    /**
     * Returns the number of tiles that this piece can move.
     */
    public int getMoveDistance ()
    {
        return 1;
    }

    /**
     * Returns the minimum number of tiles away that this piece can fire.
     */
    public int getMinFireDistance ()
    {
        return 1;
    }

    /**
     * Returns the maximum number of tiles away that this piece can fire.
     */
    public int getMaxFireDistance ()
    {
        return 1;
    }

    /**
     * Returns true if the specified target is in range of attack of this
     * piece.
     */
    public boolean targetInRange (int nx, int ny, int tx, int ty)
    {
        int dist = getDistance(nx, ny, tx, ty);
        return (dist >= getMinFireDistance() && dist <= getMaxFireDistance());
    }

    /**
     * Returns the "tile" distance between this and the specified piece.
     */
    public int getDistance (Piece other)
    {
        return (other == null) ? Integer.MAX_VALUE : 
                                 getDistance(other.x, other.y);
    }

    /**
     * Returns the "tile" distance between this piece and the specified
     * location.
     */
    public int getDistance (int tx, int ty)
    {
        return getDistance(x, y, tx, ty);
    }

    /**
     * Returns the Manhatten distance between two points.
     */
    public static int getDistance (int x, int y, int tx, int ty)
    {
        return Math.abs(x - tx) + Math.abs(y - ty);
    }

    /**
     * Gets the cost of traversing this category of terrain in tenths of a
     * movement point.
     */
    public int traversalCost (TerrainConfig terrain)
    {
        return terrain.traversalCost;
    }

    /** Returns a brief description of this piece. */
    public String info ()
    {
        return infoType() + " id:" + pieceId + " o:" + owner +
            " x:" + x + " y:" + y + " d:" + damage;
    }

    /** Returns a translatable name for this piece (or <code>null</code> if
     * none exists). */
    public String getName ()
    {
        return null;
    }
    
    /** Returns the stepper used to compute paths for this type of piece. */
    public AStarPathUtil.Stepper getStepper ()
    {
        return _pieceStepper;
    }

    /**
     * Allows the piece to do any necessary initialization before the game
     * starts.
     */
    public void init ()
    {
        // start with zero damage
        damage = 0;
    }

    /**
     * Updates this pieces position and orientation.
     *
     * @return true if the piece's position changed, false if not.
     */
    public boolean position (int nx, int ny)
    {
        // avoid NOOP
        if (nx != x || ny != y) {
            updatePosition(nx, ny);
            recomputeBounds();
            return true;
        }
        return false;
    }

    /**
     * Instructs the piece to rotate clockwise if direction is {@link Piece#CW}
     * and counter-clockwise if it is {@link Piece#CCW}.
     *
     * @return true if rotation is supported and the piece rotated, false
     * if it is not supported (pieces longer than one segment cannot be
     * rotated).
     */
    public boolean rotate (int direction)
    {
        // update our orientation
        orientation = (short)((direction == CW) ? ((orientation + 1) % 4) :
                              ((orientation + 3) % 4));
        recomputeBounds();
        return true;
    }

    /**
     * Returns true if this piece can pass non-traversable tiles during
     * movement, false otherwise.
     */
    public boolean isFlyer ()
    {
        return false;
    }

    /**
     * Returns true if this piece can remain on non-traversable tiles after
     * movement, false otherwise.
     */
    public boolean isAirborne ()
    {
        return false;
    }

    /**
     * Selects the shortest move that puts us within range of firing on
     * the specified target.
     *
     * @param any if true we don't care about the best shot location, just that
     * there is at least one valid shot location.
     */
    public Point computeShotLocation (
        BangBoard board, Piece target, PointSet moveSet, boolean any)
    {
        int minfdist = getMinFireDistance(), maxfdist = getMaxFireDistance();
        int moves = Integer.MAX_VALUE;

        // first check if we can fire without moving (assuming our current
        // location is in our move set)
        if (moveSet.contains(x, y)) {
            int tdist = target.getDistance(x, y);
            if (tdist >= minfdist && tdist <= maxfdist &&
                checkLineOfSight(board, x, y, target)) {
                return new Point(x, y);
            }
        }

        // next search the move set for the closest location
        Point spot = null;
        for (int ii = 0, ll = moveSet.size(); ii < ll; ii++) {
            int px = moveSet.getX(ii), py = moveSet.getY(ii);
            int dist = getDistance(px, py);
            int tdist = target.getDistance(px, py);
            if (dist < moves && tdist >= minfdist && tdist <= maxfdist &&
                checkLineOfSight(board, px, py, target)) {
                moves = dist;
                if (spot == null) {
                    spot = new Point();
                }
                spot.setLocation(px, py);
                if (any) {
                    break;
                }
            }
        }

        return spot;
    }

    /**
     * Creates any effect that must be applied prior to applying the {@link
     * ShotEffect} that results from this piece shooting another.
     */
    public Effect willShoot (BangObject bangobj, Piece target, ShotEffect shot)
    {
        return null;
    }

    /**
     * Creates an effect that will "shoot" the specified target piece.
     *
     * @param scale a value that should be used to scale the damage done.
     */
    public ShotEffect shoot (BangObject bangobj, Piece target, float scale)
    {
        // create a basic shot effect
        int damage = computeScaledDamage(bangobj, target, scale);
        ShotEffect shot = generateShotEffect(bangobj, target, damage);
        // give the target a chance to deflect the shot
        return target.deflect(bangobj, this, shot, scale);
    }

    /**
     * Gives a unit a chance to "deflect" a shot, by replacing a normal
     * shot effect with one deflected to a different location.
     *
     * @return the original effect if no deflection is desired or a new
     * shot effect that has been properly deflected.
     */
    public ShotEffect deflect (
        BangObject bangobj, Piece shooter, ShotEffect effect, float scale)
    {
        // default is no deflection
        return effect;
    }

    /**
     * When a unit shoots another piece, the unit may also do collateral damage
     * to nearby units. This method should return effects indicating such
     * damage. <em>Note:</em> the piece is responsible for calling {@link
     * Effect#init} on those effects before returning them.
     */
    public Effect[] collateralDamage (
        BangObject bangobj, Piece target, int damage)
    {
        return null;
    }

    /**
     * If a target returns fire when shot, this method should return the
     * appropriate shot effect to enforce that.
     *
     * @param damage the amount of damage done by the initial shooter (the
     * piece may or may not account for this when returning fire).
     */
    public ShotEffect returnFire (
        BangObject bangobj, Piece shooter, int damage)
    {
        return null;
    }

    /**
     * Allows the piece to produce an effect to deploy immediately before it
     * dies.
     *
     * @param shooterId the id of the piece shooting or otherwise damaging
     * this piece, or <code>-1</code> for none
     */
    public Effect willDie (BangObject bangobj, int shooterId)
    {
        return null;  
    }
    
    /**
     * Returns true if this piece prevents other pieces from occupying the
     * same square, or false if it can colocate.
     */
    public boolean preventsOverlap (Piece lapper)
    {
        return true;
    }

    /**
     * Some pieces interact with other pieces, which takes place via this
     * method. An effect should be returned communicating the nature of the
     * interaction.
     */
    public Effect maybeInteract (Piece other)
    {
        return null;
    }

    /**
     * Returns true if this piece can traverse the board at the specified
     * coordinates.
     */
    public boolean canTraverse (BangBoard board, int tx, int ty)
    {
        return board.isGroundOccupiable(tx, ty);
    }

    /**
     * Writes the persistent state of this piece to the specified stream.
     *
     * @param scenIds: A sorted array of scenario Ids
     */
    public void persistTo (ObjectOutputStream oout, String[] scenIds)
        throws IOException
    {
        oout.writeInt(pieceId);
        oout.writeShort(x);
        oout.writeShort(y);
        oout.writeShort(orientation);
        if (scenId == null || scenIds == null) {
            oout.writeShort(-1);
        } else {
            short idx = -1;
            try {
                idx = (short)Arrays.binarySearch(scenIds, scenId);
            } finally {
                oout.writeShort(idx);
            }
        }
    }
    
    /**
     * Reads the persistent state of this piece from the specified stream.
     *
     * @param scenIds: A sorted array of scenario Ids
     */
    public void unpersistFrom (ObjectInputStream oin, String[] scenIds)
        throws IOException
    {
        pieceId = oin.readInt();
        short x = oin.readShort(), y = oin.readShort();
        orientation = oin.readShort();
        position(x, y);
        short idx = oin.readShort();
        if (scenIds != null && idx >= 0 && idx < scenIds.length) {
            scenId = scenIds[idx];
        }
    }
    
    /**
     * Creates the appropriate derivation of {@link PieceSprite} to render
     * this piece.
     */
    public PieceSprite createSprite ()
    {
        return new PieceSprite();
    }

    /**
     * This is normally not needed, but is used by the editor to assign
     * piece IDs to new pieces.
     */
    public void assignPieceId (BangObject bangobj)
    {
        _key = null;
        pieceId = ++bangobj.maxPieceId;
    }
    
    // documentation inherited from interface DSet.Entry
    public Comparable getKey ()
    {
        if (_key == null) {
            _key = new Integer(pieceId);
        }
        return _key;
    }

    @Override // documentation inherited
    public int hashCode ()
    {
        return pieceId;
    }

    @Override // documentation inherited
    public boolean equals (Object other)
    {
        return other instanceof Piece && pieceId == ((Piece)other).pieceId;
    }

    @Override // documentation inherited
    public Object clone ()
    {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    /** Converts our orientation to a human readable string. */
    public String orientationToString ()
    {
        return (orientation >= 0) ? ORIENT_CODES[orientation] :
            ("" + orientation);
    }

    /**
     * Returns true if we can and should fire upon this target. Note that
     * this does not check to see whether the target is in range.
     */
    public boolean validTarget (Piece target, boolean allowSelf)
    {
        boolean valid = (target.isTargetable() && target.isAlive());
        if (!allowSelf) {
            valid = !target.isSameTeam(this) && valid;
        }
        return valid;
    }

    /**
     * Returns true if this piece can be targetted.
     */
    public boolean isTargetable ()
    {
        return false;
    }

    /**
     * Returns true if this piece is on the same team as the target.
     */
    public boolean isSameTeam (Piece target)
    {
        return target.owner == owner;
    }

    /**
     * Determines whether this piece has the necessary line of sight to
     * fire upon the specified target from the given location.
     */
    public boolean checkLineOfSight (
        BangBoard board, int tx, int ty, Piece target)
    {
        int units = board.getElevationUnitsPerTile(),
            e1 = computeElevation(board, tx, ty) +
                (int)(getHeight()*0.5f*units),
            e2 = target.computeElevation(board, target.x, target.y) +
                (int)(target.getHeight()*0.5f*units);
        return board.checkLineOfSight(tx, ty, e1, target.x, target.y, e2);
    }
    
    /**
     * Computes the actual damage done if this piece were to fire on the
     * specified target, accounting for this piece's current damage level and
     * other limiting factors.
     *
     * @param scale a value that should be used to scale the damage after all
     * other factors have been considered.
     */
    public int computeScaledDamage (
            BangObject bangobj, Piece target, float scale)
    {
        // compute the damage we're doing to this piece
        int ddamage = computeDamage(target);

        // scale the damage by our own damage level; but always fire as if
        // we have at least half hit points
        int undamage = Math.max(50, 100-damage);
        ddamage = (ddamage * undamage) / 100;

        // account for any other pieces which have attack adjustments
        if (bangobj != null) {
            for (Piece p : bangobj.pieces) {
                ddamage = p.adjustPieceAttack(this, ddamage);
            }
        }

        // account for any influences on the attacker or defender
        if (this instanceof Unit && ((Unit)this).influence != null) {
            ddamage = ((Unit)this).influence.adjustAttack(target, ddamage);
        }
        if (target instanceof Unit && ((Unit)target).influence != null) {
            ddamage = ((Unit)target).influence.adjustDefend(this, ddamage);
        }

        // finally scale the damage by the desired value
        return Math.round(scale * ddamage);
    }

    /**
     * Adjusts the attack of other pieces.
     */
    public int adjustPieceAttack (Piece attacker, int damage)
    {
        // by default do nothing
        return damage;
    }

    /**
     * Returns the attack influence icon or null if no attack influence.  Used
     * after a call to {@link computeScaledDamage}.
     */
    public String attackInfluenceIcon ()
    {
        return null;
    }
    
    /**
     * Returns the defend influence icon or null if no attack influence.  Used
     * after a call to {@link computeScaledDamage}.
     */
    public String defendInfluenceIcon (Piece target)
    {
        Influence influence = (target instanceof Unit ? 
                ((Unit)target).influence : null);
        if (influence != null && influence.didAdjustDefend()) {
            return influence.getName();
        }
        return null;
    }
    
    /** Returns the frequency with which this piece can move. */
    protected int getTicksPerMove ()
    {
        return 4;
    }

    /**
     * Computes the new orientation for this piece were it to travel from
     * its current coordinates to the specified coordinates.
     */
    protected int computeOrientation (int nx, int ny)
    {
        int hx = x, hy = y;

        // if it is purely a horizontal or vertical move, simply orient
        // in the direction of the move
        if (nx == hx) {
            return (ny > hy) ? SOUTH : NORTH;
        } else if (ny == hy) {
            return (nx > hx) ? EAST : WEST;
        }

        // otherwise try to behave naturally: moving forward first if
        // possible and turning sensibly to reach locations behind us
        switch (orientation) {
        case NORTH: return (ny < hy) ? ((nx > hx) ? EAST : WEST) : SOUTH;
        case SOUTH: return (ny > hy) ? ((nx > hx) ? EAST : WEST) : NORTH;
        case EAST:  return (nx > hx) ? ((ny > hy) ? SOUTH : NORTH) : WEST;
        case WEST:  return (nx < hx) ? ((ny > hy) ? SOUTH : NORTH) : EAST;
        // erm, this shouldn't happen
        default: return NORTH;
        }
    }

    /**
     * Called by {@link #position} after it has confirmed that we are in
     * fact changing position and not NOOPing or setting our location for
     * the first time. Derived pieces that want to customize their
     * position handling should override this method.
     */
    protected void updatePosition (int nx, int ny)
    {
        // determine our new orientation
        orientation = (short)computeOrientation(nx, ny);
        lastX = x;
        lastY = y;
        x = (short)nx;
        y = (short)ny;
    }

    /**
     * Called to allow derived classes to update their bounds when the
     * piece has been repositioned or reoriented.
     */
    protected void recomputeBounds ()
    {
    }

    /** Helper function for {@link #info}. */
    protected String infoType ()
    {
        String cname = getClass().getName();
        return cname.substring(cname.lastIndexOf(".")+1);
    }

    /**
     * Returns the number of percentage points of damage this piece does
     * to pieces of the specified type.
     */
    protected int computeDamage (Piece target)
    {
        log.warning(getClass() + " requested to damage " +
                    target.getClass() + "?");
        return 10;
    }

    /**
     * Generate a shot effect for this piece.
     */
    protected ShotEffect generateShotEffect (
            BangObject bangobj, Piece target, int damage)
    {
        return new ShotEffect(this, target, damage,
                attackInfluenceIcon(), defendInfluenceIcon(target));
    }

    protected transient Integer _key;

    /** The default path-finding stepper. Allows movement in one of the
     * four directions. */
    protected static AStarPathUtil.Stepper _pieceStepper =
        new AStarPathUtil.Stepper() {
        public void considerSteps (int x, int y)
        {
	    considerStep(x, y - 1, 1);
	    considerStep(x - 1, y, 1);
	    considerStep(x + 1, y, 1);
	    considerStep(x, y + 1, 1);
        }
    };

    /** The number of ticks until wreckage expires. */
    protected static final int WRECKAGE_EXPIRY = 6;

    /** Used to move one tile forward from an orientation. */
    protected static final int[] FWD_X_MAP = { 0, 1, 0, -1 };

    /** Used to move one tile forward from an orientation. */
    protected static final int[] FWD_Y_MAP = { -1, 0, 1, 0 };

    /** Used to move one tile backward from an orientation. */
    protected static final int[] REV_X_MAP = { 0, -1, 0, 1 };

    /** Used to move one tile backward from an orientation. */
    protected static final int[] REV_Y_MAP = { 1, 0, -1, 0 };

    /** Used by {@link #orientationToString}. */
    protected static final String[] ORIENT_CODES = { "N", "E", "S", "W" };
}
