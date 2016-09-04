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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import name.gumartinm.weather.information.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.weather_about);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.weather_main_toolbar);
        this.setSupportActionBar(toolbar);

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_launcher);
    }

    @Override
    public void onResume() {
        super.onResume();

        final ActionBar actionBar = this.getSupportActionBar();
        actionBar.setTitle(this.getString(R.string.weather_about_action));
    }

    public void onClickLegalInformation(final View view) {
        final Intent intent = new Intent(this.getApplicationContext(), LicensesActivity.class);
        this.startActivity(intent);
    }

    public void onClickSourceCode(final View view) {
        final String url = this.getString(R.string.application_source_code_url);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public void onClickRemoteData(final View view) {
        final String url = this.getString(R.string.openweathermap_url);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    public void onClickMyWeb(final View view) {
        final String url = this.getString(R.string.my_url);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
