package com.infium.android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
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
import java.util.HashMap;
import java.util.Locale;

public class DetailedActivity extends AppCompatActivity {
    public final String LOG_TAG = DetailedActivity.class.getSimpleName();
    public final static String EXTRA_NAME = "com.infium.android.NAME";
    public final static String EXTRA_VALUE = "com.infium.android.VALUE";
    public final static String EXTRA_DESCRIPTION = "com.infium.android.DESCRIPTION";
    public final static String EXTRA_POSITION = "com.infium.android.POSITION";
    public final static String EXTRA_LOCAL_ACTIONS = "com.infium.android.LOCAL_ACTIONS";
    public final static String EXTRA_TABLE_PARENT = "com.infium.android.TABLE_PARENT";
    public final static String EXTRA_TABLE_ROW_INDEX = "com.infium.android.TABLE_ROW_INDEX";
    public final static int INTENT_REQUEST_CODE_SEARCH_SELECTION = 1;
    public final static int INTENT_REQUEST_CODE_POP_ACTIVITY = 2;
    private String typeOfActivity = null;
    private String webPage = null;
    private MyCustomAdapter mAdapter;
    private String nextWindow = null;
    private String nextMethod = null;
    private String nextUrl = null;
    private String nextTitleBarColor = null;
    private String buttonLabel = null;
    private JSONArray structure = null;
    private JSONArray layout = null;
    private JSONObject visibleData = null;
    private JSONObject visibleDataDescription = null;
    private JSONObject hiddenData = null;
    private String title = "";
    private String token = null;
    private String company = null;
    private String baseUrl = null;
    private String actionBarColor = null;

    //private HashMap<String,View> convertViewMap = new HashMap<String, View>();

