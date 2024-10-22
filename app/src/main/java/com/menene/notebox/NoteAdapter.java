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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    public interface OnNoteSelectedListener {
        void onNoteSelected(boolean isSelecting, int amount);
    }

    private Context context;
    private List<NoteModel> notes;
    private ActivityResultLauncher<Intent> launcher;
    private Map<Integer, Long> selectedNotes = new HashMap<>();
    private Boolean isSelecting = false;
    private OnNoteSelectedListener listener;
    private Realm realm;

    public NoteAdapter(Context context, List<NoteModel> notes, ActivityResultLauncher<Intent> launcher, OnNoteSelectedListener listener) {
        this.context = context;
        this.notes = notes;
        this.launcher = launcher;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.note_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteAdapter.ViewHolder holder, int position) {
        NoteModel note = notes.get(position);

        holder.title.setText(note.getTitle());
        holder.content.setText(note.getContent());
        holder.date.setText(Utility.getFormattedDate(note.getMilliseconds(), "dd MMM"));

        realm = Utility.getRealmInstance(context);

        if (isSelecting) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(selectedNotes.containsKey(position));
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.checkBox.setChecked(!selectedNotes.containsKey(position));
        }

        holder.main.setOnLongClickListener(v -> {
            holder.checkBox.setChecked(true);
            selectedNotes.put(position, note.getMilliseconds());
            isSelecting = true;
            notifyDataSetChanged();

            listener.onNoteSelected(true, selectedNotes.size());
            return true;
        });

        holder.main.setOnClickListener(v -> {
            if (isSelecting){
                if (!selectedNotes.containsKey(position)){
                    holder.checkBox.setChecked(true);
                    selectedNotes.put(position, note.getMilliseconds());

                    listener.onNoteSelected(true, selectedNotes.size());
                } else {
                    holder.checkBox.setChecked(false);
                    selectedNotes.remove(position, note.getMilliseconds());

                    listener.onNoteSelected(true, selectedNotes.size());
                }
            } else {
                Intent intent = new Intent(context, NoteActivity.class);
                intent.putExtra("title", note.getTitle());
                intent.putExtra("content", note.getContent());
                intent.putExtra("milliseconds", note.getMilliseconds());
                intent.putExtra("isEditing", true);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                launcher.launch(intent);
            }
        });
    }

    public void stopSelecting(){
        isSelecting = false;
        selectedNotes.clear();
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (int i = 0; i < notes.size(); i++) {
            selectedNotes.put(i, notes.get(i).getMilliseconds());
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
        for (NoteModel notes : notes){
            if (selectedNotes.containsValue(notes.getMilliseconds())){
                realm.beginTransaction();
                notes.deleteFromRealm();
                realm.commitTransaction();
            }
        }
        selectedNotes.clear();
        notifyDataSetChanged();
        listener.onNoteSelected(false, 0);

        isSelecting = false;
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
    }
}
