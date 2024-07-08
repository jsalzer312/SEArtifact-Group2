package com.alexstyl.specialdates.upcoming;

import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.ui.base.MementoActivity;

class GoToTodayEnabler {

    private final MementoActivity listener;

    private MenuItem goToToday;
    private boolean showToday;

    public GoToTodayEnabler(MementoActivity listener) {
        this.listener = listener;
    }

    public void reattachTo(Menu menu) {
        this.goToToday = menu.findItem(R.id.action_today);
        this.goToToday.setEnabled(showToday);
        this.goToToday.setVisible(showToday);
    }

    void validateGoToTodayButton(final RecyclerView recyclerView) {
        recyclerView.post(
                new Runnable() {
                    public void run() {
                        boolean showTodayNow = canListScroll(recyclerView);
                        if (showToday != showTodayNow) {
                            showToday = showTodayNow;
                            listener.supportInvalidateOptionsMenu();
                        }
                    }
                }
        );
    }

    private boolean canListScroll(RecyclerView recyclerView) {
        return ViewCompat.canScrollVertically(recyclerView, ViewCompat.LAYOUT_DIRECTION_LTR);
    }

    public interface Listener {

        void onGoToTodayUpdated();
    }
}
