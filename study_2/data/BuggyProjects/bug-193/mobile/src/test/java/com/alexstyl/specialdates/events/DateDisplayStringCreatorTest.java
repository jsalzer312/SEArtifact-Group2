package com.alexstyl.specialdates.events;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.assertThat;

public class DateDisplayStringCreatorTest {
    private static final String EXPECTED_STRING = "1995-05-05";
    private static final String EXPECTED_STRING_NO_YEAR = "--05-05";

    private static DateDisplayStringCreator creator;

    @BeforeClass
    public static void init() {
        creator = DateDisplayStringCreator.getInstance();
    }

    @Test
    public void givenDateWithYear_thenReturningStringIsCorrect() {
        DayDate date = DayDate.newInstance(5, 5, 1995);
        String dateToString = creator.stringOf(date);
        assertThat(dateToString).isEqualTo(EXPECTED_STRING);
    }

    @Test
    public void givenDateWithNoYear_thenReturningStringIsCorrect() {
        DayDate date = DayDate.newInstance(5, 5);
        String dateToString = creator.stringOf(date);
        assertThat(dateToString).isEqualTo(EXPECTED_STRING_NO_YEAR);
    }

}
