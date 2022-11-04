package com.example.javaobjdetection.Fragments;

import androidx.appcompat.app.AppCompatActivity;

import com.example.javaobjdetection.PhysicsActivity;
import com.google.android.material.snackbar.Snackbar;

public class CheckableSnackBar extends AppCompatActivity {
    boolean active;
    ColourSelectionDialog colourSelect;
    Snackbar snackbar;
    PhysicsActivity activity;

    public CheckableSnackBar(PhysicsActivity activity) {
        initialise(activity);
    }

    public void initialise(PhysicsActivity activity) {

    }

    public void show() {
        if (!active) {
            active = true;
            snackbar.show();
        }
    }

    public void dismiss() {
        if (active) {
            active = false;
            snackbar.dismiss();
        }
    }

    public boolean isActive() {
        return active;
    }
}
