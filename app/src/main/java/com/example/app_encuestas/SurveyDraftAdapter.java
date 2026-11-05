package com.example.app_encuestas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public final class SurveyDraftAdapter extends RecyclerView.Adapter<SurveyAdapter.Holder> {
    public interface Actions {
        void onEdit(SurveyDraft draft);
        void onDelete(SurveyDraft draft);
        void onUpload(SurveyDraft draft);
    }

    private final List<SurveyDraft> drafts;
    private final Actions actions;

    public SurveyDraftAdapter(List<SurveyDraft> drafts, Actions actions) {
        this.drafts = drafts;
        this.actions = actions;
    }

    @NonNull @Override public SurveyAdapter.Holder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {
        return new SurveyAdapter.Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_survey, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull SurveyAdapter.Holder holder, int position) {
        SurveyDraft draft = drafts.get(position);
        holder.title.setText(draft.title);
        String date = draft.updatedAt <= 0 ? "" : DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(new Date(draft.updatedAt));
        holder.meta.setText("Borrador local · " + draft.category + (date.isEmpty() ? "" : " · " + date));
        holder.status.setText("Borrador");
        holder.status.setBackgroundResource(R.drawable.bg_status_finished);
        holder.view.setText("Subir");
        holder.view.setIconResource(R.drawable.ic_export);
        holder.view.setOnClickListener(view -> actions.onUpload(draft));
        holder.edit.setOnClickListener(view -> actions.onEdit(draft));
        holder.delete.setOnClickListener(view -> actions.onDelete(draft));
        holder.export.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(view -> actions.onEdit(draft));
        if (holder.itemView.getContext() instanceof android.app.Activity) {
            PaletteTheme.applyToView(holder.itemView, (android.app.Activity) holder.itemView.getContext());
        }
    }

    @Override public int getItemCount() { return drafts.size(); }
}
