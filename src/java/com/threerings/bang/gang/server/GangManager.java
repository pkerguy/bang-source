//
// $Id$

package com.threerings.bang.gang.server;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.samskivert.util.Collections;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.data.SpeakMarshaller;
import com.threerings.crowd.chat.server.SpeakDispatcher;
import com.threerings.crowd.chat.server.SpeakProvider;

import com.threerings.coin.server.persist.CoinTransaction;

import com.threerings.bang.data.Handle;
import com.threerings.bang.data.PardnerEntry;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.server.PardnerEntryUpdater;
import com.threerings.bang.server.persist.FinancialAction;

import com.threerings.bang.gang.client.GangService;
import com.threerings.bang.gang.data.GangCodes;
import com.threerings.bang.gang.data.GangMemberEntry;
import com.threerings.bang.gang.data.GangObject;
import com.threerings.bang.gang.server.persist.GangRepository;
import com.threerings.bang.gang.server.persist.GangRepository.GangRecord;

import static com.threerings.bang.Log.*;

/**
 * Handles gang-related functionality.
 */
public class GangManager
    implements GangProvider, SpeakProvider.SpeakerValidator, GangCodes
{
    /**
     * Initializes the gang manager and registers its invocation service.
     */
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        _gangrepo = new GangRepository(conprov);

        // register ourselves as the provider of the (bootstrap) GangService
        BangServer.invmgr.registerDispatcher(new GangDispatcher(this), true);
    }

    // documentation inherited from GangProvider
    public void inviteMember (ClientObject caller,
        Handle handle, String message, GangService.ConfirmListener listener)
        throws InvocationException
    {
    }
    
    /**
     * Ensures that the gang data for the identified gang is loaded from the
     * database.  This is called on the invoker thread when resolving clients,
     * and should be followed by a call to {@link #resolveGangObject} or
     * {@link #releaseGangObject} on the omgr thread.
     */
    public void stashGangObject (int gangId)
        throws PersistenceException
    {
        // first, determine if the object is already loaded
        GangObject gangobj;
        synchronized (_gangs) {
            if ((gangobj = _gangs.get(gangId)) != null) {
                gangobj.resolving++;
                return;
            }
        }
        
        // if not, we must load it
        gangobj = _gangrepo.loadGang(gangId, true).createGangObject();
        gangobj.resolving++;
        _gangs.put(gangId, gangobj);
    }
    
    /**
     * Fetches the gang object referenced earlier on the invoker thread by
     * {@link #stashGangObject}, registering it if necessary.  Called when
     * the player client is successfully resolved.
     */
    public void resolveGangObject (PlayerObject user)
    {
        GangObject gangobj = getGangObject(user.gangId);
        gangobj.resolving--;
        new GangMemberEntryUpdater(user, gangobj).updateEntries();
        user.gangOid = gangobj.getOid();
    }
    
    /**
     * Releases the reference created earlier by {@link #stashGangObject}.
     * Called when an error occurs in client resolution.
     */ 
    public void releaseGangObject (int gangId)
    {
        GangObject gangobj = _gangs.get(gangId);
        gangobj.resolving--;
        maybeDestroyGangObject(gangobj);
    }
    
    /**
     * Processes a request to form a gang.  It is assumed that the player does
     * not already belong to a gang and that the provided name is valid.
     */
    public void formGang (
        final PlayerObject user, final Handle name,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        new FinancialAction(user, FORM_GANG_SCRIP_COST, FORM_GANG_COIN_COST) {
            protected int getCoinType () {
                return CoinTransaction.GANG_CREATION;
            }
            protected String getCoinDescrip () {
                return MessageBundle.tcompose("m.gang_creation", name);
            }
            protected String persistentAction () {
                try {
                    _gangrepo.insertGang(_grec);
                    BangServer.playrepo.updatePlayerGang(user.playerId,
                        _grec.gangId, LEADER_RANK,
                        _joined = System.currentTimeMillis());
                    return null;
                } catch (PersistenceException e) {
                    return INTERNAL_ERROR;
                }
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                _gangrepo.deleteGang(_grec.gangId);
                BangServer.playrepo.updatePlayerGang(
                    user.playerId, 0, (byte)0, 0L);
            }
            protected void actionCompleted () {
                log.info("Formed new gang [who=" + user.who() + ", name=" +
                    name + ", gangId=" + _grec.gangId + "].");
                if (!user.isActive()) {
                    return; // he bailed; no point in continuing
                }
                GangObject gangobj = _grec.createGangObject();
                _gangs.put(_grec.gangId, gangobj);
                initGangObject(gangobj);
                try {
                    user.startTransaction();
                    user.setGangId(_grec.gangId);
                    user.setGangRank(LEADER_RANK);
                    user.setJoinedGang(_joined);
                    gangobj.addToMembers(
                        new GangMemberEntryUpdater(user, gangobj).gmentry);
                    user.setGangOid(gangobj.getOid());
                } finally {
                    user.commitTransaction();
                }
                listener.requestProcessed();
            }
            protected void actionFailed () {
                listener.requestFailed(INTERNAL_ERROR);
            }
            protected GangRecord _grec = new GangRecord(name.toString());
            protected long _joined;
        }.start();
    }
    
    /**
     * Processes a request to add to the gang's coffers.  It is assumed that
     * the player belongs to a gang and that the amounts are valid.
     */
    public void addToCoffers (
        final PlayerObject user, final int scrip, final int coins,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        new FinancialAction(user, scrip, coins) {
            protected int getCoinType () {
                return CoinTransaction.GANG_DONATION;
            }
            protected String getCoinDescrip () {
                return "m.gang_donation";
            }
            protected String persistentAction () {
                try {
                    _gangrepo.addToCoffers(user.gangId, scrip, coins);
                    return null;   
                } catch (PersistenceException e) {
                    return INTERNAL_ERROR;
                }
            }
            protected void rollbackPersistentAction ()
                throws PersistenceException {
                _gangrepo.addToCoffers(user.gangId, -scrip, -coins);
            }
            protected void actionCompleted () {
                log.info("Added to gang coffers [who=" + user.who() +
                    ", gangId=" + user.gangId + ", scrip=" + scrip +
                    ", coins=" + coins + "].");
                GangObject gangobj = getGangObject(user.gangId);
                if (gangobj == null) {
                    return; // adder bailed
                }
                try {
                    gangobj.startTransaction();
                    gangobj.setScrip(gangobj.scrip + scrip);
                    gangobj.setCoins(gangobj.coins + coins);
                } finally {
                    gangobj.commitTransaction();
                }
                listener.requestProcessed();
            }
            protected void actionFailed () {
                listener.requestFailed(INTERNAL_ERROR);
            }
        }.start();
    }
    
    /**
     * Processes a request to remove a member from a gang.  The entry provided
     * is assumed to describe a member of the specified gang.  At least one
     * member of the gang must be online.  When the last member is removed, the
     * gang itself is deleted.
     */
    public void removeFromGang (
        final int gangId, final int playerId, final Handle handle,
        final InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        GangObject gangobj = getGangObject(gangId);
        if (gangobj == null) {
            log.warning("Missing gang object for removal [gangId=" +
                gangId + ", playerId=" + playerId + ", handle=" +
                handle + "].");
            throw new InvocationException(INTERNAL_ERROR);
        }
        
        // determine whether to delete the gang
        final boolean delete = (gangobj.members.size() == 1);
        
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                BangServer.playrepo.updatePlayerGang(playerId, 0, (byte)0, 0L);
                if (delete) {
                    _gangrepo.deleteGang(gangId);
                }
            }
            public void handleSuccess () {
                log.info("Removed member from gang [gangId=" + gangId +
                    ", playerId=" + playerId + ", handle=" + handle +
                    ", delete=" + delete + "].");
                // the order is important here; the updater will remove itself
                // when the gang id is cleared and destroy the gang object if
                // there are no more members online
                GangObject gangobj = getGangObject(gangId);
                if (gangobj != null) {
                    gangobj.removeFromMembers(handle);    
                }
                PlayerObject plobj =
                    (PlayerObject)BangServer.lookupBody(handle);
                if (plobj != null) {
                    try {
                        plobj.startTransaction();
                        plobj.setGangId(0);
                        plobj.setGangOid(0);
                    } finally {
                        plobj.commitTransaction();
                    }
                } 
                listener.requestProcessed();
            } 
        });
    }
    
    /**
     * Processes a request to change a member's rank.  The entry provided is
     * assumed to describe a member of the specified gang.
     */
    public void changeMemberRank (
        final int gangId, final int playerId, final Handle handle,
        final byte rank, final InvocationService.ConfirmListener listener)
    {
        BangServer.invoker.postUnit(new PersistingUnit(listener) {
            public void invokePersistent ()
                throws PersistenceException {
                BangServer.playrepo.updateGangRank(playerId, rank);
            }
            public void handleSuccess () {
                log.info("Changed member rank [gangId=" + gangId +
                    ", playerId=" + playerId + ", handle=" + handle +
                    ", rank=" + rank + "].");
                PlayerObject plobj =
                    (PlayerObject)BangServer.lookupBody(handle);
                if (plobj != null) {
                    plobj.setGangRank(rank);
                }
                GangObject gangobj = getGangObject(gangId);
                if (gangobj != null) {
                    GangMemberEntry entry = gangobj.members.get(handle);
                    entry.rank = rank;
                    gangobj.updateMembers(entry);
                }
                listener.requestProcessed();
            } 
        });
    }
    
    // documentation inherited from interface SpeakProvider.SpeakerValidator
    public boolean isValidSpeaker (
        DObject speakObj, ClientObject speaker, byte mode)
    {
        GangObject gangobj = (GangObject)speakObj;
        PlayerObject user = (PlayerObject)speaker;
        return gangobj.members.containsKey(user.handle);
    }
    
    /**
     * Retrieves the identified gang object, initializing it if necessary.
     */
    protected GangObject getGangObject (int gangId)
    {
        GangObject gangobj = _gangs.get(gangId);
        if (gangobj != null && !gangobj.isActive()) {
            initGangObject(gangobj);
        }
        return gangobj;
    }
    
    /**
     * Initializes and registers a previously created and mapped gang object.
     */
    protected void initGangObject (GangObject gangobj)
    {
        gangobj.speakService =
            (SpeakMarshaller)BangServer.invmgr.registerDispatcher(
                new SpeakDispatcher(new SpeakProvider(gangobj, this)), false);
        BangServer.omgr.registerObject(gangobj);
        log.info("Initialized gang object [gangId=" + gangobj.gangId +
            ", name=" + gangobj.name + "].");
    }
    
    /**
     * Unmaps and destroys the given gang object if there are no more members
     * online or in the process of resolution.
     */
    protected void maybeDestroyGangObject (GangObject gangobj)
    {
        synchronized (_gangs) {
            if (!gangobj.canBeDestroyed()) {
                return;
            }
            _gangs.remove(gangobj.gangId);
        }
        BangServer.invmgr.clearDispatcher(gangobj.speakService);
        gangobj.destroy();
        log.info("Destroyed gang object [gangId=" + gangobj.gangId +
            ", name=" + gangobj.name + "].");
    }
    
    /** Updates gang member set entries as the player objects change. */
    protected class GangMemberEntryUpdater extends PardnerEntryUpdater
    {
        public GangMemberEntry gmentry;
        
        public GangMemberEntryUpdater (PlayerObject player, GangObject gangobj)
        {
            super(player);
            _gangobj = gangobj;
            gmentry = (GangMemberEntry)entry;
        }
        
        @Override // documentation inherited
        protected PardnerEntry createPardnerEntry (PlayerObject player)
        {
            return new GangMemberEntry(player);
        }
        
        @Override // documentation inherited
        public void attributeChanged (AttributeChangedEvent ace)
        {
            // remove the updater if the player leaves the gang
            super.attributeChanged(ace);
            if (ace.getName().equals(PlayerObject.GANG_ID) &&
                _player.gangId <= 0) {
                remove();
            }
        }
        
        @Override // documentation inherited
        public void updateEntries ()
        {
            _gangobj.updateMembers(gmentry);
        }
        
        @Override // documentation inherited
        protected boolean shouldRemove ()
        {
            // only remove when player object destroyed or removed from gang
            return (!_player.isActive() || _player.gangId <= 0);
        }
        
        @Override // documentation inherited
        protected void remove ()
        {
            // when the last member logs off, remove the dobj
            super.remove();
            maybeDestroyGangObject(_gangobj);
        }
        
        /** The gang object to update when the entry changes. */
        protected GangObject _gangobj;
    }
    
    /** The persistent store for gang data. */
    protected GangRepository _gangrepo;
    
    /** Maps gang ids to currently loaded gang objects. */
    protected IntMap<GangObject> _gangs =
        Collections.synchronizedIntMap(new HashIntMap<GangObject>());
}