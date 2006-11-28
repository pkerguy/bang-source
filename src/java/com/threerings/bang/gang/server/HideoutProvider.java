//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.client.HideoutService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link HideoutService}.
 */
public interface HideoutProvider extends InvocationProvider
{
    /**
     * Handles a {@link HideoutService#addToCoffers} request.
     */
    public void addToCoffers (ClientObject caller, int arg1, int arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#changeMemberRank} request.
     */
    public void changeMemberRank (ClientObject caller, Handle arg1, byte arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#expelMember} request.
     */
    public void expelMember (ClientObject caller, Handle arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#formGang} request.
     */
    public void formGang (ClientObject caller, Handle arg1, String arg2, InvocationService.ConfirmListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link HideoutService#leaveGang} request.
     */
    public void leaveGang (ClientObject caller, InvocationService.ConfirmListener arg1)
        throws InvocationException;
}