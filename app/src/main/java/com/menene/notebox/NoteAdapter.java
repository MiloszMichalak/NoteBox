package com.menene.notebox;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.realm.Realm;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    public interface OnNoteSelectedListener {
        void onNoteSelected(boolean isSelecting, int amount);
    }

    private final Context context;
    private List<NoteModel> notes;
    private final ActivityResultLauncher<Intent> launcher;
    private final Set<Integer> selectedNotes = new HashSet<>();
    private final OnNoteSelectedListener listener;
    private final Realm realm;
    private int layoutId;
    public Boolean isSelecting = false;

    public NoteAdapter(Context context, List<NoteModel> notes, ActivityResultLauncher<Intent> launcher, OnNoteSelectedListener listener, int layoutId) {
        this.context = context;
        this.notes = notes;
        this.launcher = launcher;
        this.listener = listener;
        this.layoutId = layoutId;
        this.realm = Utility.getRealmInstance(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoteModel note = notes.get(position);

        holder.bind(note, position);
        holder.setListeners();
    }

    public void stopSelecting() {
        isSelecting = false;
        selectedNotes.clear();
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (int i = 0; i < notes.size(); i++) {
            selectedNotes.add(i);
        }
        notifyDataSetChanged();
        listener.onNoteSelected(true, selectedNotes.size());
    }

    public void deselectAll() {
        selectedNotes.clear();
        notifyDataSetChanged();
        listener.onNoteSelected(true, 0);
    }

    public void deleteSelected() {
        realm.beginTransaction();
        for (int index : selectedNotes) {
            NoteModel note = notes.get(index);
            note.deleteFromRealm();
        }
        realm.commitTransaction();

        selectedNotes.clear();
        notifyDataSetChanged();
        listener.onNoteSelected(false, 0);
        isSelecting = false;
    }

    public void updateElements(List<NoteModel> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }

    public void setLayoutId(int layoutId) {
        this.layoutId = layoutId;
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, content, date;
        AppCompatCheckBox checkBox;
        RelativeLayout main;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            content = itemView.findViewById(R.id.content);
            date = itemView.findViewById(R.id.date);
            checkBox = itemView.findViewById(R.id.checkBox);
            main = itemView.findViewById(R.id.main);
        }

        public void bind(NoteModel note, int position) {
            title.setText(note.getTitle());
            content.setText(note.getContent());
            date.setText(Utility.getFormattedDate(note.getMilliseconds(), "dd MMM"));

            if (isSelecting) {
                checkBox.setVisibility(View.VISIBLE);
                checkBox.setChecked(selectedNotes.contains(position));

                animateCheckBox();

                if (layoutId == R.layout.note_item_list){
                    checkBox.post(() -> {
                        title.animate().translationX(checkBox.getWidth()).setDuration(300).start();
                        date.animate().translationX(checkBox.getWidth()).setDuration(300).start();
                    });
                }
            } else {
                checkBox.setVisibility(View.GONE);
                checkBox.setChecked(false);

                date.animate().translationX(0).setDuration(300).start();
                title.animate().translationX(0).setDuration(300).start();
            }
        }

        private void animateCheckBox() {
            if (!selectedNotes.isEmpty() && !(selectedNotes.size() == notes.size())) {
                checkBox.setScaleX(0);
                checkBox.setScaleY(0);
                checkBox.setAlpha(0);

                checkBox.animate()
                        .scaleX(1)
                        .scaleY(1)
                        .alpha(1)
                        .setDuration(300)
                        .start();
            }
        }

        public void setListeners() {
            main.setOnLongClickListener(v -> {
                toggleSelection();
                return true;
            });

            View.OnClickListener pressedListener = v -> {
                if (isSelecting) {
                    selectNote();
                } else {
                    EditNote();
                }
            };

            main.setOnClickListener(pressedListener);
            checkBox.setOnClickListener(pressedListener);
        }

        private void selectNote() {
            if (!selectedNotes.contains(getAdapterPosition())) {
                checkBox.setChecked(true);
                selectedNotes.add(getAdapterPosition());
            } else {
                checkBox.setChecked(false);
                selectedNotes.remove(getAdapterPosition());
            }
            listener.onNoteSelected(true, selectedNotes.size());
        }

        private void EditNote() {
            Intent intent = new Intent(context, NoteActivity.class);
            intent.putExtra("title", notes.get(getAdapterPosition()).getTitle());
            intent.putExtra("content", notes.get(getAdapterPosition()).getContent());
            intent.putExtra("milliseconds", notes.get(getAdapterPosition()).getMilliseconds());
            intent.putExtra("isEditing", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launcher.launch(intent);
        }

        private void toggleSelection() {
            checkBox.setChecked(true);
            selectedNotes.add(getAdapterPosition());
            isSelecting = true;
            notifyDataSetChanged();

            listener.onNoteSelected(true, selectedNotes.size());
        }
    }
}
