//
// $Id$

package com.threerings.bang.data;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.samskivert.util.ArrayUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;
import com.threerings.media.util.AStarPathUtil;

import com.threerings.bang.data.piece.BigPiece;
import com.threerings.bang.data.piece.Bonus;
import com.threerings.bang.data.piece.Chopper;
import com.threerings.bang.data.piece.Piece;
import com.threerings.bang.util.PointSet;

import static com.threerings.bang.Log.log;

/**
 * Describes the terrain of the game board.
 */
public class BangBoard extends SimpleStreamableObject
{
    /** Creates a board with the specified dimensions. */
    public BangBoard (int width, int height)
    {
        _width = width;
        _height = height;
        _tiles = new int[width*height];
        _btstate = new byte[width*height];
        _tstate = new byte[width*height];
        _pgrid = new byte[width*height];
        _bbounds = new Rectangle(0, 0, _width, _height);
        fill(Terrain.NONE);
    }

    /** A default constructor for unserialization. */
    public BangBoard ()
    {
    }

    /** Returns the width of the board. */
    public int getWidth ()
    {
        return _width;
    }

    /** Returns the height of the board. */
    public int getHeight ()
    {
        return _height;
    }

    /**
     * Returns the bounds of our board. <em>Do not modify</em> the
     * returned rectangle.
     */
    public Rectangle getBounds ()
    {
        return _bbounds;
    }

    /** Fills the board with the specified tile. */
    public void fill (Terrain tile)
    {
        Arrays.fill(_tiles, tile.code);
    }

    /**
     * Returns the tile value at the specified x and y coordinate.
     */
    public Terrain getTile (int xx, int yy)
    {
        int index = yy * _width + xx;
        if (index >= _tiles.length) {
            log.warning("Requested to get OOB tile " +
                        "[x=" + xx + ", y=" + yy + "].");
            Thread.dumpStack();
            return Terrain.NONE;
        } else {
            return Terrain.fromCode(_tiles[index]);
        }
    }

    /**
     * Updates the tile value at the specified x and y coordinate.
     */
    public void setTile (int xx, int yy, Terrain tile)
    {
        int index = yy * _width + xx;
        if (index >= _tiles.length) {
            log.warning("Requested to set OOB tile [x=" + xx + ", y=" + yy +
                        ", tile=" + tile + "].");
            Thread.dumpStack();
        } else {
            _tiles[index] = tile.code;
        }
    }

    /**
     * Computes and returns a path for the specified piece to the
     * specified coordinates. Returns null if no path could be found.
     */
    public List computePath (Piece piece, int tx, int ty)
    {
        return AStarPathUtil.getPath(
            _tpred, piece.getStepper(), piece, _width+_height, piece.x, piece.y,
            tx, ty, true);
    }

    /**
     * Returns the coordinates of a location near to the specified
     * coordinates into which a piece can be spawned. First the
     * coordinates immediately surrounding the location are searched, then
     * one unit away, and so on. Within a particular "shell" the
     * coordinates are searched randomly. Returns null if no occupiable
     * spot could be located.
     */
    public Point getOccupiableSpot (int cx, int cy, int maxdist)
    {
        int bx = -1, by = -1;
        PointSet spots = new PointSet();
      SEARCH:
        for (int dist = 0; dist < 3; dist++) {
            spots.clear();
            spots.addFrame(cx, cy, dist, getBounds());
            int[] coords = spots.toIntArray();
            ArrayUtil.shuffle(coords);
            for (int ii = 0; ii < coords.length; ii++) {
                int hx = PointSet.decodeX(coords[ii]);
                int hy = PointSet.decodeY(coords[ii]);
                if (isOccupiable(hx, hy)) {
                    bx = hx;
                    by = hy;
                    break SEARCH;
                }
            }
        }
        return (bx == -1) ? null : new Point(bx, by);
    }

    /**
     * Adds the supplied set of pieces to our board "shadow" data. This is
     * done at the start of the game; all subsequent changes are
     * incremental.
     */
    public void shadowPieces (Iterator iter)
    {
        while (iter.hasNext()) {
            updateShadow(null, (Piece)iter.next());
        }
    }

    /**
     * Updates the shadow for the specified piece.
     */
    public void updateShadow (Piece opiece, Piece piece)
    {
        // unshadow the piece's old position (big pieces never move)
        if (opiece != null) {
            int pos = _width*opiece.y+opiece.x;
            _tstate[pos] = _btstate[pos];
        }

        // now add a shadow for the new piece
        if (piece != null) {
            if (piece instanceof BigPiece) {
                Rectangle pbounds = ((BigPiece)piece).getBounds();
                for (int yy = pbounds.y, ly = yy + pbounds.height;
                     yy < ly; yy++) {
                    for (int xx = pbounds.x, lx = xx + pbounds.width;
                         xx < lx; xx++) {
                        if (_bbounds.contains(xx, yy)) {
                            _tstate[_width*yy+xx] = 2;
                            _btstate[_width*yy+xx] = 2;
                        }
                    }
                }

            } else if (piece instanceof Bonus) {
                _tstate[_width*piece.y+piece.x] = 1;

            } else {
                _tstate[_width*piece.y+piece.x] = 3;
            }
        }
    }

