package com.menene.notebox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
    private TextView amountOfNotes, allNotes, allNotesText, sortOptions;
    private LinearLayout headerTitle;
    private AppCompatCheckBox selectAll;
    private AppCompatTextView selectAllText;
    private BottomNavigationView bottomNavigationView;
    private ImageView optionsBtn, sortingArrow;
    private PopupMenu popupMenu, sortMenu;

    private RealmResults<NoteModel> notes;
    private NoteAdapter adapter;
    private String sortOrder, sortType;
    SharedPreferences prefs;
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
        setupUi();
        initializePreferences();
        initRealm();
        setupBottomNav();
        setupRecyclerView();
        setupPopmenu();
        setupSortMenu();
        setupListeners();
        setupOnBackPressed();
        setupHeaderAlphaAnimation();
    }

    private void setupUi() {
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
        sortOptions = findViewById(R.id.sort);
        sortingArrow = findViewById(R.id.sorting_order);
        recyclerView = findViewById(R.id.recyclerView);
        selectAll = findViewById(R.id.select_all);
        selectAllText = findViewById(R.id.select_all_text);
        allNotesText = findViewById(R.id.all_notes_text);
        bottomNavigationView = findViewById(R.id.bottom_nav);
        optionsBtn = findViewById(R.id.options);
    }

    private void setupBottomNav() {
        bottomNavigationView.post(() -> bottomNavigationView.setTranslationY(bottomNavigationView.getHeight()));

        bottomNavigationView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsets onApplyWindowInsets(@NonNull View view, @NonNull WindowInsets insets) {
                view.setPadding(
                        view.getPaddingLeft(),
                        view.getPaddingTop(),
                        view.getPaddingRight(),
                        0
                );
                return insets;
            }
        });
    }

    private void initializePreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        editor = prefs.edit();
        sortType = prefs.getString(SORT_TYPE, "title");
        sortOrder = prefs.getString(SORT_ORDER, "asc");
    }

    private void initRealm() {
        Realm realm = Utility.getRealmInstance(getApplicationContext());
        notes = realm.where(NoteModel.class)
                .sort(sortType, sortOrder.equals("asc") ? Sort.ASCENDING : Sort.DESCENDING)
                .findAll();
        amountOfNotes.setText(String.valueOf(notes.size()));
    }

    private void setupRecyclerView() {
        int columns = prefs.getInt(GRID_LAYOUT_PREF, 3);
        recyclerView.setLayoutManager(new GridLayoutManager(this, columns));

        int layoutId = columns == 1 ? R.layout.note_item_list : R.layout.note_item;
        adapter = new NoteAdapter(getApplicationContext(), notes, resultLauncher, this, layoutId);
        recyclerView.setAdapter(adapter);
    }

    private void setLayout(int spanCount, int layoutId) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        editor.putInt(GRID_LAYOUT_PREF, spanCount);
        adapter.setLayoutId(layoutId);
    }

    private void setupPopmenu() {
        popupMenu = new PopupMenu(this, optionsBtn);
        popupMenu.getMenuInflater().inflate(R.menu.options_menu, popupMenu.getMenu());

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
    }

    private void showSubMenu() {
        PopupMenu popupMenu = new PopupMenu(this, optionsBtn);
        popupMenu.getMenuInflater().inflate(R.menu.view_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.grid_small) {
                setLayout(3, R.layout.note_item);
            } else if (item.getItemId() == R.id.grid_medium) {
                setLayout(2, R.layout.note_item);
            } else if (item.getItemId() == R.id.list) {
                setLayout(1, R.layout.note_item_list);
            }
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            editor.apply();
            return true;
        });

        popupMenu.show();
    }

    private void changeSortingType(String type, Sort sort, int resId) {
        notes = notes.sort(type, sort);
        editor.putString(SORT_TYPE, type);
        sortOptions.setText(getString(resId));
    }

    private void setupSortMenu() {
        sortMenu = new PopupMenu(this, sortOptions);
        sortMenu.getMenuInflater().inflate(R.menu.sortmenu, sortMenu.getMenu());

        sortMenu.setOnMenuItemClickListener(item -> {
            sortOrder = prefs.getString(SORT_ORDER, "asc");
            Sort sort = sortOrder.equals("asc") ? Sort.ASCENDING : Sort.DESCENDING;

            if (item.getItemId() == R.id.title) {
                changeSortingType("title", sort, R.string.title);
            } else if (item.getItemId() == R.id.createDate) {
                changeSortingType("milliseconds", sort, R.string.create_date);
            }
            adapter.updateElements(notes);
            editor.apply();

            return true;
        });
    }

    private void setupListeners() {
        addNoteBtn.setOnClickListener(v -> resultLauncher.launch(new Intent(this, NoteActivity.class)));

        optionsBtn.setOnClickListener(v -> popupMenu.show());

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.deleteBtn) {
                adapter.deleteSelected();
                return true;
            }
            return false;
        });

        selectAll.setOnClickListener(v -> {
            if (selectAll.isChecked()) {
                adapter.selectAll();
            } else {
                adapter.deselectAll();
            }
        });

        sortOptions.setOnClickListener(v -> sortMenu.show());

        setupSortingArrow();
    }

    private void setupSortingArrow() {
        sortOptions.setText(sortType.equals("title") ? R.string.title : R.string.create_date);
        sortingArrow.setImageResource(sortOrder.equals("asc") ? R.drawable.up_arrow : R.drawable.down_arrow);

        sortingArrow.setTag(sortOrder);

        sortingArrow.setOnClickListener(v -> {
            sortType = prefs.getString(SORT_TYPE, "title");
            if (sortingArrow.getTag().equals("asc")) {
                changeSortingOrder(R.drawable.down_arrow, "desc", Sort.DESCENDING);
            } else {
                changeSortingOrder(R.drawable.up_arrow, "asc", Sort.ASCENDING);
            }
            editor.apply();
            adapter.updateElements(notes);
        });
    }

    private void changeSortingOrder(int resId, String orderType, Sort sortingType) {
        sortingArrow.setImageResource(resId);
        sortingArrow.setTag(orderType);
        notes = notes.sort(sortType, sortingType);
        editor.putString(SORT_ORDER, orderType);
    }

    private void setupOnBackPressed() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (adapter.isSelecting) {
                    adapter.stopSelecting();
                    onNoteSelected(false, 0);

                    resetUi();
                } else {
                    finish();
                }
            }
        });
    }

    private void setupHeaderAlphaAnimation() {
        AppBarLayout header = findViewById(R.id.toolbarLayout);

        header.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            int totalScrollRange = appBarLayout.getTotalScrollRange();
            float scrollFactor = (float) Math.abs(verticalOffset) / totalScrollRange;

            headerTitle.setAlpha(0.8f - scrollFactor);
            allNotes.setAlpha(scrollFactor);
        });
    }

    public void resetUi() {
        allNotesText.setText(getString(R.string.all_notes));
        amountOfNotes.setVisibility(View.VISIBLE);
        amountOfNotes.setText(String.valueOf(notes.size()));
        allNotes.setVisibility(View.VISIBLE);
        addNoteBtn.show();
        bottomNavigationView.post(() -> bottomNavigationView.animate().translationY(bottomNavigationView.getHeight()).setDuration(500).start());

        optionsBtn.setVisibility(View.VISIBLE);
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

            bottomNavigationView.post(() -> bottomNavigationView.animate().translationY(amount > 0 ? 0 : bottomNavigationView.getHeight()).setDuration(700).start());
        } else {
            selectAll.setVisibility(ImageView.GONE);
            selectAllText.setVisibility(View.GONE);

            resetUi();
        }
    }
}