package com.menene.notebox;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteSelectedListener {
    RecyclerView recyclerView;
    FloatingActionButton addNoteBtn;
    Realm realm;
    RealmResults<NoteModel> notes;
    TextView amountOfNotes, allNotes;
    AppBarLayout header;
    LinearLayout headerTitle;
    AppCompatCheckBox selectAll;
    AppCompatTextView selectAllText;
    TextView allNotesText;
    BottomNavigationView bottomNavigationView;
    NoteAdapter adapter;

    private final ActivityResultLauncher<Intent> resultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == RESULT_OK) {
                    amountOfNotes.setText(String.valueOf(notes.size()));
                }
            });

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        addNoteBtn = findViewById(R.id.addNoteBtn);

        amountOfNotes = findViewById(R.id.amountOfNotes);
        allNotes = findViewById(R.id.allNotes);

        headerTitle = findViewById(R.id.headerTitle);

        addNoteBtn.setOnClickListener(v -> resultLauncher.launch(new Intent(this, NoteActivity.class)));

        realm = Utility.getRealmInstance(getApplicationContext());
        notes = realm.where(NoteModel.class).findAll();
        amountOfNotes.setText(String.valueOf(notes.size()));

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getApplicationContext(), 3));

        adapter = new NoteAdapter(getApplicationContext(), notes, resultLauncher, this);
        recyclerView.setAdapter(adapter);

        header = findViewById(R.id.toolbarLayout);

        selectAll = findViewById(R.id.select_all);
        selectAllText = findViewById(R.id.select_all_text);
        allNotesText = findViewById(R.id.all_notes_text);

        bottomNavigationView = findViewById(R.id.bottom_nav);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.deleteBtn) {
                adapter.deleteSelected();
                return true;
            }
            return false;
        });

        header.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            float scrollFactor = (float) Math.abs(verticalOffset) / totalScrollRange + 0.2f;

            headerTitle.setAlpha(1 - scrollFactor);
            allNotes.setAlpha(scrollFactor - 0.2f);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                adapter.stopSelecting();
                onNoteSelected(false, 0);

                Utility.resetUi(amountOfNotes, allNotesText, allNotes, addNoteBtn, bottomNavigationView, getString(R.string.all_notes), notes.size());
            }
        });

        selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked){
                if (selectAll.isChecked()) {
                    adapter.selectAll();
                } else {
                    adapter.deselectAll();
                }
            }
        });
    }

    @Override
    public void onNoteSelected(boolean isSelecting, int amount) {
        if (isSelecting) {
            selectAll.setChecked(notes.size() == amount);

            selectAll.setVisibility(ImageView.VISIBLE);
            selectAllText.setVisibility(View.VISIBLE);

            allNotes.setVisibility(View.INVISIBLE);
            amountOfNotes.setVisibility(View.INVISIBLE);

            allNotesText.setText(getString(R.string.selected, amount));
            addNoteBtn.hide();

            if (amount > 0) {
                bottomNavigationView.setVisibility(View.VISIBLE);
            } else {
                bottomNavigationView.setVisibility(View.INVISIBLE);
            }
        } else {
            selectAll.setVisibility(ImageView.GONE);
            selectAllText.setVisibility(View.GONE);

            Utility.resetUi(amountOfNotes, allNotesText, allNotes, addNoteBtn, bottomNavigationView, getString(R.string.all_notes), notes.size());
        }
    }
}