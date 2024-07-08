package com.alexstyl.specialdates.widgetprovider;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.date.DateFormatUtils;
import com.alexstyl.specialdates.datedetails.DateDetailsActivity;
import com.alexstyl.specialdates.events.ContactEvents;
import com.alexstyl.specialdates.events.DayDate;
import com.alexstyl.specialdates.service.PeopleEventsProvider;
import com.alexstyl.specialdates.ui.activity.MainActivity;
import com.alexstyl.specialdates.util.NaturalLanguageUtils;

public class TodayWidgetProvider extends AppWidgetProvider {

    private WidgetImageLoader imageLoader;

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        updateWidgets(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (imageLoader == null) {
            imageLoader = WidgetImageLoader.newInstance(context.getResources(), AppWidgetManager.getInstance(context));
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {

        new QueryUpcomingTask(PeopleEventsProvider.newInstance(context)) {
            @Override
            void onLoaded(ContactEvents contactEvents) {
                if (contactEvents.size() > 0) {
                    updateForDate(context, appWidgetManager, appWidgetIds, contactEvents);
                } else {
                    onUpdateNoEventsFound(context, appWidgetManager, appWidgetIds);
                }
            }
        }.execute();
    }

    private void updateForDate(Context context, final AppWidgetManager appWidgetManager, int[] appWidgetIds, ContactEvents contactEvents) {
        DayDate date = contactEvents.getDate();
        Intent intent = DateDetailsActivity.getStartIntent(context, date.getDayOfMonth(), date.getMonth(), date.getYear());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        );

        String title = toString(context, date);

        final int N = appWidgetIds.length;

        String label = NaturalLanguageUtils.joinContacts(context, contactEvents.getContacts(), 2);

        UpcomingWidgetPreferences preferences = new UpcomingWidgetPreferences(context);
        WidgetVariant selectedVariant = preferences.getSelectedVariant();
        TransparencyColorCalculator transparencyColorCalculator = new TransparencyColorCalculator();
        float opacity = preferences.getOppacityLevel(); //TODO
        int selectedTextColor = context.getResources().getColor(selectedVariant.getTextColor());

        WidgetColorCalculator calculator = new WidgetColorCalculator(selectedTextColor);
        int finalHeaderColor = calculator.getColor(DayDate.today(), date);
        for (int i = 0; i < N; i++) {
            final int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget_upcoming);

            remoteViews.setTextViewText(R.id.upcoming_widget_header, title);
            remoteViews.setTextViewText(R.id.upcoming_widget_events_text, label);

            remoteViews.setTextColor(R.id.upcoming_widget_events_text, selectedTextColor);
            remoteViews.setTextColor(R.id.upcoming_widget_header, finalHeaderColor);

            int background = context.getResources().getColor(selectedVariant.getBackgroundColorResId());
            int backgroundColor = transparencyColorCalculator.calculateColor(background, opacity);
            remoteViews.setInt(R.id.upcoming_widget_background_image, "setBackgroundColor", backgroundColor);

            remoteViews.setOnClickPendingIntent(R.id.upcoming_widget_background, pendingIntent);
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

            imageLoader.loadPicture(contactEvents.getContacts(), appWidgetId, remoteViews);
        }
    }

    private String toString(Context context, DayDate todayDate) {
        return DateFormatUtils.formatTimeStampString(context, todayDate.toMillis(), false, true);
    }

    public void onUpdateNoEventsFound(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        final int N = appWidgetIds.length;

        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];

            // Get the layout for the App Widget and attach an on-click listener
            // to the button
            RemoteViews views = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_simple_nocontacts
            );
            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
            );

            views.setTextViewText(android.R.id.text1, context.getString(R.string.today_widget_empty));
            views.setOnClickPendingIntent(R.id.upcoming_widget_background, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    public static void updateWidgets(Context context) {
        Intent intent = new Intent(context, TodayWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        int ids[] = AppWidgetManager.getInstance(context).getAppWidgetIds(
                new ComponentName(context, TodayWidgetProvider.class)
        );

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

}
