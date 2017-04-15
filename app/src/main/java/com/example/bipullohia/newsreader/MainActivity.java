package com.example.bipullohia.newsreader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    ArrayAdapter arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView newsListView = (ListView) findViewById(R.id.newsListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        newsListView.setAdapter(arrayAdapter);

        try {
            DownloadInfoTask task = new DownloadInfoTask();
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public class DownloadInfoTask extends AsyncTask<String, Void, String> {


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

                int maxnum = 20;

                if (array.length() < 20) {
                    maxnum = array.length();
                }

                for (int i = 0; i < maxnum; i++) {

                    String newsId = array.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + newsId + ".json?print=pretty");

                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    in = httpURLConnection.getInputStream();

                    reader = new InputStreamReader(in);
                    data = reader.read();

                    String articleInfo = "";

                    while(data!=-1){

                        char current = (char) data;
                        articleInfo += current;

                        data = reader.read();
                    }

                    JSONObject jo = new JSONObject(articleInfo);

                    if((!jo.isNull("title")) && (!jo.isNull("url"))){

                        String articleTitle = jo.getString("title");
                        String articleUrl = jo.getString("url");

                        url = new URL(articleUrl);

                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        in = httpURLConnection.getInputStream();

                        reader = new InputStreamReader(in);
                        data = reader.read();

                        String articleContent = "";

                        while(data!=-1){

                            char current = (char) data;
                            articleContent += current;

                            data = reader.read();
                        }

                        Log.i("article content", articleContent);

                        



                    }

                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }


            return null;


        }
    }
}
