package name.gumartinm.weather.information.widget;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class WidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        // For each widget that needs an update, get the text that we should display:
        //   - Create a RemoteViews object for it
        //   - Set the text in the RemoteViews object
        //   - Tell the AppWidgetManager to show that views object for the widget.
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
            int appWidgetId = appWidgetIds[i];
            // To prevent any ANR timeouts, we perform the update in a service
        	final Intent intent = new Intent(context.getApplicationContext(), WidgetIntentService.class);
        	intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra("refreshAppWidget", true);
            context.startService(intent);
        }
    }
    
    @Override
    public void onDeleted(final Context context, final int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i=0; i<N; i++) {
        	WidgetConfigure.deletePreference(context, appWidgetIds[i]);
            WidgetIntentService.deleteWidgetCurrentData(context, appWidgetIds[i]);
        }
    }

    public static void updateAppWidget(final Context context, final int appWidgetId) {

        final Intent intent = new Intent(context.getApplicationContext(), WidgetIntentService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("refreshAppWidget", false);
        context.startService(intent);
    }

    public static void refreshAllAppWidgets(final Context context) {
        final ComponentName widgets = new ComponentName(context.getApplicationContext(), WidgetProvider.class);
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context.getApplicationContext());

        final int[] appWidgetIds = appWidgetManager.getAppWidgetIds(widgets);
        for (final int appWidgetId : appWidgetIds) {
            refreshAppWidget(context.getApplicationContext(), appWidgetId);
        }
    }

    public static void refreshAppWidget(final Context context, final int appWidgetId) {

        final Intent intent = new Intent(context.getApplicationContext(), WidgetIntentService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.putExtra("refreshAppWidget", true);
        context.startService(intent);
    }
}
