package com.alexstyl.specialdates.events;

import android.content.Context;
import android.net.Uri;
import android.view.View;

import com.alexstyl.specialdates.DisplayName;
import com.alexstyl.specialdates.contact.Contact;
import com.alexstyl.specialdates.contact.actions.LabeledAction;

import java.util.List;

class TestContact extends Contact {

    public TestContact(long id, DisplayName displayName) {
        super(id, displayName);
    }

    @Override
    protected List<LabeledAction> onBuildActions(Context context) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public Uri getLookupUri() {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void displayQuickInfo(Context context, View view) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public String getImagePath() {
        throw new UnsupportedOperationException("Not supported");
    }
}
