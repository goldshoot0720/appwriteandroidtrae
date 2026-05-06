package com.example.appwriteandroidtrae;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Consumer;

public class VoiceInputHelper {

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<Intent> speechLauncher;
    private String prompt;
    private Consumer<String> confirmedCallback;

    public VoiceInputHelper(AppCompatActivity activity) {
        this.activity = activity;
        this.speechLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    ArrayList<String> matches = result.getData()
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches == null || matches.isEmpty()) {
                        Toast.makeText(activity, R.string.voice_no_result, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    confirm(matches.get(0));
                }
        );
    }

    public void bindTextInput(TextInputLayout inputLayout, EditText editText, int promptRes) {
        inputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        inputLayout.setEndIconDrawable(android.R.drawable.ic_btn_speak_now);
        inputLayout.setEndIconContentDescription(activity.getString(R.string.voice_input));
        inputLayout.setEndIconOnClickListener(v -> start(
                activity.getString(promptRes),
                text -> {
                    editText.setText(text);
                    editText.setSelection(editText.getText() != null ? editText.getText().length() : 0);
                }
        ));
    }

    public void start(String prompt, Consumer<String> confirmedCallback) {
        this.prompt = prompt;
        this.confirmedCallback = confirmedCallback;

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.TAIWAN.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.TAIWAN.toLanguageTag());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);

        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(activity, R.string.voice_not_available, Toast.LENGTH_LONG).show();
        }
    }

    private void confirm(String text) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.voice_confirm_title)
                .setMessage(text)
                .setPositiveButton(R.string.voice_confirm_apply, (dialog, which) -> {
                    if (confirmedCallback != null) {
                        confirmedCallback.accept(text);
                    }
                })
                .setNeutralButton(R.string.voice_confirm_retry, (dialog, which) ->
                        start(prompt != null ? prompt : activity.getString(R.string.voice_input), confirmedCallback))
                .setNegativeButton(R.string.ui_close, null)
                .show();
    }
}
