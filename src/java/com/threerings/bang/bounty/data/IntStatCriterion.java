//
// $Id$

package com.threerings.bang.bounty.data;

import java.io.IOException;
import java.util.HashSet;

import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;

import com.threerings.util.MessageBundle;

import com.threerings.bang.data.Stat;

import com.threerings.bang.game.data.BangObject;
import com.threerings.bang.game.data.Criterion;

/**
 * Requires that a particular integer stat be less than, greater than or equal to a value.
 */
public class IntStatCriterion extends Criterion
{
    /** Defines the various supported conditions. */
    public enum Condition { LESS_THAN, AT_LEAST, EQUAL_TO };

    /** The statistic in question. */
    public Stat.Type stat;

    /** The condition to be met. */
    public Condition condition;

    /** The value against which to compare the stat. */
    public int value;

    // from Criterion
    public String getDescription ()
    {
        String msg = MessageBundle.compose("m." + condition.toString().toLowerCase() + "_descrip",
                                           stat.key(), MessageBundle.taint(String.valueOf(value)));
        return MessageBundle.qualify(OfficeCodes.OFFICE_MSGS, msg);
    }

    // from Criterion
    public void addWatchedStats (HashSet<Stat.Type> stats)
    {
        stats.add(stat);
    }

    // from Criterion
    public String getCurrentState (BangObject bangobj)
    {
        return String.valueOf(bangobj.critStats.getIntStat(stat));
    }

    // from Criterion
    public String isMet (BangObject bangobj)
    {
        return createMessage(bangobj, "failed");
    }

    // from Criterion
    public String reportMet (BangObject bangobj)
    {
        return createMessage(bangobj, "met");
    }

    // from interface Savable
    public void write (JMEExporter ex) throws IOException
    {
        OutputCapsule out = ex.getCapsule(this);
        out.write(stat.toString(), "stat", null);
        out.write(condition.toString(), "condition", null);
        out.write(value, "value", 0);
    }

    // from interface Savable
    public void read (JMEImporter im) throws IOException
    {
        InputCapsule in = im.getCapsule(this);
        stat = Stat.Type.valueOf(in.readString("stat", null));
        condition = Condition.valueOf(in.readString("condition", null));
        value = in.readInt("value", 0);
    }

    // from interface Savable
    public Class getClassTag ()
    {
        return getClass();
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        IntStatCriterion ocrit = (IntStatCriterion)other;
        return stat == ocrit.stat && condition == ocrit.condition && value == ocrit.value;
    }

    @Override // from Object
    public String toString ()
    {
        return stat + " " + condition + " " + value;
    }

    protected String createMessage (BangObject bangobj, String type)
    {
        int actual = (bangobj.critStats == null) ? 0 : bangobj.critStats.getIntStat(stat);
        switch (condition) {
        case LESS_THAN:
            if (type.equals("failed") && actual < value) {
                return null;
            }
            break;
        case EQUAL_TO:
            if (type.equals("failed") && actual == value) {
                return null;
            }
            break;
        case AT_LEAST:
            if (type.equals("failed") && actual >= value) {
                return null;
            }
            break;
        }
        String msg = MessageBundle.compose("m." + condition.toString().toLowerCase() + "_" + type,
                                           stat.key(), MessageBundle.taint(String.valueOf(value)),
                                           MessageBundle.taint(String.valueOf(actual)));
        return MessageBundle.qualify(OfficeCodes.OFFICE_MSGS, msg);
    }
}
