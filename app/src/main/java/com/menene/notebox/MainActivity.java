package com.menene.notebox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import io.realm.Sort;

public class MainActivity extends AppCompatActivity implements NoteAdapter.OnNoteSelectedListener {
    private RecyclerView recyclerView;
    private FloatingActionButton addNoteBtn;
    private RealmResults<NoteModel> notes;
    private TextView amountOfNotes, allNotes;
    private LinearLayout headerTitle;
    private AppCompatCheckBox selectAll;
    private AppCompatTextView selectAllText;
    private TextView allNotesText;
    private BottomNavigationView bottomNavigationView;
    private NoteAdapter adapter;
    private ImageView optionsBtn, sortingArrow;
    private PopupMenu popupMenu, sortMenu;
    private String sortOrder;
    private String sortType;
    private TextView sortOptions;
    private SharedPreferences.Editor editor;

    private static final String PREFS_NAME = "prefs";
    private static final String GRID_LAYOUT_PREF = "grid_layout_pref";
    private static final String SORT_TYPE = "sort_type";
    private static final String SORT_ORDER = "sort_order";

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

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int columns = prefs.getInt(GRID_LAYOUT_PREF, 3);

        addNoteBtn = findViewById(R.id.addNoteBtn);

        amountOfNotes = findViewById(R.id.amountOfNotes);
        allNotes = findViewById(R.id.allNotes);

        headerTitle = findViewById(R.id.headerTitle);

        sortOptions = findViewById(R.id.sort);

        sortingArrow = findViewById(R.id.sorting_order);

        addNoteBtn.setOnClickListener(v -> resultLauncher.launch(new Intent(this, NoteActivity.class)));

        Realm realm = Utility.getRealmInstance(getApplicationContext());

        sortType = prefs.getString(SORT_TYPE, "title");
        sortOrder = prefs.getString(SORT_ORDER, "asc");
        sortOptions.setText(sortType.equals("title")
                ? getString(R.string.title) : getString(R.string.createDate));
        sortingArrow.setImageResource(sortOrder.equals("asc") ? R.drawable.up_arrow : R.drawable.down_arrow);

