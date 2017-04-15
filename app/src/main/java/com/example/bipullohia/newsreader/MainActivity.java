package com.example.bipullohia.newsreader;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Top 5 News");

        ListView newsListView = (ListView) findViewById(R.id.newsListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        newsListView.setAdapter(arrayAdapter);

        newsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", content.get(i));
                startActivity(intent);

            }
        });

        articlesDB = openOrCreateDatabase("Articles", MODE_PRIVATE, null);

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        updateListView();

        try {
            DownloadInfoTask task = new DownloadInfoTask();
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void updateListView() {

        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);

        int titleindex = c.getColumnIndex("title");
        int contentIndex = c.getColumnIndex("content");

        if (c.moveToFirst()) {

            titles.clear();
            content.clear();

            do {

                titles.add(c.getString(titleindex));
                content.add(c.getString(contentIndex));

            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();

        }


    }

    public class DownloadInfoTask extends AsyncTask<String, Void, String> {


        ProgressDialog pd = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd.setMessage("Loading news...");
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

                Log.i("content", result);

                JSONArray array = new JSONArray(result);

                int maxnum = 5;

                if (array.length() < 5) {
                    maxnum = array.length();
                }

                articlesDB.execSQL("DELETE FROM articles");

                for (int i = 0; i < maxnum; i++) {

                    String articleId = array.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleId + ".json?print=pretty");

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

                    if ((!jo.isNull("title")) && (!jo.isNull("url"))) {

                        String articleTitle = jo.getString("title");
                        String articleUrl = jo.getString("url");

                        url = new URL(articleUrl);

                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        in = httpURLConnection.getInputStream();

                        reader = new InputStreamReader(in);
                        data = reader.read();

                        String articleContent = "";

                        while (data != -1) {

                            char current = (char) data;
                            articleContent += current;

                            data = reader.read();
                        }

                        //Log.i("article content", articleContent);

                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)";

                        SQLiteStatement statement = articlesDB.compileStatement(sql);

                        statement.bindString(1, articleId);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

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
