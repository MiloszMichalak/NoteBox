package com.menene.notebox;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;

import io.realm.Realm;

public class NoteActivity extends AppCompatActivity {
    Toolbar toolbar;
    Realm realm;
    TextInputEditText titleEditText, contentEditText;
    String title, content;
    Boolean isEditing;
    Intent intent;

    private void setupToolbar(){
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            content = contentEditText.getText().toString();
            title = titleEditText.getText().toString();

            handleNoteSaving();
            finish();
        });
    }

    private void handleNoteSaving() {
        if (!content.isEmpty() || !title.isEmpty()) {
            if (title.isEmpty()) {
                title = getString(R.string.text_note) + " " + Utility.getFormattedDate(System.currentTimeMillis(), "dd/MM");
            }

            if (!isEditing || !content.equals(intent.getStringExtra("content")) || !title.equals(intent.getStringExtra("title"))) {
                if (!isEditing){
                    AddNote();
                } else {
                    EditNote(intent.getLongExtra("milliseconds", 0));
                }
            }
            setResult(RESULT_OK);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        realm = Utility.getRealmInstance(this);

        setupUi();
        checkEditMode();
        setupToolbar();
        openKeyboard();
    }

    private void setupUi(){
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        toolbar = findViewById(R.id.toolbar);
        titleEditText = findViewById(R.id.title);
        contentEditText = findViewById(R.id.content);
    }

    private void checkEditMode(){
        intent = getIntent();
        isEditing = intent.getBooleanExtra("isEditing", false);

        if (isEditing) {
            titleEditText.setText(intent.getStringExtra("title"));
            contentEditText.setText(intent.getStringExtra("content"));
            contentEditText.setSelection(contentEditText.getText().length());
        }
    }

    private void openKeyboard(){
        contentEditText.requestFocus();
        contentEditText.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(contentEditText, InputMethodManager.SHOW_IMPLICIT);
        }, 200);
    }

    private void AddNote() {
        realm.beginTransaction();
        NoteModel note = realm.createObject(NoteModel.class);
        note.setContent(content);
        note.setTitle(title);
        note.setSeconds(System.currentTimeMillis());
        realm.commitTransaction();
    }

    private void EditNote(long milliseconds) {
        realm.executeTransaction(realm -> {
            NoteModel note = realm.where(NoteModel.class)
                    .equalTo("milliseconds", milliseconds)
                    .findFirst();
            if (note != null) {
                note.setContent(content);
                note.setTitle(title);
                note.setSeconds(System.currentTimeMillis());
            }
        });
    }
}