    /**
     * Returns true if the specified piece can occupy the specified
     * coordinate.
     */
    public boolean canOccupy (Piece piece, int x, int y)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        int max = 1;
        if (piece instanceof Chopper) {
            max = 2;
        }
        return (_tstate[y*_bbounds.width+x] <= max);
    }

    /**
     * Returns true if the specified coordinate is both unoccupied by any
     * other piece and traversable.
     */
    public boolean isOccupiable (int x, int y)
    {
        if (!_bbounds.contains(x, y)) {
            return false;
        }
        return (_tstate[y*_bbounds.width+x] <= 0);
    }

    /**
     * Computes the supplied piece's move sets based on its current
     * location and the state of the board.
     */
    public void computeMoves (Piece piece, PointSet moves)
    {
        // clear out the planning grid
        Arrays.fill(_pgrid, (byte)0);

        int mdist = piece.getMoveDistance();
        log.info("Recomputing sets for " + piece.info() +
                 " [mdist=" + mdist + "].");

        // start with 10x our movement points at our current coordinate
        // (and add one to ensure that we always end up with 1 in our
        // final coordinate)
        byte remain = (byte)(mdist * 10 + 1);
        _pgrid[piece.y*_bbounds.width+piece.x] = remain;

        // now consider each of our four neighbors
        considerMoving(piece, moves, piece.x+1, piece.y, remain);
        considerMoving(piece, moves, piece.x-1, piece.y, remain);
        considerMoving(piece, moves, piece.x, piece.y+1, remain);
        considerMoving(piece, moves, piece.x, piece.y-1, remain);
    }

    /**
     * Computes a set of possible attacks given the specified fire
     * distance.
     */
    public void computeAttacks (
        int fireDistance, int px, int py, PointSet attacks)
    {
        for (int dd = 1; dd <= fireDistance; dd++) {
            for (int xx = px, yy = py - dd; yy < py; xx++, yy++) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px + dd, yy = py; xx > px; xx--, yy++) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px, yy = py + dd; yy > py; xx--, yy--) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
            for (int xx = px - dd, yy = py; xx < px; xx++, yy--) {
                if (_bbounds.contains(xx, yy)) {
                    attacks.add(xx, yy);
                }
            }
        }
    }

    /** Returns a string representation of this board. */
    public String toString ()
    {
        return "[" + _width + "x" + _height + "]";
    }

    /**
     * Extends default behavior to initialize transient members.
     */
    public void readObject (ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        int size = _width*_height;
        _btstate = new byte[size];
        _tstate = new byte[size];
        _pgrid = new byte[size];
        _bbounds = new Rectangle(0, 0, _width, _height);
    }

    /** Helper function for {@link #recomputeSets}. */
    protected void considerMoving (
        Piece piece, PointSet moves, int xx, int yy, byte remain)
    {
        // make sure this coordinate is occupiable
        if (!_bbounds.contains(xx, yy) || !canOccupy(piece, xx, yy)) {
            return;
        }

        // see if we can move into this square with a higher remaining
        // point count than has already been accomplished
        int pos = yy*_bbounds.width+xx;
        byte premain = (byte)(remain - piece.traversalCost(getTile(xx, yy)));
        byte current = _pgrid[pos];
        if (premain <= current) {
            return;
        }

        // if so, do it
        moves.add(xx, yy);
        _pgrid[pos] = premain;

        // and then check all of our neighbors
        considerMoving(piece, moves, xx+1, yy, premain);
        considerMoving(piece, moves, xx-1, yy, premain);
        considerMoving(piece, moves, xx, yy+1, premain);
        considerMoving(piece, moves, xx, yy-1, premain);
    }

    /** Used when path finding. */
    protected transient AStarPathUtil.TraversalPred _tpred =
        new AStarPathUtil.TraversalPred() {
        public boolean canTraverse (Object traverser, int x, int y) {
            return canOccupy((Piece)traverser, x, y);
        }
    };

    /** The width and height of our board. */
    protected int _width, _height;

    /** Contains a 2D array of tiles, defining the terrain. */
    protected int[] _tiles;

    /** Tracks coordinate traversability. */
    protected transient byte[] _tstate, _btstate;

    /** A temporary array for computing move and fire sets. */
    protected transient byte[] _pgrid;

    /** A rectangle containing our bounds, used when path finding. */
    protected transient Rectangle _bbounds;
}
