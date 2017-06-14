package com.jiaozhu.earphonereciver;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private EditText text;
    private TextToSpeech tts;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (EditText) findViewById(R.id.text);
        tts = new TextToSpeech(this, this);
    }


    @Override
    public void onInit(int status) {
        Locale locale = Locale.getDefault();
        tts.setLanguage(locale);
        intent = getIntent();
        dealTextMessage(intent);
    }

    void dealTextMessage(Intent intent) {
        String share = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (share == null || share.equals("")) return;
        text.setText(share);
        speak(share);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        System.out.println("onNewIntent");
        dealTextMessage(intent);
        super.onNewIntent(intent);
    }

    @Override
    public void onDestroy() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private boolean speak(String str) {
        String[] strs = str.split("\\n|。");
        tts.stop();
        boolean flag = true;
        for (String temp : strs) {
            if (temp.equals("")) continue;
            if (tts.speak(temp, TextToSpeech.QUEUE_ADD, null, "") != 0) {
                flag = false;
                break;
            }
        }
        return flag;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_read:
                speak(text.getText().toString());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
