/**
 * Copyright 2014 Gustavo Martin Morcuende
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package name.gumartinm.weather.information.app;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import name.gumartinm.weather.information.BuildConfig;
import name.gumartinm.weather.information.R;
import timber.log.Timber;

public class WeatherInformationApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG_MODE) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new GoogleAnalyticsReportingTree(
                    new GoogleAnalyticsTrackers(this.getApplicationContext())));
        }
    }

    private static class GoogleAnalyticsTrackers {

        private enum TrackerName {
            EXCEPTIONS_TRACKER, // Tracker used when logging caught exceptions in this app.
        };

        private final HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
        private final Context appContext;

        private GoogleAnalyticsTrackers(final Context appContext) {
            this.appContext = appContext;
        }

        /**
         * Get tracker
         * @param trackerId
         * @return Tracker
         */
        private synchronized Tracker getTracker(final TrackerName trackerId) {
            if (!mTrackers.containsKey(trackerId)) {

                final GoogleAnalytics analytics = GoogleAnalytics.getInstance(appContext.getApplicationContext());

                final Tracker t = (trackerId == TrackerName.EXCEPTIONS_TRACKER) ?
                        analytics.newTracker(R.xml.exceptions_tracker) :
                        analytics.newTracker(R.xml.exceptions_tracker);

                // Do not retrieve user's information. I strongly care about user's privacy.
                t.enableAdvertisingIdCollection(false);

                mTrackers.put(trackerId, t);
            }
            return mTrackers.get(trackerId);
        }

        /**
         * Send exception
         * @param exception
         * @param trackerName
         */
        private void send(final Throwable exception, final TrackerName trackerName) {
            final Tracker tracker = this.getTracker(trackerName);

            // Build and send exception.
            tracker.send(new HitBuilders.ExceptionBuilder()
                    .setDescription(
                            new StandardExceptionParser(appContext.getApplicationContext(), null)
                                    .getDescription(Thread.currentThread().getName(), exception))
                    .setFatal(false)
                    .build());
        }
    }

    private static class GoogleAnalyticsReportingTree extends Timber.HollowTree {
        private final GoogleAnalyticsTrackers analyticsTrackers;

        private GoogleAnalyticsReportingTree(final GoogleAnalyticsTrackers analyticsTrackers) {
            this.analyticsTrackers = analyticsTrackers;
        }

        @Override
        public void i(final String message, final Object... args) {
            // Do nothing, just report exceptions.
        }

        @Override
        public void i(final Throwable t, final String message, final Object... args) {
            i(message, args); // Just add to the log.
        }

        @Override
        public void e(final String message, final Object... args) {
            i("ERROR: " + message, args); // Just add to the log.
        }

        @Override
        public void e(final Throwable exception, final String message, final Object... args) {
            e(message, args);

            this.analyticsTrackers.send(exception, GoogleAnalyticsTrackers.TrackerName.EXCEPTIONS_TRACKER);
        }
    }
}
