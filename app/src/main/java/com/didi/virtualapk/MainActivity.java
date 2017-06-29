package com.didi.virtualapk;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.didi.virtualapk.internal.PluginContentResolver;
import com.didi.virtualapk.internal.LoadedPlugin;

import java.io.File;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView textView = (TextView)findViewById(R.id.textView);
        String cpuArch;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cpuArch = Build.SUPPORTED_ABIS[0];
        } else {
            cpuArch = Build.CPU_ABI;
        }
        textView.setText(cpuArch);
        Log.d("ryg", "onCreate cpu arch is "+ cpuArch);
        this.loadPlugin(this);
        Log.d("ryg", "onCreate classloader is "+ getClassLoader());
    }

    public void onButtonClick(View v) {
        if (v.getId() == R.id.button) {
            final String pkg = "com.didi.virtualapk.demo";
            if (PluginManager.getInstance(this).getLoadedPlugin(pkg) == null) {
                Toast.makeText(this, "plugin [com.didi.virtualapk.demo] not loaded", Toast.LENGTH_SHORT).show();
                return;
            }

            // test Activity and Service
            Intent intent = new Intent();
            intent.setClassName(pkg, "com.didi.virtualapk.demo.aidl.BookManagerActivity");
            startActivity(intent);

            // test ContentProvider
            Uri bookUri = Uri.parse("content://com.didi.virtualapk.demo.book.provider/book");
            LoadedPlugin plugin = PluginManager.getInstance(this).getLoadedPlugin(pkg);
            bookUri = PluginContentResolver.wrapperUri(plugin, bookUri);

            Cursor bookCursor = getContentResolver().query(bookUri, new String[]{"_id", "name"}, null, null, null);
            while (bookCursor.moveToNext()) {
                int bookId = bookCursor.getInt(0);
                String bookName = bookCursor.getString(1);
                Log.d("ryg", "query book:" + bookId + ", " + bookName);
            }
            bookCursor.close();
        } else if (v.getId() == R.id.about) {
            showAbout();
        }
    }

    private void loadPlugin(Context base) {
        PluginManager pluginManager = PluginManager.getInstance(base);
        File apk = new File(Environment.getExternalStorageDirectory(), "Test.apk");
        if (apk.exists()) {
            try {
                pluginManager.loadPlugin(apk);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showAbout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.about_detail);
        builder.setTitle("关于");
        builder.setNegativeButton("好的", null);
        builder.show();
    }
}
