package com.menene.notebox;


import android.content.Context;

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
}
