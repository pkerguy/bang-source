//
// $Id$

package com.threerings.bang.game.tools.xml;

import java.io.Serializable;
import org.apache.commons.digester.Digester;

import com.samskivert.xml.SetPropertyFieldsRule;
import com.threerings.tools.xml.CompiledConfigParser;

import com.threerings.bang.game.data.TutorialConfig;

/**
 * Parses a {@link TutorialConfig} from its XML definition.
 */
public class TutorialConfigParser extends CompiledConfigParser
{
    // documentation inherited
    protected Serializable createConfigObject ()
    {
        return new TutorialConfig();
    }

    // documentation inherited
    protected void addRules (Digester digest)
    {
        // create and configure class record instances
        String prefix = "tutorial";
        digest.addRule(prefix, new SetPropertyFieldsRule());

        for (int ii = 0; ii < ACTIONS.length; ii += 2) {
            String aprefix = prefix + "/" + ACTIONS[ii];
            digest.addObjectCreate(aprefix, ACTIONS[ii+1]);
            digest.addRule(aprefix, new SetPropertyFieldsRule());
            digest.addSetNext(
                aprefix, "addAction", TutorialConfig.Action.class.getName());
        }
    }

    protected static final String[] ACTIONS = {
        "text", TutorialConfig.Text.class.getName(),
        "wait", TutorialConfig.Wait.class.getName(),
        "add_unit", TutorialConfig.AddUnit.class.getName(),
        "move_unit", TutorialConfig.MoveUnit.class.getName(),
    };
}
