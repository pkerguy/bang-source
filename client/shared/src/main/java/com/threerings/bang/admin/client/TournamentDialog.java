//Created By SecondAmendment (Samuel) on 11/8/2017
//Work in Progress...

package com.threerings.bang.admin.client;

import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.threerings.bang.client.bui.ServiceButton;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.saloon.client.ParlorConfigView;
import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.saloon.data.SaloonCodes;
import com.threerings.bang.saloon.data.SaloonObject;
import com.threerings.bang.util.BangContext;
import com.threerings.util.MessageBundle;

public class TournamentDialog extends BDecoratedWindow implements ActionListener, SaloonService {

    //TODO: *MAKE TOURNAMENT PARLOR TYPE, *ENCORPORATE TOURNAMENTLIST, ADDING AND REMOVING PLAYERS FROM BRACKETS

    protected BangContext context;
    protected BLabel tnLabel, tnSizeLabel, passwordLabel, matched;
    protected BTextField tnInput, tnSizeInput, password;
    protected BCheckBox matchedCB;
    protected MessageBundle msgs;
    protected SaloonObject saloon;


    /**
     * Creates a Tournament Dialog
     *
     * @param context the games context.
     * @param saloon saloon instance that the user is currently in
     */
    public TournamentDialog(BangContext context, SaloonObject saloon) {

        //Creates BDecoratedWindow with the context's Stylesheet
        super(context.getStyleSheet(), "Tournament");

        msgs = context.getMessageManager().getBundle(SaloonCodes.SALOON_MSGS);
        this.saloon = saloon;

        BContainer params = new BContainer(
                GroupLayout.makeVert(GroupLayout.NONE, GroupLayout.TOP, GroupLayout.STRETCH));
        add(params);

        BContainer row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(tnLabel = new BLabel("Name:"));
        tnLabel.setTooltipText("Sets the Tournament Name");
        row.add(tnInput = new BTextField(50));
        tnInput.setPreferredWidth(100);
        tnInput.setEnabled(true);
        params.add(row);

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(tnSizeLabel = new BLabel("Size:"));
        tnSizeLabel.setTooltipText("Amount of players the Tournament will hold");
        row.add(tnSizeInput = new BTextField(50));
        tnSizeInput.setPreferredWidth(75);
        tnSizeInput.setEnabled(true);
        params.add(row);

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(passwordLabel = new BLabel(msgs.get("m.use_password")));
        passwordLabel.setTooltipText("Password");
        row.add(password = new BTextField(50));
        password.setPreferredWidth(75);
        password.setEnabled(false);
        params.add(row);

        /*

        row = GroupLayout.makeHBox(GroupLayout.LEFT);
        row.add(matched = new BLabel(msgs.get("m.use_matched")));
        matched.setTooltipText(msgs.get("m.parlor_matched_tip"));
        row.add(matchedCB = new BCheckBox(null));
        matchedCB.setSelected(false);
        params.add(row);

        */

        BContainer buttons = GroupLayout.makeHBox(GroupLayout.CENTER);
        buttons.add(new ServiceButton(context, msgs.get("m.create"),
                "Tournament Creation Failed!") {
            protected boolean callService () {
              // ParlorInfo.Type type = ParlorInfo.Type.TOURNAMENT;
              // saloon.service.createParlor(
                   //   ParlorInfo.Type.TOURNAMENT, password.getText(), matchedCB.isSelected(), createResultListener());
                return true;
            }
            protected boolean onSuccess (Object result) {
                context.getLocationDirector().moveTo((Integer)result);

                return false;
            }
        });
        buttons.add(new BButton(msgs.get("m.cancel")).
                addListener(context.getBangClient().makePopupClearer(this, true)));
        add(buttons, GroupLayout.FIXED);
    }

    @Override
    public void actionPerformed(ActionEvent event) {

    }

    @Override
    public void findMatch(Criterion criterion, ResultListener listener) {

    }

    @Override
    public void leaveMatch(int matchOid) {
        //Remove Users from the tournament Bracket
    }

    @Override
    public void createParlor(ParlorInfo.Type type, String password, boolean matched, ResultListener rl) {

        //Create a Parlor for the Tournament

    }

    @Override
    public void joinParlor(Handle creator, String password, ResultListener rl) {

        //Place Users who join the parlor in a Bracket

    }
}
