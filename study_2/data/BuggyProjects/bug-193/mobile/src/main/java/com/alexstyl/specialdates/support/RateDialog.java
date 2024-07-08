package com.alexstyl.specialdates.support;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alexstyl.specialdates.Navigator;
import com.alexstyl.specialdates.R;
import com.alexstyl.specialdates.ui.base.MementoActivity;
import com.novoda.notils.caster.Views;

public class RateDialog extends MementoActivity {

    private final String smiley = " " + Emoticon.SMILEY.asText();
    private AskForSupport askForSupport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_dialog);
        askForSupport = new AskForSupport(context());

        Views.findById(this, R.id.support_rate_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Navigator(v.getContext()).toPlayStore();
                Toast.makeText(context(), R.string.support_thanks_for_rating, Toast.LENGTH_LONG).show();
                askForSupport.onRateEnd();
                finish();
            }
        });

        Views.findById(this, R.id.support_cancel_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askForSupport.onRateEnd();
                finish();
            }
        });

        TextView textDescription = Views.findById(this, R.id.support_description);
        textDescription.append(smiley);

        ImageView imageView = Views.findById(this, R.id.support_heroimage);
        Animation pulse = AnimationUtils.loadAnimation(imageView.getContext(), R.anim.heartbeat);
        imageView.startAnimation(pulse);
    }

}
