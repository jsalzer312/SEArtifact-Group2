package com.alexstyl.specialdates.ui.base;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.novoda.notils.exception.DeveloperError;

public class MementoActivity extends AppCompatActivity {

    /**
     * Override this method in order to let the activity handle the up button.
     * When pressed it will navigate the user to the parent of the activity
     */
    protected boolean shouldUseHomeAsUp() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (shouldUseHomeAsUp()) {
            ActionBar bar = getSupportActionBar();
            if (bar != null) {
                bar.setHomeButtonEnabled(true);
                bar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            return handleUp();
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean handleUp() {
        if (!shouldUseHomeAsUp()) {
            return false;
        }
        Intent parent = getSupportParentActivityIntent();
        complainForNoSetParent(parent);
        parent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(parent);
        return true;
    }

    private void complainForNoSetParent(Intent parent) {
        if (parent == null) {
            throw new DeveloperError("Make sure to set parent Activity through the AndroidManifest if you want to use shouldUseHomeAsUp()");
        }
    }

    protected Context context() {
        return this;
    }

}
