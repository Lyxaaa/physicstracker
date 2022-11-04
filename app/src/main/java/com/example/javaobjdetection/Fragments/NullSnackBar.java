package com.example.javaobjdetection.Fragments;

import android.view.View;

import com.example.javaobjdetection.PhysicsActivity;
import com.example.javaobjdetection.R;
import com.google.android.material.snackbar.Snackbar;

public class NullSnackBar extends CheckableSnackBar {

    public NullSnackBar(PhysicsActivity activity) {
        super(activity);
    }
    @Override
    public void initialise(PhysicsActivity activity) {
        this.activity = activity;

        active = false;

        colourSelect = new ColourSelectionDialog();

        snackbar = Snackbar.make(activity.findViewById(R.id.camera_view_clickable), R.string.none_selected, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.more_info, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colourSelect.show(getSupportFragmentManager(), "Ambiguous Selection");
            }
        });
    }
}
