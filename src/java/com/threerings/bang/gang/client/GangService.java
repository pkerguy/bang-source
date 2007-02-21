//
// $Id$

package com.threerings.bang.gang.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.bang.data.Handle;

import com.threerings.bang.gang.data.GangInfo;
/**
 * Provides gang-related functionality.
 */
public interface GangService extends InvocationService
{
    /**
     * Requests to view information concerning a gang.
     *
     * @param listener a listener to receive the {@link GangInfo}.
     */
    public void getGangInfo (Client client, Handle name, ResultListener listener);

    /**
     * Invite the specified user to be a member of our gang.
     */
    public void inviteMember (
        Client client, Handle handle, String message, ConfirmListener listener);
}