        notes = realm.where(NoteModel.class)
                .sort(sortType, sortOrder.equals("asc") ? Sort.ASCENDING : Sort.DESCENDING)
                .findAll();
        amountOfNotes.setText(String.valueOf(notes.size()));

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, columns));

        if (columns == 1){
            adapter = new NoteAdapter(getApplicationContext(), notes, resultLauncher, this, R.layout.note_item_list);
        } else {
            adapter = new NoteAdapter(getApplicationContext(), notes, resultLauncher, this, R.layout.note_item);
        }
        recyclerView.setAdapter(adapter);

        AppBarLayout header = findViewById(R.id.toolbarLayout);

        selectAll = findViewById(R.id.select_all);
        selectAllText = findViewById(R.id.select_all_text);
        allNotesText = findViewById(R.id.all_notes_text);

        bottomNavigationView = findViewById(R.id.bottom_nav);

        optionsBtn = findViewById(R.id.options);

        popupMenu = new PopupMenu(this, optionsBtn);
        popupMenu.getMenuInflater().inflate(R.menu.options_menu, popupMenu.getMenu());

        optionsBtn.setOnClickListener(v -> popupMenu.show());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.edit) {
                onNoteSelected(true, 0);
                adapter.notifyDataSetChanged();

                return true;
            } else if (item.getItemId() == R.id.view) {
               showSubMenu();
               return true;
            }
            return false;
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.deleteBtn) {
                adapter.deleteSelected();
                return true;
            }
            return false;
        });

        header.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            float scrollFactor = (float) Math.abs(verticalOffset) / totalScrollRange;

            headerTitle.setAlpha(0.8f - scrollFactor);
            allNotes.setAlpha(scrollFactor);
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (adapter.isSelecting) {
                    adapter.stopSelecting();
                    onNoteSelected(false, 0);

                    Utility.resetUi(amountOfNotes, allNotesText, allNotes, addNoteBtn, bottomNavigationView,
                            getString(R.string.all_notes), notes.size(), optionsBtn);
                } else {
                    finish();
                }
            }
        });

        selectAll.setOnClickListener(v -> {
            if (selectAll.isChecked()) {
                adapter.selectAll();
            } else {
                adapter.deselectAll();
            }
        });

        sortOptions = findViewById(R.id.sort);

        sortMenu = new PopupMenu(this, sortOptions);
        sortMenu.getMenuInflater().inflate(R.menu.sortmenu, sortMenu.getMenu());

        sortOptions.setOnClickListener(v -> sortMenu.show());

        editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();

        sortMenu.setOnMenuItemClickListener(item -> {
            sortOrder = prefs.getString(SORT_ORDER, "asc");
            Sort sort = sortOrder.equals("asc") ? Sort.ASCENDING : Sort.DESCENDING;

            if (item.getItemId() == R.id.title) {
                notes = notes.sort("title", sort);
                editor.putString(SORT_TYPE, "title");
                sortOptions.setText(getString(R.string.title));
            } else if (item.getItemId() == R.id.createDate) {
                notes = notes.sort("milliseconds", sort);
                editor.putString(SORT_TYPE, "milliseconds");
                sortOptions.setText(getString(R.string.createDate));
            }
            adapter.updateElements(notes);
            editor.apply();

            return true;
        });

        sortingArrow.setTag(sortOrder);

        sortingArrow.setOnClickListener(v -> {
            sortType = prefs.getString(SORT_TYPE, "title");
            if (sortingArrow.getTag().equals("asc")){
                sortingArrow.setImageResource(R.drawable.down_arrow);
                sortingArrow.setTag("desc");
                notes = notes.sort(sortType, Sort.DESCENDING);
                editor.putString(SORT_ORDER, "desc");
            } else {
                sortingArrow.setImageResource(R.drawable.up_arrow);
                sortingArrow.setTag("asc");
                notes = notes.sort(sortType, Sort.ASCENDING);
                editor.putString(SORT_ORDER, "asc");
            }
            editor.apply();
            adapter.updateElements(notes);
        });
    }

    private void showSubMenu() {
        PopupMenu popupMenu = new PopupMenu(this, optionsBtn);
        popupMenu.getMenuInflater().inflate(R.menu.view_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item1 -> {
            if (item1.getItemId() == R.id.grid_small) {
                setLayout(3, R.layout.note_item);
            } else if (item1.getItemId() == R.id.grid_medium) {
                setLayout(2, R.layout.note_item);
            } else if (item1.getItemId() == R.id.list){
                setLayout(1, R.layout.note_item_list);
            }
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            editor.apply();
            return true;
        });

        popupMenu.show();
    }

    private void setLayout(int spanCount, int layoutId) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        editor.putInt(GRID_LAYOUT_PREF, spanCount);
        adapter.setLayoutId(layoutId);
    }

    @Override
    public void onNoteSelected(boolean isSelecting, int amount) {
        if (isSelecting) {
            selectAll.setChecked(notes.size() == amount);
            selectAll.setVisibility(ImageView.VISIBLE);

            selectAllText.setVisibility(View.VISIBLE);

            allNotes.setVisibility(View.INVISIBLE);
            amountOfNotes.setVisibility(View.INVISIBLE);

            optionsBtn.setVisibility(View.GONE);

            adapter.isSelecting = true;

            allNotesText.setText(getString(R.string.selected, amount));
            addNoteBtn.hide();

            bottomNavigationView.setVisibility(amount > 0 ? View.VISIBLE : View.INVISIBLE);
        } else {
            selectAll.setVisibility(ImageView.GONE);
            selectAllText.setVisibility(View.GONE);

            Utility.resetUi(amountOfNotes, allNotesText, allNotes, addNoteBtn, bottomNavigationView, getString(R.string.all_notes), notes.size(), optionsBtn);
        }
    }
}