    private boolean showPrintIcon = false;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INTENT_REQUEST_CODE_SEARCH_SELECTION) {
            if (resultCode == RESULT_OK) {
                String name = data.getStringExtra(EXTRA_NAME);
                String value = data.getStringExtra(EXTRA_VALUE);
                String description = data.getStringExtra(EXTRA_DESCRIPTION);
                int position = data.getIntExtra(EXTRA_POSITION, -1);

                String tableParent = data.getStringExtra(EXTRA_TABLE_PARENT);
                int tableRowIndex = data.getIntExtra(EXTRA_TABLE_ROW_INDEX, -1);

                try {
                    if (tableParent == null && tableRowIndex == -1){
                        visibleData.put(name, value);
                        visibleDataDescription.put(name, description);
                    }else{
                        visibleData.getJSONArray(tableParent).getJSONObject(tableRowIndex).put(name, value);
                        visibleDataDescription.getJSONArray(tableParent).getJSONObject(tableRowIndex).put(name, description);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error", e);
                }

                ListView listView = (ListView) findViewById(R.id.listView);

                View v = listView.getChildAt(position - listView.getFirstVisiblePosition());

                if (v != null) {
                    TextView someText = (TextView) v.findViewById(R.id.cell_field_search_selection_description);
                    someText.setText(description);
                }
            }
        }

        if (requestCode == INTENT_REQUEST_CODE_POP_ACTIVITY) {
            if (resultCode == RESULT_OK) {
                String localActions = data.getStringExtra(EXTRA_LOCAL_ACTIONS);
                JSONArray localActionsObject = null;
                try{
                    localActionsObject = new JSONArray(localActions);
                }catch (JSONException e){
                    Log.e(LOG_TAG, "Error", e);
                }

                runLocalActions(localActionsObject);
            }
        }
    }

    private void buildInitialData(JSONObject values, JSONObject descriptions){
        visibleData = new JSONObject();
        visibleDataDescription = new JSONObject();

        for (int i = 0; i < structure.length(); i++) {
            JSONObject row = structure.optJSONObject(i);
            String type = row.optString("Type");
            JSONArray columnStructure = row.optJSONArray("Column");

            if (type.equals("Table")){
                String name = row.optString("Name");

                JSONArray columnValues = null;
                if (values != null){
                    columnValues = values.optJSONArray(name);
                }

                if (columnValues != null && columnValues.length() > 0){

                    JSONArray dataTable = new JSONArray();
                    JSONArray dataDescriptionTable = new JSONArray();

                    for (int z = 0; z < columnValues.length(); z++) {
                        JSONObject dataRow = new JSONObject();
                        JSONObject dataDescriptionRow = new JSONObject();

                        for (int a = 0; a < columnStructure.length(); a++) {
                            String columnType = columnStructure.optJSONObject(a).optString("Type");
                            String columnName = columnStructure.optJSONObject(a).optString("Name");

                            JSONObject valueRow = columnValues.optJSONObject(z);

                            if (columnType.equals("Field")){
                                String fieldValue = valueRow.optString(columnName);

                                try{ dataRow.put(columnName, fieldValue); } catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                                try{ dataDescriptionRow.put(columnName, ""); } catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                            }
                        }
                        dataTable.put(dataRow);
                        dataDescriptionTable.put(dataDescriptionRow);
                    }

                    try{ visibleData.put(name,dataTable);} catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                    try{ visibleDataDescription.put(name,dataDescriptionTable);} catch (JSONException e){ Log.e(LOG_TAG, "Error", e) ;}
                }else{
                    JSONObject dataRow = new JSONObject();
                    JSONObject dataDescriptionRow = new JSONObject();

                    for (int a = 0; a < columnStructure.length(); a++) {
                        String columnType = columnStructure.optJSONObject(a).optString("Type");
                        String columnName = columnStructure.optJSONObject(a).optString("Name");

                        if (columnType.equals("SearchSelection")){
                            try{ dataRow.put(columnName, ""); } catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                            try{ dataDescriptionRow.put(columnName, "<not set>"); } catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                        }else{
                            try{ dataRow.put(columnName, ""); } catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                            try{ dataDescriptionRow.put(columnName, ""); } catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                        }

                    }

                    JSONArray dataTable = new JSONArray();
                    JSONArray dataDescriptionTable = new JSONArray();
                    dataTable.put(dataRow);
                    dataDescriptionTable.put(dataDescriptionRow);
                    try{ visibleData.put(name,dataTable);} catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                    try{ visibleDataDescription.put(name,dataDescriptionTable);} catch (JSONException e){ Log.e(LOG_TAG, "Error", e); }
                }
            }else{
                String name = row.optString("Name");
                try{
                    JSONObject returnObject = buildInitialDataForRow(values, descriptions, row);
                    if (row.optString("Type").equals("LabelTrueFalse")){
                        visibleData.put(name,returnObject.optBoolean("value", false));
                        visibleDataDescription.put(name,returnObject.optBoolean("description", false));
                    }else{
                        visibleData.put(name,returnObject.optString("value", ""));
                        visibleDataDescription.put(name,returnObject.optString("description", ""));
                    }

                }catch(JSONException e){ Log.e(LOG_TAG, "Error", e); }
            }
        }
    }

    private JSONObject buildInitialDataForRow(JSONObject values, JSONObject descriptions, JSONObject row){
        String name = row.optString("Name");
        String type = row.optString("Type");
        Object value = null;
        String description = null;

        if (values == null){
            values = new JSONObject();
        }

        if (descriptions == null){
            descriptions = new JSONObject();
        }

        if (type.equals("Field")){
            value = values.optString(name, "");
        }

        if (type.equals("LabelTrueFalse")){
            value = values.optBoolean(name, false);
        }

        if (type.equals("SearchSelection")){
            value = values.optString(name, "");
            description = descriptions.optString(name, "<not set>");
        }

        JSONObject returnObject = new JSONObject();

        if (value != null){
            try{
                returnObject.put("value", value);
            }catch (JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }
        }

        if (description != null){
            try{
                returnObject.put("description", description);
            }catch (JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }
        }

        return returnObject;
    }

    private void rebuildLayout(){
        if (structure != null){
            layout = new JSONArray();

            try{
                for (int i = 0; i < structure.length(); i++) {
                    JSONObject row = structure.getJSONObject(i);

                    if (row.getString("Type").equals("LabelValueLink")){
                        JSONObject newObject = new JSONObject();

                        newObject.put("Type", "LabelValueLink");

                        if (row.has("Label")){
                            newObject.put("Label", row.getString("Label"));
                        }

                        if (row.has("Value")){
                            newObject.put("Value", row.getString("Value"));
                        }

                        if (row.has("Method")){
                            newObject.put("Method", row.getString("Method"));
                        }

                        if (row.has("Url")){
                            newObject.put("Url", row.getString("Url"));
                        }

                        if (row.has("Body")){
                            newObject.put("Body", row.getString("Body"));
                        }

                        if (row.has("Refresh")){
                            newObject.put("Refresh", row.getString("Refresh"));
                        }

                        if (row.has("TitleBarColorNewWindow")){
                            newObject.put("TitleBarColorNewWindow", row.getString("TitleBarColorNewWindow"));
                        }

                        if (row.has("Indent")){
                            newObject.put("Indent", row.getString("Indent"));
                        }

                        if (row.has("IconUnicode")){
                            newObject.put("IconUnicode", row.getString("IconUnicode"));
                        }

                        if (row.has("IconColor")){
                            newObject.put("IconColor", row.getString("IconColor"));
                        }

                        layout.put(newObject);
                    }

                    if (row.getString("Type").equals("LabelWithLink")){
                        JSONObject newObject = new JSONObject();
                        newObject.put("Type", "LabelWithLink");
                        newObject.put("Label", row.getString("Label"));
                        newObject.put("Method", row.getString("Method"));
                        newObject.put("Url", row.getString("Url"));
                        layout.put(newObject);
                    }

                    if (row.getString("Type").equals("Field")){
                        JSONObject newObject = new JSONObject();
                        newObject.put("Type", "Field");
                        newObject.put("Name", row.getString("Name"));
                        newObject.put("Label", row.getString("Label"));
                        newObject.put("KeyboardType", row.getString("KeyboardType"));
                        layout.put(newObject);
                    }

                    if (row.getString("Type").equals("SearchSelection")){
                        JSONObject newObject = new JSONObject();
                        newObject.put("Type", "SearchSelection");
                        newObject.put("Name", row.getString("Name"));
                        newObject.put("Label", row.getString("Label"));
                        newObject.put("SearchUrl", row.getString("SearchUrl"));
                        layout.put(newObject);
                    }

                    if (row.getString("Type").equals("LabelTrueFalse")){
                        JSONObject newObject = new JSONObject();
                        newObject.put("Type", "LabelTrueFalse");
                        newObject.put("Name", row.getString("Name"));
                        newObject.put("Label", row.getString("Label"));
                        layout.put(newObject);
                    }

                    if (row.getString("Type").equals("LabelValue")){
                        JSONObject newObject = new JSONObject();
                        newObject.put("Type", "LabelValue");
                        newObject.put("Label", row.getString("Label"));
                        newObject.put("Value", row.getString("Value"));
                        layout.put(newObject);
                    }

                    if (row.getString("Type").equals("LabelHeader")){
                        JSONObject newObject = new JSONObject();
                        newObject.put("Type", "LabelHeader");
                        newObject.put("Label", row.getString("Label"));
                        layout.put(newObject);
                    }

                    if (row.getString("Type").equals("Table")){
                        int numberOfElementsInValue = visibleData.getJSONArray(row.getString("Name")).length();

                        for (int b = 0; b < numberOfElementsInValue; b++) {

                            JSONObject headerObject = new JSONObject();
                            headerObject.put("Type", "LabelHeader");
                            headerObject.put("Label", row.getString("Name") + " " + (b+1));

                            layout.put(headerObject);

                            JSONArray columns = row.optJSONArray("Column");

                            for (int c = 0; c < columns.length(); c++) {
                                JSONObject column = columns.getJSONObject(c);

                                String columnType = column.getString("Type");

                                if (columnType.equals("Field")) {
                                    JSONObject newObject = new JSONObject();
                                    newObject.put("Type", "Field");
                                    newObject.put("Name", column.getString("Name"));
                                    newObject.put("Label", column.getString("Label"));
                                    newObject.put("KeyboardType", column.getString("KeyboardType"));
                                    newObject.put("TableParent", row.getString("Name"));
                                    newObject.put("TableRowIndex", b);
                                    layout.put(newObject);
                                }

                                if (columnType.equals("SearchSelection")) {
                                    JSONObject newObject = new JSONObject();
                                    newObject.put("Type", "SearchSelection");
                                    newObject.put("Name", column.getString("Name"));
                                    newObject.put("Label", column.getString("Label"));
                                    newObject.put("SearchUrl", column.getString("SearchUrl"));
                                    newObject.put("TableParent", row.getString("Name"));
                                    newObject.put("TableRowIndex", b);
                                    layout.put(newObject);
                                }
                            }
                        }

                        JSONObject footerObject = new JSONObject();
                        footerObject.put("Type", "Footer");
                        footerObject.put("TableParent", row.getString("Name"));
                        footerObject.put("Label", "Add row");

                        layout.put(footerObject);
                    }
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }
        }

        if (mAdapter == null){
            mAdapter = new MyCustomAdapter();

            ListView listView = (ListView)findViewById(R.id.listView);
            listView.setItemsCanFocus(true);
            listView.setAdapter(mAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    try{

                        JSONObject row = layout.getJSONObject(position);

                        if (row.getString("Type").equals("LabelValueLink")){
                            if (row.has("Url") && row.has("Method")){
                                String method = row.getString("Method");
                                String url = row.getString("Url");

                                Intent intent = new Intent(getApplicationContext(), DetailedActivity.class);
                                intent.putExtra(MainActivity.EXTRA_METHOD, method);
                                intent.putExtra(MainActivity.EXTRA_URL, url);
                                intent.putExtra(MainActivity.EXTRA_COMPANY, company);
                                intent.putExtra(MainActivity.EXTRA_BODY, "");
                                intent.putExtra(MainActivity.EXTRA_BASEURL, baseUrl);

                                if (row.has("TitleBarColorNewWindow")){
                                    intent.putExtra(MainActivity.EXTRA_TITLE_BAR_COLOR, row.getString("TitleBarColorNewWindow"));
                                }

                                startActivityForResult(intent, INTENT_REQUEST_CODE_POP_ACTIVITY);
                            }
                        }

                        if (row.getString("Type").equals("LabelWithLink")){
                            if (row.getString("Url") != null && (!row.getString("Url").equals(""))){
                                String method = row.getString("Method");
                                String url = row.getString("Url");

                                Intent intent = new Intent(getApplicationContext(), DetailedActivity.class);
                                intent.putExtra(MainActivity.EXTRA_METHOD, method);
                                intent.putExtra(MainActivity.EXTRA_URL, url);
                                intent.putExtra(MainActivity.EXTRA_COMPANY, company);
                                intent.putExtra(MainActivity.EXTRA_BODY, "");
                                intent.putExtra(MainActivity.EXTRA_BASEURL, baseUrl);

                                startActivityForResult(intent, INTENT_REQUEST_CODE_POP_ACTIVITY);
                            }
                        }

                        if (row.getString("Type").equals("LabelTrueFalse")){

                            ImageView imageView = (ImageView)view.findViewById(R.id.cell_label_true_false_imageView);

                            boolean value = visibleData.optBoolean(row.optString("Name"));

                            if (value){
                                imageView.setImageDrawable(null);
                                visibleData.put(row.optString("Name"), false);
                            }else{
                                imageView.setImageResource(R.drawable.ic_action_accept_dark);
                                visibleData.put(row.optString("Name"), true);
                            }
                        }

                        if (row.getString("Type").equals("SearchSelection")){

                            String name = row.getString("Name");

                            String tableParent = row.optString("TableParent", null);
                            int tableRowIndex = row.optInt("TableRowIndex", -1);

                            String value;

                            if (tableParent == null && tableRowIndex == -1){
                                value = visibleData.getString(name);
                            }else{
                                value = visibleData.optJSONArray(tableParent).optJSONObject(tableRowIndex).optString(name);
                            }

                            Intent intent = new Intent(getApplicationContext(), SearchSelectionActivity.class);
                            intent.putExtra(SearchSelectionActivity.EXTRA_NAME, row.getString("Name"));
                            intent.putExtra(SearchSelectionActivity.EXTRA_VALUE, value);
                            intent.putExtra(SearchSelectionActivity.EXTRA_POSITION, position);
                            intent.putExtra(SearchSelectionActivity.EXTRA_TITLE, row.getString("Label"));
                            intent.putExtra(SearchSelectionActivity.EXTRA_SEARCH_URL, row.getString("SearchUrl"));
                            intent.putExtra(SearchSelectionActivity.EXTRA_TABLE_PARENT, row.optString("TableParent", null));
                            intent.putExtra(SearchSelectionActivity.EXTRA_TABLE_ROW_INDEX, row.optInt("TableRowIndex", -1));

                            intent.putExtra(MainActivity.EXTRA_TITLE_BAR_COLOR, actionBarColor);

                            startActivityForResult(intent, INTENT_REQUEST_CODE_SEARCH_SELECTION);
                        }

                        if (row.getString("Type").equals("Field")){

                            AlertDialog.Builder builder = new AlertDialog.Builder(DetailedActivity.this);

                            LayoutInflater inflater = DetailedActivity.this.getLayoutInflater();

                            View newView = inflater.inflate(R.layout.flow_ui_dialog_field, null);
                            builder.setView(newView);

                            TextView label = (TextView)newView.findViewById(R.id.label);
                            final EditText field = (EditText)newView.findViewById(R.id.field);

                            label.setBackgroundColor(Color.parseColor(actionBarColor));

                            if (row.getString("KeyboardType").equals("Decimal")) {
                                field.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                            }

                            String name = row.getString("Name");

                            String tableParent = row.optString("TableParent", null);
                            int tableRowIndex = row.optInt("TableRowIndex", -1);

                            if (tableParent == null && tableRowIndex == -1){
                                field.setText(visibleData.optString(name));
                                label.setText(row.getString("Label"));
                            }else{
                                field.setText(visibleData.optJSONArray(tableParent).optJSONObject(tableRowIndex).optString(name));
                                label.setText(tableParent + " " + (tableRowIndex+1) + " - " + row.getString("Label"));
                            }

                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        JSONObject row = layout.getJSONObject(position);
                                        String name = row.getString("Name");

                                        String tableParent = row.optString("TableParent", null);
                                        int tableRowIndex = row.optInt("TableRowIndex", -1);

                                        if (tableParent == null && tableRowIndex == -1) {
                                            try {
                                                visibleData.put(name, field.getText().toString());
                                            } catch (JSONException e) {
                                                Log.e(LOG_TAG, "", e);
                                            }
                                        }

                                        if (tableParent != null && tableRowIndex != -1) {
                                            try {
                                                visibleData.getJSONArray(tableParent).getJSONObject(tableRowIndex).put(name, field.getText().toString());
                                                //int lastIndexInTable = visibleData.getJSONArray(tableParent).length() - 1;

                                                //if (tableRowIndex == lastIndexInTable) {
                                                ////Log.d(LOG_TAG, "Changing in last row in table, new row should be created");
                                                ////addEmptyRowToTable(tableParent);
                                                //}
                                            } catch (JSONException e) {
                                                Log.e(LOG_TAG, "", e);
                                            }
                                        }
                                        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                                    } catch (JSONException e) {
                                        Log.e(LOG_TAG, "Error", e);
                                    }
                                }
                            });

                            field.setSelection(field.getText().length());

                            builder.show();

                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                        }

                        if (row.getString("Type").equals("Footer")){
                            String tableParent = row.getString("TableParent");
                            addEmptyRowToTable(tableParent);
                        }
                    }catch (JSONException e){
                        Log.e(LOG_TAG, "", e);
                    }
                }
            });
        }

        setTitle(title);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (typeOfActivity != null && typeOfActivity.equals("JSON")){
            if (structure != null){
                savedInstanceState.putString("typeOfActivity", typeOfActivity);
                savedInstanceState.putString("title", title);
                savedInstanceState.putString("structure", structure.toString());
                savedInstanceState.putString("visibleData", visibleData.toString());
                savedInstanceState.putString("visibleDataDescription", visibleDataDescription.toString());

                savedInstanceState.putString("nextWindow", nextWindow);
                savedInstanceState.putString("nextMethod", nextMethod);
                savedInstanceState.putString("nextUrl", nextUrl);
                savedInstanceState.putString("buttonLabel", buttonLabel);

                if (hiddenData != null){
                    savedInstanceState.putString("hiddenData", hiddenData.toString());
                }
            }
        }

        if (typeOfActivity != null && typeOfActivity.equals("HTML")) {
            savedInstanceState.putString("typeOfActivity", typeOfActivity);
            savedInstanceState.putString("webPage", webPage);
            savedInstanceState.putBoolean("showPrintIcon", showPrintIcon);
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        getSupportActionBar().setElevation(0);

        String titleBarColor = intent.getStringExtra(MainActivity.EXTRA_TITLE_BAR_COLOR);

        if ((titleBarColor == null)||(titleBarColor.equals(""))){
            actionBarColor = MainActivity.DEFAULT_COLOR;
        }else{
            actionBarColor = titleBarColor;
        }

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor(actionBarColor)));

        SharedPreferences prefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        token = prefs.getString("token", null);
        company = prefs.getString("company", null);

        String method = intent.getStringExtra(MainActivity.EXTRA_METHOD);
        String url = intent.getStringExtra(MainActivity.EXTRA_URL);
        String company = intent.getStringExtra(MainActivity.EXTRA_COMPANY);
        String body = intent.getStringExtra(MainActivity.EXTRA_BODY);
        baseUrl = intent.getStringExtra(MainActivity.EXTRA_BASEURL);

        if (savedInstanceState != null) {
            if (savedInstanceState.getString("typeOfActivity").equals("JSON")) {

                setContentView(R.layout.activity_flow_ui);

                try {
                    typeOfActivity = savedInstanceState.getString("typeOfActivity");

                    nextWindow = savedInstanceState.getString("nextWindow");
                    nextMethod = savedInstanceState.getString("nextMethod");
                    nextUrl = savedInstanceState.getString("nextUrl");
                    buttonLabel = savedInstanceState.getString("buttonLabel");

                    title = savedInstanceState.getString("title");
                    structure = new JSONArray(savedInstanceState.getString("structure"));
                    visibleData = new JSONObject(savedInstanceState.getString("visibleData"));
                    visibleDataDescription = new JSONObject(savedInstanceState.getString("visibleDataDescription"));

                    if (savedInstanceState.getString("hiddenData") != null) {
                        hiddenData = new JSONObject(savedInstanceState.getString("hiddenData"));
                    }

                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error", e);
                }
                rebuildLayout();
            }

            if (savedInstanceState.getString("typeOfActivity").equals("HTML")) {

                if (typeOfActivity == null) {
                    setContentView(R.layout.activity_flow_ui_html);
                    typeOfActivity = "HTML";
                }

                webPage = savedInstanceState.getString("webPage");
                showPrintIcon = savedInstanceState.getBoolean("showPrintIcon");

                WebView webView = (WebView)findViewById(R.id.webView);

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        DetailedActivity.this.setTitle(view.getTitle());
                    }
                });

                webView.loadData(webPage, "text/html; charset=UTF-8", null);

            }
        }else{
            new LoadFromServerFirst().execute(token, method, url, company, body);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if ((typeOfActivity != null) && (typeOfActivity.equals("JSON"))){
            if ((nextMethod != null) && (nextUrl != null) && (buttonLabel != null) && (!nextMethod.equals("")) && (!nextUrl.equals("")) && (!buttonLabel.equals(""))){
                getMenuInflater().inflate(R.menu.flow_ui, menu);

                MenuItem item = menu.findItem(R.id.button);
                item.setTitle(buttonLabel);

                if (buttonLabel.equals("Next")){
                    item.setIcon(R.drawable.ic_action_forward);
                }

                if (buttonLabel.equals("Run")){
                    item.setIcon(R.drawable.ic_action_forward);
                }

                if (buttonLabel.equals("Add")){
                    item.setIcon(R.drawable.ic_action_new);
                }

                if (buttonLabel.equals("Create")){
                    item.setIcon(R.drawable.ic_action_new);
                }

                if (buttonLabel.equals("Change")){
                    item.setIcon(R.drawable.ic_action_edit);
                }

                if (buttonLabel.equals("Reverse")){
                    item.setIcon(R.drawable.ic_action_new);
                }

                if (buttonLabel.equals("Search")){
                    item.setIcon(R.drawable.ic_action_forward);
                }

                return true;
            }
        }

        if ((typeOfActivity != null) && (typeOfActivity.equals("HTML"))) {
            if (showPrintIcon){
                getMenuInflater().inflate(R.menu.flow_ui, menu);
                MenuItem item = menu.findItem(R.id.button);
                item.setTitle("Print");
                item.setIcon(R.drawable.ic_action_print);
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if ((typeOfActivity != null) && (typeOfActivity.equals("JSON"))) {

            if (id == R.id.button) {
                JSONObject body = new JSONObject();
                try {
                    body.put("VisibleData", visibleData);
                    if (hiddenData != null) {
                        body.put("HiddenData", hiddenData);
                    }
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error", e);
                }

                if (nextWindow.equals("New")) {
                    Intent intent = new Intent(getApplicationContext(), DetailedActivity.class);
                    intent.putExtra(MainActivity.EXTRA_METHOD, nextMethod);
                    intent.putExtra(MainActivity.EXTRA_URL, nextUrl);
                    intent.putExtra(MainActivity.EXTRA_TITLE_BAR_COLOR, nextTitleBarColor);

                    intent.putExtra(MainActivity.EXTRA_COMPANY, company);
                    intent.putExtra(MainActivity.EXTRA_BODY, body.toString());
                    intent.putExtra(MainActivity.EXTRA_BASEURL, baseUrl);

                    startActivityForResult(intent, INTENT_REQUEST_CODE_POP_ACTIVITY);
                }

                if (nextWindow.equals("Same")) {
                    new LoadFromServerFirst().execute(token, nextMethod, nextUrl, company, body.toString());
                }

                return true;
            }
        }

        if ((typeOfActivity != null) && (typeOfActivity.equals("HTML"))) {
            if (id == R.id.button) {
                WebView webView = (WebView)findViewById(R.id.webView);
                createWebPrintJob(webView);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private class LoadFromServerFirst extends AsyncTask<Object, Void, JSONObject> {
        protected JSONObject doInBackground(Object... params) {
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
                outputObject.put("ContentType", urlConnection.getContentType());
                outputObject.put("Show-Print-Icon", urlConnection.getHeaderField("Show-Print-Icon"));
            }catch (JSONException e){
                Log.e(LOG_TAG, "Error", e);
            }

            return outputObject;
        }

        protected void onPostExecute(JSONObject outputObject) {
            String response = null;
            String contentType = null;

            try {
                response = outputObject.getString("Output");
                contentType = outputObject.getString("ContentType");
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error", e);
            }

            if (contentType.equals("application/json")) {
                if (typeOfActivity == null) {
                    setContentView(R.layout.activity_flow_ui);
                    typeOfActivity = "JSON";
                }

                try {
                    JSONObject responseJson = new JSONObject(response);

                    if (responseJson.getString("Response").equals("UI")) {
                        JSONObject responseDataJson = responseJson.getJSONObject("Data");

                        nextWindow = responseDataJson.optString("Window");
                        nextMethod = responseDataJson.optString("Method");
                        nextUrl = responseDataJson.optString("Url");
                        nextTitleBarColor = responseDataJson.optString("TitleBarColorNewWindow");
                        buttonLabel = responseDataJson.optString("ButtonLabel");
                        title = responseDataJson.getString("Title");
                        structure = responseDataJson.optJSONArray("Structure");
                        JSONObject values = responseDataJson.optJSONObject("VisibleData");
                        JSONObject descriptions = responseDataJson.optJSONObject("VisibleDataDescription");

                        if (structure == null){
                            structure = new JSONArray();
                        }

                        try {
                            if (responseDataJson.has("HiddenData")){
                                hiddenData = responseDataJson.getJSONObject("HiddenData");
                            }
                        } catch (JSONException e) {
                            Log.e(LOG_TAG, "Error", e);
                        }

                        invalidateOptionsMenu();

                        buildInitialData(values, descriptions);
                        rebuildLayout();
                    }

                    if (responseJson.getString("Response").equals("LocalActions")) {
                        JSONArray responseDataJson = responseJson.getJSONArray("Data");
                        runLocalActions(responseDataJson);
                    }

                } catch (JSONException e) {
                    Log.e(LOG_TAG, "", e);
                }
            }

            if (contentType.equals("text/html")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try{
                        if ((outputObject.getString("Show-Print-Icon") != null)&&(outputObject.getString("Show-Print-Icon").equals("true"))){
                            showPrintIcon = true;
                        }
                    } catch(JSONException e ){
                        Log.e(LOG_TAG, "Error", e);
                    }
                }

                if (typeOfActivity == null) {
                    setContentView(R.layout.activity_flow_ui_html);
                    typeOfActivity = "HTML";
                }

                webPage = response;

                WebView webView = (WebView)findViewById(R.id.webView);

                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        DetailedActivity.this.setTitle(view.getTitle());
                        DetailedActivity.this.invalidateOptionsMenu();
                    }
                });

                webView.loadData(response, "text/html; charset=UTF-8", null);
            }
        }
    }

    private class MyCustomAdapter extends BaseAdapter {
        private static final int TYPE_LABEL_WITH_LINK = 0;
        private static final int TYPE_FIELD = 1;
        private static final int TYPE_SEARCH_SELECTION = 2;
        private static final int TYPE_LABEL_HEADER = 3;
        private static final int TYPE_FOOTER = 4;
        private static final int TYPE_LABEL_TRUE_FALSE = 5;
        private static final int TYPE_LABEL_VALUE = 6;
        private static final int TYPE_LABEL_VALUE_LINK = 7;

        private static final int TYPE_MAX_COUNT = 8;
        private LayoutInflater mInflater;

        public MyCustomAdapter() {
            mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getItemViewType(int position) {
            try{
                JSONObject row = layout.getJSONObject(position);
                if (row.getString("Type").equals("LabelValueLink")){
                    if (row.has("Value")){
                        return TYPE_LABEL_VALUE;
                    }else{
                        return TYPE_LABEL_WITH_LINK;
                    }
                }
                if (row.getString("Type").equals("LabelWithLink")){
                    return TYPE_LABEL_WITH_LINK;
                }
                if (row.getString("Type").equals("Field")){
                    return TYPE_FIELD;
                }
                if (row.getString("Type").equals("SearchSelection")){
                    return TYPE_SEARCH_SELECTION;
                }
                if (row.getString("Type").equals("LabelHeader")){
                    return TYPE_LABEL_HEADER;
                }
                if (row.getString("Type").equals("Footer")){
                    return TYPE_FOOTER;
                }
                if (row.getString("Type").equals("LabelTrueFalse")){
                    return TYPE_LABEL_TRUE_FALSE;
                }
                if (row.getString("Type").equals("LabelValue")){
                    return TYPE_LABEL_VALUE;
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "", e);
            }
            return 0;
        }

        // This does not work in Android 5.0

        @Override
        public boolean areAllItemsEnabled(){
            return true;
        }

        @Override
        public boolean isEnabled(int position){
            try{
                JSONObject row = layout.getJSONObject(position);
                if (row.getString("Type").equals("LabelValueLink")){
                    if (row.has("Url") && row.has("Url")){
                        return true;
                    }else{
                        return true;
                    }
                }
                if (row.getString("Type").equals("LabelWithLink")){
                    if (row.getString("Url") != null &&(!row.getString("Url").equals(""))){
                        return true;
                    }else{
                        return true;
                    }
                }
                if (row.getString("Type").equals("Field")){
                    return true;
                }
                if (row.getString("Type").equals("SearchSelection")){
                    return true;
                }
                if (row.getString("Type").equals("LabelHeader")){
                    return true;
                }
                if (row.getString("Type").equals("Footer")){
                    return true;
                }
                if (row.getString("Type").equals("LabelTrueFalse")){
                    return true;
                }
                if (row.getString("Type").equals("LabelValue")){
                    return true;
                }
            } catch (JSONException e) {
                Log.e(LOG_TAG, "", e);
            }
            return false;
        }

        @Override
        public int getViewTypeCount() {
            return TYPE_MAX_COUNT;
        }

        @Override
        public int getCount() {
            return layout.length();
        }

        @Override
        public String getItem(int position) {
            return "";
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            if (true) {
                switch (type) {
                    case TYPE_LABEL_WITH_LINK:
                        ViewHolder holderLabel = new ViewHolder();
                        convertView = mInflater.inflate(R.layout.flow_ui_listview_cell_label_with_link, parent, false);
                        holderLabel.icon = (TextView)convertView.findViewById(R.id.cell_label_with_link_icon);
                        holderLabel.textView = (TextView)convertView.findViewById(R.id.cell_label_with_link_textView);
                        convertView.setTag(holderLabel);
                        break;
                    case TYPE_LABEL_TRUE_FALSE:
                        ViewHolderLabelTrueFalse holderLabelTrueFalse = new ViewHolderLabelTrueFalse();
                        convertView = mInflater.inflate(R.layout.flow_ui_listview_cell_label_true_false, parent, false);
                        holderLabelTrueFalse.label = (TextView)convertView.findViewById(R.id.cell_label_true_false_textView);
                        holderLabelTrueFalse.image = (ImageView)convertView.findViewById(R.id.cell_label_true_false_imageView);
                        convertView.setTag(holderLabelTrueFalse);
                        break;
                    case TYPE_FIELD:

                        ViewHolderField2 holderField2 = new ViewHolderField2();
                        convertView = mInflater.inflate(R.layout.flow_ui_listview_cell_field2, parent, false);
                        holderField2.label = (TextView)convertView.findViewById(R.id.cell_field_label);
                        holderField2.text = (TextView)convertView.findViewById(R.id.cell_field_text);
                        convertView.setTag(holderField2);
                        break;

                    case TYPE_SEARCH_SELECTION:
                        ViewHolderSearchSelection holderSearchSelection = new ViewHolderSearchSelection();
                        convertView = mInflater.inflate(R.layout.flow_ui_listview_cell_search_selection, parent, false);
                        holderSearchSelection.label = (TextView)convertView.findViewById(R.id.cell_field_search_selection_label);
                        holderSearchSelection.description = (TextView)convertView.findViewById(R.id.cell_field_search_selection_description);
                        convertView.setTag(holderSearchSelection);
                        break;

                    case TYPE_LABEL_HEADER:
                        ViewHolderLabelHeader holderHeader = new ViewHolderLabelHeader();
                        convertView = mInflater.inflate(R.layout.flow_ui_listview_cell_header, parent, false);
                        holderHeader.label = (TextView)convertView.findViewById(R.id.cell_header_textView);
                        convertView.setTag(holderHeader);
                        break;

                    case TYPE_LABEL_VALUE:
                        ViewHolderLabelValue holderLabelValue = new ViewHolderLabelValue();
                        convertView = mInflater.inflate(R.layout.flow_ui_listview_cell_label_value, parent, false);
                        holderLabelValue.label = (TextView)convertView.findViewById(R.id.cell_label_value_label);
                        holderLabelValue.value = (TextView)convertView.findViewById(R.id.cell_label_value_value);
                        convertView.setTag(holderLabelValue);
                        break;

                    case TYPE_FOOTER:
                        ViewHolderFooter holderFooter = new ViewHolderFooter();
                        convertView = mInflater.inflate(R.layout.flow_ui_listview_cell_footer, parent, false);
                        holderFooter.label = (TextView)convertView.findViewById(R.id.cell_footer_textView);
                        convertView.setTag(holderFooter);
                        break;

                }
            }

            if (type == TYPE_LABEL_WITH_LINK) {
                ViewHolder holder = (ViewHolder)convertView.getTag();
                JSONObject obj = layout.optJSONObject(position);
                String iconUnicode = obj.optString("IconUnicode");
                String iconColor = obj.optString("IconColor");
                String label = obj.optString("Label");
                int indent = obj.optInt("Indent");

                final float scale = getResources().getDisplayMetrics().density;

                if (!iconUnicode.equals("")){
                    Typeface tf = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
                    holder.icon.setTypeface(tf);
                    holder.icon.setText(iconUnicode);
                    holder.icon.setTextColor(Color.parseColor(iconColor));
                    holder.icon.setVisibility(View.VISIBLE);
                }else{
                    holder.icon.setVisibility(View.GONE);
                }

                if (indent == 0){
                    holder.textView.setPadding(0, 0, 0, 0);
                }else{
                    holder.textView.setPadding((int)(scale * 0.5f * indent * 20), 0, 0, 0);
                }

                holder.textView.setText(label);
            }

            if (type == TYPE_LABEL_TRUE_FALSE) {
                ViewHolderLabelTrueFalse holder = (ViewHolderLabelTrueFalse)convertView.getTag();
                JSONObject obj = layout.optJSONObject(position);
                String label = obj.optString("Label");
                holder.label.setText(label);

                boolean value = visibleData.optBoolean(obj.optString("Name"));

                if (value){
                    holder.image.setImageResource(R.drawable.ic_action_accept_dark);
                }else{
                    holder.image.setImageDrawable(null);
                }
            }

            if (type == TYPE_FIELD){
                ViewHolderField2 holderField2 = (ViewHolderField2)convertView.getTag();
                JSONObject obj = layout.optJSONObject(position);
                String label = obj.optString("Label");
                String name = obj.optString("Name");

                String tableParent = obj.optString("TableParent", null);
                int tableRowIndex = obj.optInt("TableRowIndex", -1);

                if (tableParent == null && tableRowIndex == -1){
                    holderField2.label.setText(label);
                    holderField2.text.setText(visibleData.optString(name));
                }else{
                    holderField2.label.setText(label);
                    holderField2.text.setText(visibleData.optJSONArray(tableParent).optJSONObject(tableRowIndex).optString(name));
                }
            }

            if (type == TYPE_SEARCH_SELECTION) {
                ViewHolderSearchSelection holderSearchSelection = (ViewHolderSearchSelection)convertView.getTag();
                JSONObject obj = layout.optJSONObject(position);
                String label = obj.optString("Label");
                String name = obj.optString("Name");

                String tableParent = obj.optString("TableParent", null);
                int tableRowIndex = obj.optInt("TableRowIndex", -1);

                if (tableParent == null && tableRowIndex == -1){
                    holderSearchSelection.label.setText(label);
                    holderSearchSelection.description.setText(visibleDataDescription.optString(name));
                }else{
                    holderSearchSelection.label.setText(label);
                    holderSearchSelection.description.setText(visibleDataDescription.optJSONArray(tableParent).optJSONObject(tableRowIndex).optString(name));
                }
            }

            if (type == TYPE_LABEL_HEADER) {
                ViewHolderLabelHeader holderHeader = (ViewHolderLabelHeader)convertView.getTag();
                JSONObject obj = layout.optJSONObject(position);
                String label = obj.optString("Label");
                holderHeader.label.setText(label);
            }

            if (type == TYPE_LABEL_VALUE) {
                ViewHolderLabelValue holderLabelValue = (ViewHolderLabelValue)convertView.getTag();
                JSONObject obj = layout.optJSONObject(position);
                String label = obj.optString("Label");
                String value = obj.optString("Value");
                holderLabelValue.label.setText(label);
                holderLabelValue.value.setText(value);

                int indent = obj.optInt("Indent");

                final float scale = getResources().getDisplayMetrics().density;

                if (indent == 0){
                    holderLabelValue.label.setPadding(0, 0, 0, 0);
                }else{
                    holderLabelValue.label.setPadding((int)(scale * 0.5f * indent * 20), 0, 0, 0);
                }
            }

            if (type == TYPE_FOOTER) {
                ViewHolderFooter holderFooter = (ViewHolderFooter)convertView.getTag();
                JSONObject obj = layout.optJSONObject(position);
                String label = obj.optString("Label");
                holderFooter.label.setText(label);
            }

            return convertView;
        }
    }

    public static class ViewHolderSearchSelection {
        public TextView label;
        public TextView description;
    }

    public static class ViewHolderField2 {
        public TextView label;
        public TextView text;
    }

    public static class ViewHolderLabelHeader {
        public TextView label;
    }

    public static class ViewHolderLabelValue {
        public TextView label;
        public TextView value;
    }

    public static class ViewHolderFooter {
        public TextView label;
    }

    public static class ViewHolderLabelTrueFalse {
        public TextView label;
        public ImageView image;
    }

    public static class ViewHolder {
        public TextView icon;
        public TextView textView;
        public EditText editText;
        public MutableWatcher mWatcher;
    }

    class MutableWatcher implements TextWatcher {
        private int mPosition;
        private boolean mActive;

        void setPosition(int position) {
            mPosition = position;
        }

        void setActive(boolean active) {
            mActive = active;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            if (mActive) {
                JSONObject obj = layout.optJSONObject(mPosition);
                String type = obj.optString("Type");
                String name = obj.optString("Name");
                String tableParent = obj.optString("TableParent", null);
                int tableRowIndex = obj.optInt("TableRowIndex", -1);

                if (type.equals("Field") && tableParent == null && tableRowIndex == -1){
                    try{
                        visibleData.put(name, s.toString());
                    }catch(JSONException e){
                        Log.e(LOG_TAG, "", e);
                    }
                }

                if (type.equals("Field") && tableParent != null && tableRowIndex != -1){
                    try{
                        visibleData.getJSONArray(tableParent).getJSONObject(tableRowIndex).put(name, s.toString());
                    }catch(JSONException e){
                        Log.e(LOG_TAG, "", e);
                    }
                }
            }
        }
    }

    private void addEmptyRowToTable(String tableParent){
        try{
            for (int a = 0; a < structure.length(); a++) {
                JSONObject row = structure.optJSONObject(a);
                if (row.optString("Type").equals("Table")&&row.optString("Name").equals(tableParent)){
                    JSONObject dataRow = new JSONObject();
                    JSONObject descriptionRow = new JSONObject();
                    for (int b = 0; b < row.optJSONArray("Column").length(); b++) {
                        if (row.optJSONArray("Column").optJSONObject(b).getString("Type").equals("SearchSelection")){
                            dataRow.put(row.optJSONArray("Column").optJSONObject(b).getString("Name"), "");
                            descriptionRow.put(row.optJSONArray("Column").optJSONObject(b).getString("Name"), "<not set>");
                        }else{
                            dataRow.put(row.optJSONArray("Column").optJSONObject(b).getString("Name"), "");
                            descriptionRow.put(row.optJSONArray("Column").optJSONObject(b).getString("Name"), "");
                        }
                    }
                    visibleData.optJSONArray(tableParent).put(dataRow);
                    visibleDataDescription.optJSONArray(tableParent).put(descriptionRow);
                }
            }
        }catch (JSONException e){}
        rebuildLayout();
    }

    private JSONArray JSONArrayRemoveAtIndex(JSONArray jsonArray, int index){
        JSONArray list = new JSONArray();
        int len = jsonArray.length();
        if (jsonArray != null) {
            for (int i=0;i<len;i++)
            {
                if (i != index)
                {
                    try{ list.put(jsonArray.get(i)); }catch (JSONException e){
                        Log.e(LOG_TAG, "Error", e);
                    }
                }
            }
        }
        return list;
    }

    private void runLocalActions(JSONArray actions){
        if (actions.length() > 0){
            if (actions.optJSONObject(0).optString("Action").equals("Pop")){
                actions = JSONArrayRemoveAtIndex(actions, 0);
                Intent returnIntent = new Intent();
                returnIntent.putExtra(EXTRA_LOCAL_ACTIONS, actions.toString());
                setResult(RESULT_OK,returnIntent);
                finish();
                return;
            }

            if (actions.optJSONObject(0).optString("Action").equals("Reload")){
                Intent intent = getIntent();
                String method = intent.getStringExtra(MainActivity.EXTRA_METHOD);
                String url = intent.getStringExtra(MainActivity.EXTRA_URL);
                String company = intent.getStringExtra(MainActivity.EXTRA_COMPANY);
                String body = intent.getStringExtra(MainActivity.EXTRA_BODY);
                new LoadFromServerFirst().execute(token, method, url, company, body);
                actions = JSONArrayRemoveAtIndex(actions, 0);
                runLocalActions(actions);
                return;
            }

            if (actions.optJSONObject(0).optString("Action").equals("MessageFlash")){
                String message = actions.optJSONObject(0).optString("Message");
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                actions = JSONArrayRemoveAtIndex(actions, 0);
                runLocalActions(actions);
                return;
            }
        }
    }

    private void createWebPrintJob(WebView webView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PrintManager printManager = (PrintManager)DetailedActivity.this.getSystemService(Context.PRINT_SERVICE);
            PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(webView.getTitle());
            printManager.print(webView.getTitle(), printAdapter, new PrintAttributes.Builder().build());
        }
    }
}