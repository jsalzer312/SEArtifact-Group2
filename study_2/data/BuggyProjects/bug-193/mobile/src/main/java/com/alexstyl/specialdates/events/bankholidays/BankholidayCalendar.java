package com.alexstyl.specialdates.events.bankholidays;

import android.support.annotation.Nullable;

import com.alexstyl.specialdates.date.Date;
import com.alexstyl.specialdates.events.DayDate;
import com.alexstyl.specialdates.events.namedays.EasterCalculator;

public class BankholidayCalendar {

    private static final Object LOCK = new Object();

    private static BankholidayCalendar INSTANCE;
    private final BankHolidayRepository repository;

    private BankholidayCalendar(BankHolidayRepository repository) {
        this.repository = repository;
    }

    public static BankholidayCalendar get() {
        if (INSTANCE == null) {
            INSTANCE = new BankholidayCalendar(new BankHolidayRepository(new EasterCalculator()));
            INSTANCE.initialise();
        }
        return INSTANCE;
    }

    private void initialise() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (LOCK) {
                    int year = DayDate.today().getYear();
                    repository.preloadHolidaysForYear(year);
                }
            }
        });
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
    }

    @Nullable
    public BankHoliday getBankholidayFor(Date date) {
        synchronized (LOCK) {
            return repository.calculateBankholidayFor(date);
        }
    }
}
