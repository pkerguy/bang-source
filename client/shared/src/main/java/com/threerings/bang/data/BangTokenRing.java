//
// $Id$

package com.threerings.bang.data;

import com.threerings.crowd.data.TokenRing;

/**
 * Provides custom access controls.
 */
public class BangTokenRing extends TokenRing
{
    /** Indicates that the user is an "insider" and may get access to pre-release stuff. */
    public static final int INSIDER = (1 << 1);

    /** Indicates that the user is support personel. */
    public static final int SUPPORT = (1 << 2);

    /** Indicates that the user is a demo account. */
    public static final int DEMO = (1 << 3);

    /** Indicates that the user is anonymous. */
    public static final int ANONYMOUS = (1 << 4);

    /** Indicatest that the user is over 13. */
    public static final int OVER_13 = (1 << 5);

    /** Indicatest that the user is a content creator. */
    public static final int CONTENT_CREATOR = (1 << 6);

    /** Indicatest that the user is able to un-hide. */
    public static final int UNHIDE = (1 << 7);

    /** Indicatest that the user bought the game */
    public static final int PREMIUM = (1 << 8);


    /**
     * Constructs a token ring with the supplied set of tokens.
     */
    public BangTokenRing (int tokens)
    {
        super(tokens);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #UNHIDE} token.
     */
    public boolean isUnhider ()
    {
        return holdsToken(UNHIDE);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #CONTENT_CREATOR} token.
     */
    public boolean isContentCreator ()
    {
        return holdsToken(CONTENT_CREATOR);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #INSIDER} token.
     */
    public boolean isInsider ()
    {
        return holdsToken(INSIDER);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #SUPPORT} token.
     */
    public boolean isSupport ()
    {
        return holdsToken(SUPPORT);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #DEMO} token.
     */
    public boolean isDemo ()
    {
        return holdsToken(DEMO);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #PREMIUM} token.
     */
    public boolean isPremium ()
    {
        return holdsToken(PREMIUM);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #ANONYMOUS} token.
     */
    public boolean isAnonymous ()
    {
        return holdsToken(ANONYMOUS);
    }

    /**
     * Convenience function for checking whether this ring holds the {@link #OVER_13} token.
     */
    public boolean isOver13 ()
    {
        return holdsToken(OVER_13);
    }
}
