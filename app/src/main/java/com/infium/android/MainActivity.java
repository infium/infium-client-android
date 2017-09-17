/*
 * Copyright 2012-2017 Infium AB
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

package com.infium.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public final String LOG_TAG = MainActivity.class.getSimpleName();
    public final String DEFAULT_URL = "https://infium-eu.appspot.com/api/";
    public final static String DEFAULT_COLOR = "#923E9E";
    public final static String EXTRA_METHOD = "com.infium.android.METHOD";
    public final static String EXTRA_URL = "com.infium.android.URL";
    public final static String EXTRA_TITLE_BAR_COLOR = "com.infium.android.TITLE_BAR_COLOR";
    public final static String EXTRA_COMPANY = "com.infium.android.COMPANY";
    public final static String EXTRA_BODY = "com.infium.android.BODY";
    public final static String EXTRA_BASEURL = "com.infium.android.BASEURL";
    private static String uniqueID = null;
    private static final String PREF_UNIQUE_ID = "com.infium.android.PREF_UNIQUE_ID";

    public synchronized static String id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null){
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(DEFAULT_COLOR)));
        }
        setTitle("Welcome");
        redrawButtons();
        getSupportActionBar().setElevation(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        redrawButtons();
    }

    private void redrawButtons(){
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");
        String url = prefs.getString("url", "");

        if (token.equals("")){
            EditText urlField = findViewById(R.id.url);
            EditText usernameField = findViewById(R.id.username);
            EditText passwordField = findViewById(R.id.password);

            TextView sessionActiveText = findViewById(R.id.sessionActiveText);
            TextView versionText = findViewById(R.id.versionText);

            Button loginLogoutButton = findViewById(R.id.loginLogoutButton);
            Button menuButton = findViewById(R.id.menu);

            loginLogoutButton.setText("Login");
            versionText.setText("v " + BuildConfig.VERSION_NAME);

            urlField.setVisibility(View.VISIBLE);
            usernameField.setVisibility(View.VISIBLE);
            passwordField.setVisibility(View.VISIBLE);
            versionText.setVisibility(View.VISIBLE);

            sessionActiveText.setVisibility(View.GONE);
            menuButton.setVisibility(View.GONE);

            urlField.setEnabled(true);
            usernameField.setEnabled(true);
            passwordField.setEnabled(true);

            if (url.equals("")){
                urlField.setText(DEFAULT_URL);
            }else{
                urlField.setText(url);
            }
        }else{
            EditText urlField = findViewById(R.id.url);
            EditText usernameField = findViewById(R.id.username);
            EditText passwordField = findViewById(R.id.password);

            TextView sessionActiveText = findViewById(R.id.sessionActiveText);
            TextView versionText = findViewById(R.id.versionText);

            Button loginLogoutButton = findViewById(R.id.loginLogoutButton);
            Button menuButton = findViewById(R.id.menu);

            try{
                loginLogoutButton.setText("Logout");
                versionText.setText("v " + BuildConfig.VERSION_NAME);

                urlField.setVisibility(View.GONE);
                usernameField.setVisibility(View.GONE);
                passwordField.setVisibility(View.GONE);

                versionText.setVisibility(View.VISIBLE);

                sessionActiveText.setVisibility(View.VISIBLE);

                menuButton.setVisibility(View.VISIBLE);

                urlField.setEnabled(false);
                usernameField.setEnabled(false);
                passwordField.setEnabled(false);

            }catch (Exception e){
                Log.e(LOG_TAG, "", e);
            }
        }

        if (token.equals("")){
            findViewById(R.id.username).requestFocus();
        }else{
            findViewById(R.id.username).clearFocus();
            findViewById(R.id.password).clearFocus();
        }
    }

    public void menuButtonTapped(View v) {
        goToMenu();
    }

    public void goToMenu(){
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String company = prefs.getString("company", null);
        String url = prefs.getString("url", null);

        Intent intent = new Intent(getApplicationContext(), DetailedActivity.class);
        intent.putExtra(EXTRA_METHOD, "GET");
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_COMPANY, company);
        intent.putExtra(EXTRA_BODY, "");
        intent.putExtra(EXTRA_BASEURL, url);
        intent.putExtra(EXTRA_TITLE_BAR_COLOR, DEFAULT_COLOR);
        startActivity(intent);
    }

    public void loginLogoutButtonTapped(View v) {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String token = prefs.getString("token", "");

        if (token.equals("")){
            loginButtonTapped();
        }else{
            logoutButtonTapped();
        }
    }

    private void logoutButtonTapped(){
        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String company = prefs.getString("company", null);
        String url = prefs.getString("url", null);
        String token = prefs.getString("token", "");

        new Logout().execute(url, company, token);
    }

    private void loginButtonTapped(){
        EditText urlField = findViewById(R.id.url);
        EditText usernameField = findViewById(R.id.username);
        EditText passwordField = findViewById(R.id.password);

        String url = urlField.getText().toString();
        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        String[] strings = username.split("@");

        if (strings.length < 2){
            Toast.makeText(getApplicationContext(), "The username must be in the format 'name@123456'", Toast.LENGTH_LONG).show();
        }else{
            if (strings.length > 2){
                if (strings[2].substring(0,8).equals("https://")){
                    new Login().execute(strings[2], strings[1], strings[0], password);
                }else{
                    if ((strings.length > 3)&&(strings[3].equals("NOSSL"))){
                        new Login().execute(strings[2], strings[1], strings[0], password);
                    }else{
                        Toast.makeText(getApplicationContext(), "The server URL must start with 'https://'", Toast.LENGTH_LONG).show();
                    }
                }
            }else{
                new Login().execute(url, strings[1], strings[0], password);
            }
        }
    }

    private class Login extends AsyncTask<Object, Void, String> {
        protected String doInBackground(Object... params) {
            SharedPreferences.Editor editor2 = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit();
            editor2.putString("url", (String)params[0]);
            editor2.putString("company", (String)params[1]);
            editor2.apply();

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String output = null;

            try {
                URL url = new URL(params[0] + "login/");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("X-Client-Platform", "Android");
                urlConnection.setRequestProperty("X-Client-Platform-Version", Build.VERSION.RELEASE);
                urlConnection.setRequestProperty("X-Client-Platform-Device", Build.MANUFACTURER + " " + Build.MODEL);
                urlConnection.setRequestProperty("X-Client-Platform-Language", Locale.getDefault().getLanguage());
                urlConnection.setRequestProperty("X-Client-App-Version", BuildConfig.VERSION_NAME);
                urlConnection.setRequestProperty("X-Client-Login-Company", (String)params[1]);

                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                DataOutputStream wr = new DataOutputStream (urlConnection.getOutputStream ());

                String jsonString = null;

                String installationId = id(getApplicationContext());

                try{
                    JSONObject obj = new JSONObject();
                    obj.put("Username", params[2]);
                    obj.put("Password", params[3]);
                    obj.put("InstallationId", installationId);
                    jsonString = obj.toString();

                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error", e);
                }

                if (jsonString != null){
                    wr.writeBytes(jsonString);
                }
                wr.flush();
                wr.close ();

                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return "";
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return "";
                }
                output = buffer.toString();
                try {
                    JSONObject outputJson = new JSONObject(output);
                    JSONObject jsonData = outputJson.getJSONObject("Data");
                    String token = jsonData.getString("Token");

                    SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit();
                    editor.putString("token", token);
                    editor.apply();
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "Could not get the new token, could be the wrong username, password or a server error", e);

                    SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit();
                    editor.putString("token", null);
                    editor.apply();
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
                return "";
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "", e);
                    }
                }
            }
            return output;
        }

        protected void onPostExecute(String result) {
            SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            String token = prefs.getString("token", null);

            if (token != null){
                EditText usernameField = findViewById(R.id.username);
                EditText passwordField = findViewById(R.id.password);

                usernameField.setText("");
                passwordField.setText("");

                goToMenu();
            }else{
                Toast.makeText(getApplicationContext(), "Wrong username and/or password", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class Logout extends AsyncTask<Object, Void, JSONObject> {
        protected JSONObject doInBackground(Object... params) {
            String urlParam = (String)params[0];
            String companyParam = (String)params[1];
            String tokenParam = (String)params[2];

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String output = null;
            try {
                URL url = new URL(urlParam + "logout/");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty("X-Client-Platform", "Android");
                urlConnection.setRequestProperty("X-Client-Platform-Version", Build.VERSION.RELEASE);
                urlConnection.setRequestProperty("X-Client-Platform-Device", Build.MANUFACTURER + " " + Build.MODEL);
                urlConnection.setRequestProperty("X-Client-Platform-Language", Locale.getDefault().getLanguage());
                urlConnection.setRequestProperty("X-Client-App-Version", BuildConfig.VERSION_NAME);
                urlConnection.setRequestProperty("X-Client-Login-Token", tokenParam);
                urlConnection.setRequestProperty("X-Client-Login-Company", companyParam);

                urlConnection.setDoOutput(true);
                urlConnection.setChunkedStreamingMode(0);

                DataOutputStream wr = new DataOutputStream (urlConnection.getOutputStream ());
                wr.writeBytes("");
                wr.flush();
                wr.close ();

                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                if (buffer.length() == 0) {
                    return null;
                }
                output = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "", e);
                    }
                }
            }

            JSONObject outputObject = new JSONObject();

            try{
                outputObject.put("Output", output);
            }catch (JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }

            return outputObject;
        }

        protected void onPostExecute(JSONObject outputObject) {

            SharedPreferences.Editor editor = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE).edit();
            editor.putString("token", null);
            editor.apply();

            redrawButtons();
        }
    }
}