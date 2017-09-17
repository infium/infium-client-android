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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import org.json.JSONArray;
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

public class SearchSelectionActivity extends AppCompatActivity {

    public final String LOG_TAG = SearchSelectionActivity.class.getSimpleName();
    public final static String EXTRA_NAME = "com.infium.android.NAME";
    public final static String EXTRA_POSITION = "com.infium.android.POSITION";
    public final static String EXTRA_TITLE = "com.infium.android.TITLE";
    public final static String EXTRA_VALUE = "com.infium.android.VALUE";
    public final static String EXTRA_SEARCH_URL = "com.infium.android.SEARCH_URL";
    public final static String EXTRA_TABLE_PARENT = "com.infium.android.TABLE_PARENT";
    public final static String EXTRA_TABLE_ROW_INDEX = "com.infium.android.TABLE_ROW_INDEX";
    private String name = null;
    private int position;
    private String defaultValue = null;
    private String token = null;
    private String company = null;
    private String searchUrl = null;
    private int searchSerialNumber = 0;
    private JSONArray searchResults = null;
    private String tableParent = null;
    private int tableRowIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null){
            getSupportActionBar().setElevation(0);
        }

        setContentView(R.layout.activity_flow_ui_search_selection);

        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        token = prefs.getString("token", null);
        company = prefs.getString("company", null);

        Intent intent = getIntent();
        name = intent.getStringExtra(EXTRA_NAME);
        position = intent.getIntExtra(EXTRA_POSITION, -1);
        defaultValue = intent.getStringExtra(EXTRA_VALUE);
        searchUrl = intent.getStringExtra(EXTRA_SEARCH_URL);

        tableParent = intent.getStringExtra(EXTRA_TABLE_PARENT);
        tableRowIndex = intent.getIntExtra(EXTRA_TABLE_ROW_INDEX, -1);

        setTitle(intent.getStringExtra(EXTRA_TITLE));

        EditText editText = (EditText) findViewById(R.id.searchInput);

        editText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                search(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        if (savedInstanceState != null) {
            searchSerialNumber = savedInstanceState.getInt("searchSerialNumber");
            try{
                searchResults = new JSONArray(savedInstanceState.getString("searchResults"));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "", e);
            }

            if (searchResults != null){
                rebuildLayout();
            }
        }

        if (searchSerialNumber == 0){
            search("");
        }

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(intent.getStringExtra(MainActivity.EXTRA_TITLE_BAR_COLOR))));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("searchSerialNumber", searchSerialNumber);

        if (searchResults != null){
            savedInstanceState.putString("searchResults", searchResults.toString());
        }

        super.onSaveInstanceState(savedInstanceState);
    }

    private void search(String query){
        searchSerialNumber++;

        String body = null;
        try{
            JSONObject obj = new JSONObject();
            obj.put("SearchQuery",query);
            obj.put("SearchSerialNumber",searchSerialNumber);
            body = obj.toString();

        } catch (JSONException e) {Log.e(LOG_TAG, "Error", e);}

        new LoadFromServer().execute(token, "POST", searchUrl, company, body);
    }

    private class LoadFromServer extends AsyncTask<Object, Void, String> {
        protected String doInBackground(Object... params) {
            String tokenParam = (String)params[0];
            String methodParam = (String)params[1];
            String urlParam = (String)params[2];
            String companyParam = (String)params[3];
            String bodyParam = (String)params[4];

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String output = null;

            try {
                URL url = new URL(urlParam);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod(methodParam);
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
                wr.write(bodyParam.getBytes("UTF-8"));
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
                    buffer.append(line);
                }

                if (buffer.length() == 0) {
                    return "";
                }
                output = buffer.toString();
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

        protected void onPostExecute(String response) {
            searchResults = null;

            try{
                JSONObject responseJson = new JSONObject(response);
                JSONObject responseDataJson = responseJson.getJSONObject("Data");
                searchResults = responseDataJson.getJSONArray("Result");
            }catch (JSONException e){Log.e(LOG_TAG, "Error", e);}

            rebuildLayout();
        }
    }

    public void goBack(String value, String description){
        Intent returnIntent = new Intent();

        returnIntent.putExtra(DetailedActivity.EXTRA_NAME, name);
        returnIntent.putExtra(DetailedActivity.EXTRA_VALUE, value);
        returnIntent.putExtra(DetailedActivity.EXTRA_DESCRIPTION, description);
        returnIntent.putExtra(DetailedActivity.EXTRA_POSITION, position);
        returnIntent.putExtra(DetailedActivity.EXTRA_TABLE_PARENT, tableParent);
        returnIntent.putExtra(DetailedActivity.EXTRA_TABLE_ROW_INDEX, tableRowIndex);

        setResult(RESULT_OK,returnIntent);
        finish();
    }

    private class MyCustomAdapter extends BaseAdapter {
        private static final int TYPE_LABEL = 0;
        private static final int TYPE_MAX_COUNT = 1;

        private LayoutInflater mInflater;

        public MyCustomAdapter() {
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void addItem(final String item) {
            //mData.add(item);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return TYPE_LABEL;
        }

        @Override
        public boolean isEnabled(int position){
            return true;
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_MAX_COUNT;
        }

        @Override
        public int getCount() {
            if (searchResults != null){
                return searchResults.length();
            }
            return 0;
        }

        @Override
        public String getItem(int position) {
            JSONObject obj = searchResults.optJSONObject(position);
            return obj.optString("Description");
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            if (convertView == null) {
                switch (type) {
                    case TYPE_LABEL:
                        ViewHolder holderLabel = new ViewHolder();
                        convertView = mInflater.inflate(R.layout.flow_ui_search_selection_listview_cell_label, parent, false);
                        holderLabel.description = (TextView)convertView.findViewById(R.id.flow_ui_search_selection_cell_description);
                        holderLabel.image = (ImageView)convertView.findViewById(R.id.flow_ui_search_selection_cell_image);
                        convertView.setTag(holderLabel);
                        break;
                }
            }

            if (type == TYPE_LABEL) {
                ViewHolder holder = (ViewHolder)convertView.getTag();
                JSONObject obj = searchResults.optJSONObject(position);
                String valueObj = obj.optString("Value");
                String descriptionObj = obj.optString("Description");

                if (defaultValue.equals(valueObj)){
                    holder.description.setText(descriptionObj);
                    holder.image.setImageResource(R.drawable.ic_action_accept_dark);
                }else{
                    holder.description.setText(descriptionObj);
                    holder.image.setImageDrawable(null);
                }

                holder.value = valueObj;
            }
            return convertView;
        }
    }

    public static class ViewHolder {
        public TextView description;
        public String value;
        public ImageView image;
    }

    public void rebuildLayout(){
        MyCustomAdapter mAdapter = new MyCustomAdapter();

        ListView listView = (ListView)findViewById(R.id.searchList);
        listView.setItemsCanFocus(true);
        listView.setAdapter(mAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                try{
                    JSONObject row = searchResults.getJSONObject(position);
                    goBack(row.getString("Value"), row.getString("Description"));
                }catch (JSONException e){
                    Log.e(LOG_TAG, "", e);
                }
            }
        });

        mAdapter.notifyDataSetChanged();
    }
}