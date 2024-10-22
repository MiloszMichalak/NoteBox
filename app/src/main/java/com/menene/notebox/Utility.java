package com.menene.notebox;


import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class Utility {
public static String getFormattedDate(long milliseconds, String format) {
        Date date = new Date(milliseconds);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format, Locale.getDefault());
        return simpleDateFormat.format(date);
    }

    public static Realm getRealmInstance(Context context){
        io.realm.Realm.init(context);
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .name("notebox.realm")
                .schemaVersion(1)
                .allowWritesOnUiThread(true)
                .build();
        io.realm.Realm.setDefaultConfiguration(realmConfiguration);
        return io.realm.Realm.getInstance(realmConfiguration);
    }

    public static void resetUi(TextView amountOfNotes, TextView allNotesText, TextView allNotes, FloatingActionButton addNoteBtn, BottomNavigationView bottomNavigationView,
                               String notesText, int amount) {
        allNotesText.setText(notesText);
        amountOfNotes.setVisibility(View.VISIBLE);
        amountOfNotes.setText(String.valueOf(amount));
        allNotes.setVisibility(View.VISIBLE);
        addNoteBtn.show();
        bottomNavigationView.setVisibility(View.INVISIBLE);
    }
}
