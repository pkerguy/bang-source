//
// $Id$

package com.threerings.bang.gang.server;

import com.threerings.bang.data.BucklePart;
import com.threerings.bang.data.Handle;
import com.threerings.bang.gang.data.GangGood;
import com.threerings.bang.gang.data.HideoutMarshaller;
import com.threerings.bang.gang.data.OutfitArticle;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link HideoutProvider}.
 */
public class HideoutDispatcher extends InvocationDispatcher<HideoutMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public HideoutDispatcher (HideoutProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public HideoutMarshaller createMarshaller ()
    {
        return new HideoutMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case HideoutMarshaller.ADD_TO_COFFERS:
            ((HideoutProvider)provider).addToCoffers(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.BROADCAST_TO_MEMBERS:
            ((HideoutProvider)provider).broadcastToMembers(
                source, (String)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case HideoutMarshaller.BUY_GANG_GOOD:
            ((HideoutProvider)provider).buyGangGood(
                source, (String)args[0], (Object[])args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.BUY_OUTFITS:
            ((HideoutProvider)provider).buyOutfits(
                source, (OutfitArticle[])args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case HideoutMarshaller.CHANGE_MEMBER_RANK:
            ((HideoutProvider)provider).changeMemberRank(
                source, (Handle)args[0], ((Byte)args[1]).byteValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.CHANGE_MEMBER_TITLE:
            ((HideoutProvider)provider).changeMemberTitle(
                source, (Handle)args[0], ((Integer)args[1]).intValue(), (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.EXPEL_MEMBER:
            ((HideoutProvider)provider).expelMember(
                source, (Handle)args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case HideoutMarshaller.FIND_MATCH:
            ((HideoutProvider)provider).findMatch(
                source, (Criterion)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case HideoutMarshaller.FORM_GANG:
            ((HideoutProvider)provider).formGang(
                source, (Handle)args[0], (String)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.GET_HISTORY_ENTRIES:
            ((HideoutProvider)provider).getHistoryEntries(
                source, ((Integer)args[0]).intValue(), (String)args[1], (InvocationService.ResultListener)args[2]
            );
            return;

        case HideoutMarshaller.GET_OUTFIT_QUOTE:
            ((HideoutProvider)provider).getOutfitQuote(
                source, (OutfitArticle[])args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case HideoutMarshaller.GET_UPGRADE_QUOTE:
            ((HideoutProvider)provider).getUpgradeQuote(
                source, (GangGood)args[0], (InvocationService.ResultListener)args[1]
            );
            return;

        case HideoutMarshaller.LEAVE_GANG:
            ((HideoutProvider)provider).leaveGang(
                source, (InvocationService.ConfirmListener)args[0]
            );
            return;

        case HideoutMarshaller.LEAVE_MATCH:
            ((HideoutProvider)provider).leaveMatch(
                source, ((Integer)args[0]).intValue()
            );
            return;

        case HideoutMarshaller.POST_OFFER:
            ((HideoutProvider)provider).postOffer(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (InvocationService.ResultListener)args[2]
            );
            return;

        case HideoutMarshaller.RENEW_GANG_ITEM:
            ((HideoutProvider)provider).renewGangItem(
                source, ((Integer)args[0]).intValue(), (InvocationService.ConfirmListener)args[1]
            );
            return;

        case HideoutMarshaller.RENT_GANG_GOOD:
            ((HideoutProvider)provider).rentGangGood(
                source, (String)args[0], (Object[])args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        case HideoutMarshaller.SET_BUCKLE:
            ((HideoutProvider)provider).setBuckle(
                source, (BucklePart[])args[0], (InvocationService.ConfirmListener)args[1]
            );
            return;

        case HideoutMarshaller.SET_STATEMENT:
            ((HideoutProvider)provider).setStatement(
                source, (String)args[0], (String)args[1], (InvocationService.ConfirmListener)args[2]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}