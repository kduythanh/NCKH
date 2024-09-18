package com.example.nlcs.ui.activities.auth;

import android.content.Intent;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.example.nlcs.adapter.viewpager.OnboardingAdapter;
import com.example.nlcs.databinding.ActivityAuthenticationBinding;
import com.example.nlcs.ui.activities.FlashcardActivity;

public class AuthenticationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout
        ActivityAuthenticationBinding binding = ActivityAuthenticationBinding.inflate(getLayoutInflater());
        final View view = binding.getRoot();
        setContentView(view);

        // Setup onboarding
        OnboardingAdapter onboardingAdapter = new OnboardingAdapter(this);
        binding.onboardingVp.setAdapter(onboardingAdapter);
        binding.indicator.setViewPager(binding.onboardingVp);

        // Setup view now button
        binding.startNowBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, FlashcardActivity.class));
            finish();
        });

    }
}