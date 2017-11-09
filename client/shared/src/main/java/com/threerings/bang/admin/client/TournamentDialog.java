package com.threerings.bang.admin.client;

import com.jmex.bui.BContainer;
import com.jmex.bui.BDecoratedWindow;
import com.jmex.bui.BTextField;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.threerings.bang.data.BangCodes;
import com.threerings.bang.data.Handle;
import com.threerings.bang.saloon.client.SaloonService;
import com.threerings.bang.saloon.data.Criterion;
import com.threerings.bang.saloon.data.ParlorInfo;
import com.threerings.bang.util.BangContext;

public class TournamentDialog extends BDecoratedWindow implements ActionListener, SaloonService {



    BangContext context;


    /**
     * Creates a Tournament Dialog
     *
     * @param context the games context.
     */
    public TournamentDialog(BangContext context) {

        //Creates BDecoratedWindow with the context's Stylesheet
        super(context.getStyleSheet(), "Tournament");

        this.context = context;

        //Sets Group Layout
        ((GroupLayout)getLayoutManager()).setOffAxisPolicy(GroupLayout.STRETCH);

        //Group Layout Properties
        setModal(true);
        setLayer(BangCodes.NEVER_CLEAR_LAYER);
        //Padding Left
        setLayoutManager(GroupLayout.makeVert(GroupLayout.LEFT).setGap(5));
        //Standard Dialog Size: 75% of Display width 50% of Display height
        setSize((int)(context.getDisplay().getWidth() * .75), (int)(context.getDisplay().getHeight() * .5));

        BContainer container = new BContainer(GroupLayout.makeHStretch());
        container.setBounds(container.getX(), container.getY(), (int) (context.getDisplay().getWidth()  * .75), (int) (context.getDisplay().getHeight() * .5));
        BContainer optionsContainer = new BContainer();
        optionsContainer.add(new BTextField("Tournament Name: "));
        container.add(optionsContainer);
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
