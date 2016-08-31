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
package name.gumartinm.weather.information.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import com.google.android.gms.common.GooglePlayServicesUtil;
import name.gumartinm.weather.information.R;
import timber.log.Timber;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class LicensesActivity extends AppCompatActivity {
    private WebView mWebView;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_launcher);

        setContentView(R.layout.weather_licenses);

        mWebView = (WebView) this.findViewById(R.id.weather_licenses);
    }

    @Override
    public void onResume() {
        super.onResume();

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setTitle(this.getString(R.string.weather_licenses_title));

        final String googlePlayServices = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this.getApplicationContext());
        try {
            final StringBuilder stringBuilder = this.loadData();
            stringBuilder.append(googlePlayServices).append("</pre>").append("</body>").append("</html>");
            mWebView.loadDataWithBaseURL(null, stringBuilder.toString(), "text/html", "UTF-8", null);
        } catch (final UnsupportedEncodingException e) {
            Timber.e(e, "Load data error");
        } catch (final IOException e) {
            Timber.e(e, "Load data error");
        }
    }

    private StringBuilder loadData() throws UnsupportedEncodingException, IOException {
        final InputStream inputStream = this.getResources().openRawResource(R.raw.licenses);
        try {
            final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            try {
                final BufferedReader reader = new BufferedReader(inputStreamReader);
                try {
                    final StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                        stringBuilder.append("\n");
                    }
                    return stringBuilder;
                } finally {
                    reader.close();
                }
            } finally {
                inputStreamReader.close();
            }
        } finally {
            inputStream.close();
        }
    }
}
