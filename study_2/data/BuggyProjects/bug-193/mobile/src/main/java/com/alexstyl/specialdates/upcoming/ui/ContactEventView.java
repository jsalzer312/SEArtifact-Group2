package com.alexstyl.specialdates.upcoming.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.contact.Birthday;
import com.alexstyl.specialdates.contact.Contact;
import com.alexstyl.specialdates.date.ContactEvent;
import com.alexstyl.specialdates.events.EventType;
import com.alexstyl.specialdates.images.ImageLoader;
import com.alexstyl.specialdates.ui.widget.ColorImageView;
import com.novoda.notils.caster.Views;

public class ContactEventView extends LinearLayout {

    private final TextView contactNameView;
    private final TextView eventTypeView;
    private final ColorImageView avatarView;
    private final Resources resources;

    public ContactEventView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater.from(context).inflate(R.layout.merge_upcoming_events_contact_card_view, this, true);

        contactNameView = Views.findById(this, R.id.upcoming_event_contact_card_contactname);
        eventTypeView = Views.findById(this, R.id.upcoming_event_contact_card_event_type);
        avatarView = Views.findById(this, R.id.upcoming_event_contact_card_avatar);

        setGravity(Gravity.CENTER_VERTICAL);
        resources = context.getResources();
    }

    public void displayEvent(ContactEvent event, ImageLoader imageLoader) {
        Contact contact = event.getContact();
        contactNameView.setText(contact.getDisplayName().toString());
        displayEventFor(event);
        avatarView.setBackgroundVariant(event.hashCode());
        avatarView.setLetter(event.getContact().getDisplayName().toString());

        imageLoader.loadThumbnail(contact.getImagePath(), avatarView.getImageView());
    }

    private void displayEventFor(ContactEvent event) {
        EventType eventType = event.getType();
        String eventLabel = getLabelFor(event);
        eventTypeView.setText(eventLabel);
        eventTypeView.setTextColor(getResources().getColor(eventType.getColorRes()));
    }

    private String getLabelFor(ContactEvent event) {
        EventType eventType = event.getType();
        if (eventType == EventType.BIRTHDAY) {
            Birthday birthday = event.getContact().getBirthday();
            if (birthday.includesYear()) {
                int age = birthday.getAgeOnYear(event.getYear());
                if (age > 0) {
                    return resources.getString(R.string.turns_age, age);
                }
            }
        }
        return resources.getString(eventType.nameRes());
    }

    public void setNameTypeface(Typeface typeface) {
        contactNameView.setTypeface(typeface);
    }
}
