package com.example.kthrya.myapplication3;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.util.Log;

public class SettingActivity extends PreferenceActivity {
    private PreferenceScreen screen;
    private ListPreference moveMode;
    private SwitchPreference alarmMode;
    SharedPreferences sp;
    SharedPreferences.Editor editor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        sp = getSharedPreferences("CONFIG", Context.MODE_PRIVATE);
        editor = sp.edit();

        screen = getPreferenceScreen();

        moveMode = (ListPreference)screen.findPreference("mode_move_list");
        alarmMode = (SwitchPreference)screen.findPreference("mode_alarm_fire");

        moveMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue((String) newValue);

                moveMode.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                editor.putString("MOVE", listPreference.getEntryValues()[index].toString());
                editor.commit();
                return true;
            }
        });
        alarmMode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                 boolean isChecked = (boolean) newValue;
                 if(isChecked) {
                     alarmMode.setSwitchTextOn("ON");
                     editor.putBoolean("FIRE", true);
                 }
                 else{
                     alarmMode.setSwitchTextOff("OFF");
                     editor.putBoolean("FIRE",false);
                 }
                 editor.commit();
                 return true;
                }
         });
    }

    public void onResume(){
        super.onResume();
        updateSummary();
    }

    private void updateSummary(){
        moveMode.setSummary(moveMode.getEntry());
        sp.getString("MOVE","0");
        sp.getBoolean("FIRE",true);
    }
}


