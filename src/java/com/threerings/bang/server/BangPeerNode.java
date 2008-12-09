//
// $Id$

package com.threerings.bang.server;

import com.samskivert.util.HashIntMap;
import com.samskivert.util.Tuple;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.presents.peer.server.PeerNode;
import com.threerings.presents.peer.data.ClientInfo;
import com.threerings.presents.peer.data.NodeObject;

import com.threerings.bang.data.BangClientInfo;
import com.threerings.bang.data.BangNodeObject;
import com.threerings.bang.data.Handle;

import com.threerings.bang.util.BangUtil;

import static com.threerings.bang.Log.log;

/**
 * Handles Bang-specific peer bits.
 */
public class BangPeerNode extends PeerNode
    implements SetListener<DSet.Entry>
{
    /** The index of the town managed by this peer. */
    public int townIndex;

    /** A mapping from playerId to client info record for all players online on this peer. */
    public HashIntMap<BangClientInfo> players = new HashIntMap<BangClientInfo>();

    public BangPeerNode (BangPeerManager peermgr)
    {
        _bpmgr = peermgr;
    }

    @Override // from PeerNode
    public void objectAvailable (NodeObject object)
    {
        super.objectAvailable(object);

        // look up this node's town index once and store it
        townIndex = BangUtil.getTownIndex(((BangNodeObject)object).townId);
        log.info("Got peer object " + townIndex);

        // map and issue a remotePlayerLoggedOn for all logged on players
        for (ClientInfo info : object.clients) {
            BangClientInfo binfo = (BangClientInfo)info;
            players.put(binfo.playerId, binfo);
            _bpmgr.remotePlayerLoggedOn(townIndex, binfo);
        }
    }

    @Override // from PeerNode
    public void attributeChanged (AttributeChangedEvent event)
    {
        super.attributeChanged(event);

        // pass gang directory updates to the HideoutManager
        String name = event.getName();
        if (name.equals(BangNodeObject.ACTIVATED_GANG)) {
            BangServer.hideoutmgr.activateGangLocal((Handle)event.getValue());
        } else if (name.equals(BangNodeObject.REMOVED_GANG)) {
            BangServer.hideoutmgr.removeGangLocal((Handle)event.getValue());
        } else if (name.equals(BangNodeObject.CHANGED_HANDLE)) {
            @SuppressWarnings("unchecked") Tuple<Handle, Handle> tuple =
                (Tuple<Handle, Handle>)event.getValue();
            _bpmgr.remotePlayerChangedHandle(townIndex, tuple.left, tuple.right);
        }
    }

    // from interface SetListener
    public void entryAdded (EntryAddedEvent<DSet.Entry> event)
    {
        // log.info("Remote entry added " + event);
        if (event.getName().equals(NodeObject.CLIENTS)) {
            BangClientInfo info = (BangClientInfo)event.getEntry();
            players.put(info.playerId, info);
            _bpmgr.remotePlayerLoggedOn(townIndex, info);
        }
    }

    // from interface SetListener
    public void entryUpdated (EntryUpdatedEvent<DSet.Entry> event)
    {
        // nada
    }

    // from interface SetListener
    public void entryRemoved (EntryRemovedEvent<DSet.Entry> event)
    {
        // log.info("Remote entry removed " + event);
        if (event.getName().equals(NodeObject.CLIENTS)) {
            BangClientInfo info = (BangClientInfo)event.getOldEntry();
            players.remove(info.playerId);
            _bpmgr.remotePlayerLoggedOff(townIndex, info);
        }
    }

    protected BangPeerManager _bpmgr;
}
