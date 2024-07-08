package com.alexstyl.specialdates.upcoming;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;

import com.alexstyl.specialdates.date.CelebrationDate;
import com.alexstyl.specialdates.date.ContactEvent;
import com.alexstyl.specialdates.events.DayDate;
import com.alexstyl.specialdates.events.bankholidays.BankHoliday;
import com.alexstyl.specialdates.events.bankholidays.BankholidayCalendar;
import com.alexstyl.specialdates.events.bankholidays.BankHolidaysPreferences;
import com.alexstyl.specialdates.events.bankholidays.GreekBankHolidays;
import com.alexstyl.specialdates.events.namedays.EasterCalculator;
import com.alexstyl.specialdates.events.namedays.NamedayCalendar;
import com.alexstyl.specialdates.events.namedays.NamedayCalendarProvider;
import com.alexstyl.specialdates.events.namedays.NamedayLocale;
import com.alexstyl.specialdates.events.namedays.NamedayPreferences;
import com.alexstyl.specialdates.events.namedays.NamesInADate;
import com.alexstyl.specialdates.service.PeopleEventsProvider;
import com.alexstyl.specialdates.ui.loader.SimpleAsyncTaskLoader;
import com.alexstyl.specialdates.util.ContactsObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UpcomingEventsLoader extends SimpleAsyncTaskLoader<List<CelebrationDate>> {

    private final PeopleEventsProvider peopleEventsProvider;
    private final NamedayPreferences namedayPreferences;
    private final ContactsObserver contactsObserver;
    private final LoadingTimeDuration timeDuration;
    private final EasterCalculator easterCalculator = new EasterCalculator();

    private final int currentYear;

    public UpcomingEventsLoader(Context context, PeopleEventsProvider peopleEventsProvider, LoadingTimeDuration timeDuration) {
        super(context);
        this.peopleEventsProvider = peopleEventsProvider;
        this.timeDuration = timeDuration;
        this.currentYear = timeDuration.getFrom().getYear();
        this.namedayPreferences = NamedayPreferences.newInstance(context);
        this.contactsObserver = new ContactsObserver(getContentResolver(), new Handler());
        setupContactsObserver();
    }

    @Override
    protected void onAbandon() {
        super.onAbandon();
        contactsObserver.unregister();
    }

    @Override
    public List<CelebrationDate> loadInBackground() {

        List<ContactEvent> contactEvents = peopleEventsProvider.getCelebrationDateFor(timeDuration);

        BankHolidaysPreferences bankHolidaysPreferences = BankHolidaysPreferences.newInstance(getContext());
        UpcomingDatesBuilder upcomingDatesBuilder = new UpcomingDatesBuilder()
                .withContactEvents(contactEvents);

        if (bankHolidaysPreferences.isEnabled()) {
            BankholidayCalendar.get();
            DayDate easter = easterCalculator.calculateEasterForYear(currentYear);
            List<BankHoliday> bankHolidays = new GreekBankHolidays(easter).getBankHolidaysForYear();
            upcomingDatesBuilder.withBankHolidays(bankHolidays);
        }

        if (includeNamedays()) {
            List<NamesInADate> namedays = getNamedaysFor(timeDuration);
            upcomingDatesBuilder.withNamedays(namedays);
        }

        List<CelebrationDate> allDates = upcomingDatesBuilder.build();

        Collections.sort(allDates);
        return allDates;
    }

    private boolean includeNamedays() {
        return namedayPreferences.isEnabled() && !namedayPreferences.isEnabledForContactsOnly();
    }

    private List<NamesInADate> getNamedaysFor(LoadingTimeDuration timeDuration) {
        NamedayLocale selectedLanguage = namedayPreferences.getSelectedLanguage();
        NamedayCalendarProvider namedayCalendarProvider = NamedayCalendarProvider.newInstance(getContext());
        NamedayCalendar namedayCalendar = namedayCalendarProvider.loadNamedayCalendarForLocale(selectedLanguage, currentYear);

        DayDate indexDate = timeDuration.getFrom();
        DayDate toDate = timeDuration.getTo();
        List<NamesInADate> namedays = new ArrayList<>();
        while (indexDate.isBefore(toDate)) {
            NamesInADate allNamedayOn = namedayCalendar.getAllNamedayOn(indexDate);
            if (allNamedayOn.nameCount() > 0) {
                namedays.add(allNamedayOn);
            }
            indexDate = indexDate.addDay(1);
        }
        return namedays;
    }

    private void setupContactsObserver() {
        contactsObserver.registerWith(
                new ContactsObserver.Callback() {
                    @Override
                    public void onContactsUpdated() {
                        onContentChanged();
                    }
                }
        );
    }

    public ContentResolver getContentResolver() {
        return getContext().getContentResolver();
    }

}
