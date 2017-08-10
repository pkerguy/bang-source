package com.threerings.bang.steam;

import com.codedisaster.steamworks.*;

import java.util.Collection;

public class SteamWorkshop {

    private static final SteamWorkshop instance = new SteamWorkshop();

    public static SteamWorkshop get() {
        return instance;
    }

    private final SteamUGC _ugc = new SteamUGC(new Callback());

    private SteamWorkshop() {
        /* Example */

        SteamWorkshop workshop = SteamWorkshop.get();
        for (SteamItem item : workshop.getSubscribedItems()) {
            Collection<SteamUGC.ItemState> state = item.getItemState();
            if (state.contains(SteamUGC.ItemState.Installed)) {
                // Item is installed :D. No clue what item it is tho
            }
        }

        /*
            None
            Subscribed
            LegacyItem
            Installed
            NeedsUpdate
            Downloading
            DownloadPending
         */
    }

    public SteamItem[] getSubscribedItems() {
        SteamPublishedFileID[] ids = new SteamPublishedFileID[_ugc.getNumSubscribedItems()];
        int count = _ugc.getSubscribedItems(ids);
        SteamItem[] items = new SteamItem[count];
        for (int i = 0; i < count; ) {
            items[i] = new SteamItem(ids[i++]);
        }
        return items;
    }

    public class SteamItem {

        private final SteamPublishedFileID _id;

        public SteamItem(SteamPublishedFileID id) {
            _id = id;
        }

        public Collection<SteamUGC.ItemState> getItemState() {
            return _ugc.getItemState(_id);
        }
    }

    /* That's a lot of methods. Don't think any functionality needs to be added, but currently unsure if null can be used instead of an instance */

    private class Callback implements SteamUtilsCallback, SteamUGCCallback {

        /* SteamUtilsCallback */

        @Override
        public void onSteamShutdown() {
        }

        /* SteamUGCCallback */

        @Override
        public void onUGCQueryCompleted(SteamUGCQuery query, int numResultsReturned, int totalMatchingResults, boolean isCachedData, SteamResult result) {
        }

        @Override
        public void onSubscribeItem(SteamPublishedFileID publishedFileID, SteamResult result) {
        }

        @Override
        public void onUnsubscribeItem(SteamPublishedFileID publishedFileID, SteamResult result) {
        }

        @Override
        public void onRequestUGCDetails(SteamUGCDetails details, SteamResult result) {
        }

        @Override
        public void onCreateItem(SteamPublishedFileID publishedFileID, boolean needsToAcceptWLA, SteamResult result) {
        }

        @Override
        public void onSubmitItemUpdate(boolean needsToAcceptWLA, SteamResult result) {
        }

        @Override
        public void onDownloadItemResult(int appID, SteamPublishedFileID publishedFileID, SteamResult result) {
        }

        @Override
        public void onUserFavoriteItemsListChanged(SteamPublishedFileID publishedFileID, boolean wasAddRequest, SteamResult result) {
        }

        @Override
        public void onSetUserItemVote(SteamPublishedFileID publishedFileID, boolean voteUp, SteamResult result) {
        }

        @Override
        public void onGetUserItemVote(SteamPublishedFileID publishedFileID, boolean votedUp, boolean votedDown, boolean voteSkipped, SteamResult result) {
        }

        @Override
        public void onStartPlaytimeTracking(SteamResult result) {
        }

        @Override
        public void onStopPlaytimeTracking(SteamResult result) {
        }

        @Override
        public void onStopPlaytimeTrackingForAllItems(SteamResult result) {
        }
    }
}
