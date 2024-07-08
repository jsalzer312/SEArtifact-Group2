package com.alexstyl.specialdates.events.namedays;

import com.alexstyl.specialdates.events.DayDate;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class NamedaysListTest {

    private static final String RECURRING_NAMEDAY = "recurring_nameday";
    private static final String FIXED_YEAR_NAMEDAY = "fixed_date_nameday";
    private static final int FIXED_YEAR = 2015;
    private static final int FIXED_MONTH = 1;

    private static final NamedaysList NAMEDAYS = new NamedaysList();

    @BeforeClass
    public static void setUp() {
        populateNamedays();
    }

    private static void populateNamedays() {
        NAMEDAYS.addNameday(DayDate.newInstance(1, FIXED_MONTH, FIXED_YEAR), FIXED_YEAR_NAMEDAY);
        NAMEDAYS.addNameday(DayDate.newInstance(2, FIXED_MONTH, FIXED_YEAR), FIXED_YEAR_NAMEDAY);
        NAMEDAYS.addNameday(DayDate.newInstance(3, FIXED_MONTH, FIXED_YEAR), FIXED_YEAR_NAMEDAY);

        NAMEDAYS.addNameday(DayDate.newInstance(4, FIXED_MONTH), RECURRING_NAMEDAY);
        NAMEDAYS.addNameday(DayDate.newInstance(5, FIXED_MONTH), RECURRING_NAMEDAY);

    }

    @Test
    public void whenNoYearSpecified_thenRecurringEventIsReturned() {
        DayDate dateWithNoYear = DayDate.newInstance(4, FIXED_MONTH);

        NamesInADate results = NAMEDAYS.getNamedaysFor(dateWithNoYear);

        assertThat(results.getNames().get(0)).isEqualTo(RECURRING_NAMEDAY);
    }

    @Test
    public void whenNoYearSpecified_thenFixedYearEventIsNotReturned() {
        DayDate dateWithNoYear = DayDate.newInstance(1, FIXED_MONTH);

        NamesInADate results = NAMEDAYS.getNamedaysFor(dateWithNoYear);

        assertThat(results.getNames()).isEmpty();
    }

    @Test
    public void whenYearSpecified_thenFixedYearEventIsReturned() {
        DayDate dateWithNoYear = DayDate.newInstance(1, FIXED_MONTH, FIXED_YEAR);

        NamesInADate results = NAMEDAYS.getNamedaysFor(dateWithNoYear);

        assertThat(results.getNames().get(0)).isEqualTo(FIXED_YEAR_NAMEDAY);
    }

    @Test
    public void whenYearSpecified_thenRecurringEventIsReturned() {
        DayDate dateWithNoYear = DayDate.newInstance(4, FIXED_MONTH, FIXED_YEAR);

        NamesInADate results = NAMEDAYS.getNamedaysFor(dateWithNoYear);

        assertThat(results.getNames().get(0)).isEqualTo(RECURRING_NAMEDAY);
    }

}
