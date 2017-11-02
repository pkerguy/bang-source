package com.threerings.bang.admin.client;

import com.jmex.bui.*;
import com.jmex.bui.event.ActionEvent;
import com.jmex.bui.event.ActionListener;
import com.jmex.bui.layout.GroupLayout;
import com.jmex.bui.util.Dimension;
import com.threerings.bang.data.Badge;

//Pretty sure we dont want a scrolling list in the end but can rework later to use dropdown.
public class BadgeList extends BScrollingList<BadgeList.EntryBuilder, BComponent> {

    private Badge selected;
    private BLabel selectedLabel;

    public BadgeList(Dimension size, BLabel selectedLabel) {
        super();

        this.selectedLabel = selectedLabel;
        setPreferredSize(size);

        for (Badge b : Badge.getAll())
            addValue(new EntryBuilder(b), false);

    }

    @Override // from BScrollingList
    protected BComponent createComponent(BadgeList.EntryBuilder entry) {
        return entry.build();
    }

    protected class EntryBuilder implements ActionListener {
        private Badge badge;

        public EntryBuilder(Badge currentBadge) {
            this.badge = currentBadge;
        }

        public BComponent build() {
            BContainer cont = GroupLayout.makeHBox(GroupLayout.CENTER);
            cont.add(new BButton(this.badge.getType().name(), this, String.valueOf(this.badge.getCode())));
            return cont;
        }


        @Override
        public void actionPerformed(ActionEvent event) {
            selected = badge;
            selectedLabel.setText(selected.getType().name());
        }

    }

    public Badge getSelected() {
        return selected;
    }
}