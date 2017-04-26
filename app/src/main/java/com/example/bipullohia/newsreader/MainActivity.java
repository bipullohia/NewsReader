package com.example.bipullohia.newsreader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;
    SharedPreferences sharedPreferences = null;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuinflater = getMenuInflater();
        menuinflater.inflate(R.menu.actionbar_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.refresh) {

            DownloadInfo();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(getString(R.string.mySP), MODE_PRIVATE);

        if (sharedPreferences.getBoolean((getString(R.string.SPfirstRun)), true)) {

            DownloadInfo();
            sharedPreferences.edit().putBoolean(getString(R.string.SPfirstRun), false).apply();

        }

        setTitle(getString(R.string.AppTitle));

        ListView newsListView = (ListView) findViewById(R.id.newsListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        newsListView.setAdapter(arrayAdapter);

        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("urls", urls.get(i));
                intent.putExtra("title", titles.get(i));
                startActivity(intent);

            }
        });

        articlesDB = openOrCreateDatabase(getString(R.string.tableNameinDB), MODE_PRIVATE, null);
        articlesDB.execSQL(getString(R.string.SQLCreateDB));

        updateListView();

    }

    private void DownloadInfo() {

        try {

            DownloadInfoTask task = new DownloadInfoTask();
            task.execute(getString(R.string.topNewsUrl));

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    public void updateListView() {

        Cursor c = articlesDB.rawQuery(getString(R.string.SQLSelectAllFromArticle), null);

        int titleindex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("urls");

        if (c.moveToFirst()) {

            titles.clear();
            urls.clear();

            do {

                titles.add(c.getString(titleindex));
                urls.add(c.getString(contentIndex));

            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();

        }

    }

    public class DownloadInfoTask extends AsyncTask<String, Void, String> {


        ProgressDialog pd = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd.setMessage(getString(R.string.processDislogueStart));
            pd.setCancelable(false);
            pd.setCanceledOnTouchOutside(false);
            pd.show();

        }

        @Override
        protected String doInBackground(String... strings) {

            String result = "";
            URL url;
            HttpURLConnection httpURLConnection = null;

            try {

                url = new URL(strings[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();

                InputStream in = httpURLConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {

                    char current = (char) data;
                    result += current;

                    data = reader.read();

                }

                JSONArray array = new JSONArray(result);

                int maxnum = 20;

                if (array.length() < 20) {
                    maxnum = array.length();
                }

                articlesDB.execSQL(getString(R.string.SQLDeleteFromArticles));

                for (int i = 0; i < maxnum; i++) {

                    String articleId = array.getString(i);

                    url = new URL(getString(R.string.articleUrl) + articleId + getString(R.string.articleUrl2));

                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    in = httpURLConnection.getInputStream();

                    reader = new InputStreamReader(in);
                    data = reader.read();

                    String articleInfo = "";

                    while (data != -1) {

                        char current = (char) data;
                        articleInfo += current;

                        data = reader.read();
                    }

                    JSONObject jo = new JSONObject(articleInfo);

                    if ((!jo.isNull(getString(R.string.title))) && (!jo.isNull(getString(R.string.url)))) {

                        String articleTitle = jo.getString(getString(R.string.title));
                        String articleUrl = jo.getString(getString(R.string.url));

//                        url = new URL(articleUrl);
//
//                        httpURLConnection = (HttpURLConnection) url.openConnection();
//                        in = httpURLConnection.getInputStream();
//
//                        reader = new InputStreamReader(in);
//                        data = reader.read();
//
//                        String articleContent = "";
//
//                        while (data != -1) {
//
//                            char current = (char) data;
//                            articleContent += current;
//
//                            data = reader.read();
//                        }

                        //Log.i("article content", articleContent);

                        String sql = getString(R.string.SQLInsertIntoArticles);

                        SQLiteStatement statement = articlesDB.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleUrl);

                        statement.execute();

                    }

                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return null;

        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
            pd.dismiss();
        }
    }
}
