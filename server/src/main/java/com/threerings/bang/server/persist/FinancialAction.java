//
// $Id$

package com.threerings.bang.server.persist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Statement;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.util.Invoker;

import com.threerings.bang.server.BangAuthenticator;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.server.InvocationException;

import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.PlayerObject;
import com.threerings.bang.server.BangInvoker;
import com.threerings.bang.server.BangServer;
import com.threerings.bang.util.DeploymentConfig;
import com.threerings.user.OOOUser;
import jdk.nashorn.internal.scripts.JD;

import static com.threerings.bang.Log.log;

/**
 * Provides a robust framework for doing something in exchange for a player's money. Financial
 * actions must be posted via {@link BangInvoker#post}.
 */
public abstract class FinancialAction extends Invoker.Unit
{

    public static String API_KEY = "13932gh0ghf7803172fg8027fg1328g140891624908164";

    /**
     * Starts this financial action. <em>Don't call this method, call {@link BangInvoker#post} and
     * it will take care of everything.</em>
     */
    public boolean checkStart ()
        throws InvocationException
    {
        return true;
    }

    @Override // documentation inherited
    public boolean invoke ()
    {
        if(_user.scrip < _scripCost)
        {
            fail(BangCodes.E_INSUFFICIENT_SCRIP);
            return true;
        }
        try {
            if (DeploymentConfig.usesCoins() && _coinCost > 0) {
                // _coinres = _coinmgr.getCoinRepository().reserveCoins(getCoinAccount(), _coinCost);
                _coinres = _coinCost;
            }
            // then do our persistent business
            String errmsg = persistentAction();
            if (errmsg != null) {
                fail(errmsg);
                return true;
            }
            _actionTaken = true;

            if(!DeploymentConfig.beta_build)
            {
                if (DeploymentConfig.usesCoins() && _coinCost > 0) {
                    // finally "spend" our reserved coins
                    if (!spendCoinsNow(_coinCost)) {
                        log.warning("Failed to spend coin reservation " + this, "resid", _coinres);
                        fail(BangCodes.E_INSUFFICIENT_COINS);
                        return true;
                    }
                }
            }
            if (shouldSpendCash()) {
                // spend the in-game cash
                spendCash();
                _cashSpent = true;
            }


        } catch (Exception e) {
            log.warning("Financial action failed " + this, e);
            fail(BangCodes.INTERNAL_ERROR);
        }

        return true;
    }

    @Override // documentation inherited
    public void handleResult ()
    {
        try {
            if (_failmsg != null) {
                // return the scrip and coins to the actor
                actionFailed(_failmsg);
            } else {
                actionCompleted();
            }

        } finally {
            // now it's safe for this actor to start another financial action
            _accountLock.remove(getCoinAccount());
        }
    }

    /** Returns a string representation of this instance. */
    public String toString ()
    {
        StringBuffer buf = new StringBuffer("[");
        toString(buf);
        return buf.append("]").toString();
    }

    protected FinancialAction (PlayerObject user, int scripCost, int coinCost)
    {
        _user = user;
        // admins and support get everything for free because they're cool like that
        _scripCost = scripCost;
        _coinCost = coinCost;
    }

    protected FinancialAction (int scripCost, int coinCost)
    {
        _scripCost = scripCost;
        _coinCost = coinCost;
    }

    /**
     * If a financial action involves coins, this method <em>must</em> be overridden to provide a
     * translatable string describing the purchase.
     */
    protected String getCoinDescrip ()
    {
        return null;
    }

    /**
     * Here derived classes can take any persistent action needed knowing that necessary coins have
     * been reserved and necessary scrip has been spent.
     *
     * @return null if the action was taken, a translatable error string if the action could not be
     * taken for whatever reason (any action taken in this method must be rolled back before
     * returning as {@link #rollbackPersistentAction} will <em>not</em> be called).
     */
    protected String persistentAction ()
        throws PersistenceException
    {
        return null;
    }

    /**
     * Any action taken in {@link #persistentAction} must be revoked in this method, which is
     * called if we fail to finalize
     */
    protected void rollbackPersistentAction ()
        throws PersistenceException
    {
    }

    /**
     * If all persistent processing completed successfully, this method will be called back on the
     * distributed object thread to allow final completion of the action.
     */
    protected void actionCompleted ()
    {
        StringBuffer buf = (new StringBuffer(getPurchaseType())).append("_purchase ");
        buf.append(_user.playerId).append(" t:").append(getGoodType());
        buf.append(" s:").append(_scripCost).append(" c:").append(_coinCost);
        BangServer.itemLog(buf.toString());
    }

    /**
     * If any step of the persistent processing of an action failed, rollbacks will be attempted
     * for all completed parts of the action and this method will be called on the distributed
     * object thread to allow for reporting of the failed action.
     *
     * <p><em>Note:</em> the user's scrip and coins will have been returned (in their user object
     * and in the database) by the time this method is called.
     *
     * @param cause either {@link BangCodes#INTERNAL_ERROR} or the message from an {@link
     * InvocationException} that was thrown during the processing of the action.
     */
    protected void actionFailed (String cause)
    {
    }

