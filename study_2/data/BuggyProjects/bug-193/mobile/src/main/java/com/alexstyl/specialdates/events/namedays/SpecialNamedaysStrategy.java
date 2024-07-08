package com.alexstyl.specialdates.events.namedays;

import com.alexstyl.specialdates.events.DayDate;
import com.alexstyl.specialdates.namedays.NameCelebrations;

import java.util.ArrayList;

public interface SpecialNamedaysStrategy {

    NamesInADate getNamedayByDate(DayDate date);

    ArrayList<String> getAllNames();

    NameCelebrations getNamedaysFor(String name, int year);
}