    /**
     * Called if something goes wrong during any step of this financial action. Everything that
     * completed successfully will be rolled back.
     */
    protected void fail (String failureMessage)
    {
        _failmsg = failureMessage;

        // // roll everything back that needs it
        // if (_coinres != -1) {
        //     // return the coin reservation
        //     try {
        //         if (!_coinmgr.getCoinRepository().returnReservation(_coinres)) {
        //             log.warning("Failed to return coins " + this + ".");
        //         }
        //     } catch (Exception e) {
        //         log.warning("Failed to return coins " + this, e);
        //     }
        // }

        if (_cashSpent) {
            try {
                grantCash();
            } catch (PersistenceException pe) {
                log.warning("Failed to return cash " + this, pe);
            }
        }

        if (_actionTaken) {
            try {
                rollbackPersistentAction();
            } catch (PersistenceException pe) {
                log.warning("Failed to rollback action " + this, pe);
            }
        }
    }

    /**
     * Locks (or attempts to lock) the coin account and deducts the required funds from the dobj.
     * This is called by the bang invoker before it queues this action for execution.
     */
    protected void lockAndDeduct ()
        throws InvocationException
    {
        String account = getCoinAccount();
        String ntype = getClass().getName(), otype = _accountLock.get(account);
        if (otype != null) {
            log.info("Preventing overlapping financial action", "who", account, "new", ntype,
                     "old", otype);
            throw new InvocationException(BangCodes.BANG_MSGS, "e.processing_purchase");
        }
        _accountLock.put(account, ntype);

        // check and immediately deduct the necessary funds
        String errcode = checkSufficientFunds();
        if (errcode != null) {
            _accountLock.remove(account); // release our lock
            throw new InvocationException(errcode);
        }
    }

    /**
     * Returns the actor's account in the coin database.
     */
    protected String getCoinAccount ()
    {
        return _user.username.toString();
    }

    /**
     * Checks whether the account has sufficient funds to complete the transaction.
     *
     * @return null if all is well, an error code to be reported to the client if not.
     */
    protected String checkSufficientFunds ()
    {
        if (_user.scrip < _scripCost) {
            return BangCodes.E_INSUFFICIENT_SCRIP;
        }
        switch (DeploymentConfig.getPaymentType()) {
        default:
        case COINS:
            if (_user.getCoins() < _coinCost) {
                return BangCodes.E_INSUFFICIENT_COINS;
            }
            break;
        case ONETIME:
            if (_coinCost > 0 && !_user.holdsOneTime()) {
                return BangCodes.E_LACK_ONETIME;
            }
            break;
        }
        return null;
    }

    /**
     * Checks whether we are spending non-coin currency.
     */
    protected boolean shouldSpendCash ()
    {
        return (_scripCost > 0);
    }

    /**
     * Updates the database to spend the actor's non-coin currency.
     */
    protected void spendCash ()
        throws PersistenceException
    {
        if(_user.scrip < _scripCost)
        {
            fail(BangCodes.E_INSUFFICIENT_SCRIP);
            return;
        }
        _user.setScrip(_user.getScrip() - _scripCost);
        _playrepo.spendScrip(_user.playerId, _scripCost);
    }

    /**
     * Updates the database to grant the cost in non-coin currency to the actor.
     */
    protected void grantCash ()
        throws PersistenceException
    {
//        _user.setScrip(_user.getScrip() + _scripCost);
//        _playrepo.grantScrip(_user.playerId, _scripCost);
    }

    /**
     * Updates the database to spend the actor's coins.
     */
    protected boolean spendCoinsNow (int resId)
        throws PersistenceException
    {
        if(DeploymentConfig.beta_build)
        {
            return true;
        }
        if(_user.getCoins() < resId)
        {
            fail(BangCodes.E_INSUFFICIENT_COINS);
            return false;
        }
        String desc = getCoinDescrip();
        if(desc == null) desc = "Unknown";
        desc = "Ingame";
        try {
            URL data = new URL("https://banghowdy.com/spendCoinAmountServerAPI.php?key=" + API_KEY + "&action=spend&username=" + _user.username+ "&amount=" + resId + "&description=" + desc);
            BufferedReader in = new BufferedReader(new InputStreamReader(data.openStream()));
            String line = in.readLine();
            switch (line) {
                case "FAILED":
                    return false;
                case "":
                    return false;
                default:
                    return _user.spendCoins(resId, desc);
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();

            return false;
        }
        /*//return _coinmgr.getCoinRepository().spendCoins(resId, getCoinType(), getCoinDescrip());
        if(_user.getCoins() < resId)
        {
            return false; // They don't have the money
        }
        _user.setCoins(_user.getCoins() - resId);

        return true;*/
    }

    /**
     * The type of purchases being made.
     */
    protected String getPurchaseType ()
    {
        return "generic";
    }

    /**
     * The type of good being purchased.
     */
    protected abstract String getGoodType ();

    protected void toString (StringBuffer buf)
    {
        buf.append("type=").append(getClass().getName());
        buf.append(", who=").append(_user.who());
        buf.append(", scrip=").append(_scripCost);
        buf.append(", coins=").append(_coinCost);
    }

    protected PlayerObject _user;
    protected int _scripCost, _coinCost;
    protected boolean _cashSpent, _actionTaken;
    protected String _failmsg;
    protected int _coinres = -1;

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected PlayerRepository _playrepo;

    protected static Map<String,String> _accountLock = Maps.newHashMap();
}
