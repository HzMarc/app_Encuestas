package com.example.app_encuestas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class SurveyAdapter extends RecyclerView.Adapter<SurveyAdapter.Holder> {
    public interface Actions {
        void onOpen(Survey survey);
        void onEdit(Survey survey);
        void onDelete(Survey survey);
        default void onExport(Survey survey) { }
    }

    private final List<Survey> surveys;
    private final Actions actions;
    private final boolean showActions;
    private final boolean showExport;

    public SurveyAdapter(List<Survey> surveys, Actions actions) {
        this(surveys, actions, true, false);
    }

    public SurveyAdapter(List<Survey> surveys, Actions actions, boolean showActions) {
        this(surveys, actions, showActions, false);
    }

    public SurveyAdapter(List<Survey> surveys, Actions actions,
                         boolean showActions, boolean showExport) {
        this.surveys = surveys;
        this.actions = actions;
        this.showActions = showActions;
        this.showExport = showExport;
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_survey, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (holder.itemView.getContext() instanceof android.app.Activity) {
            PaletteTheme.applyToView(holder.itemView,
                    (android.app.Activity) holder.itemView.getContext());
        }
        Survey survey = surveys.get(position);
        holder.title.setText(survey.getTitle());
        String source = survey.isGoogleForm() ? "Google Forms" : survey.getCategory();
        holder.meta.setText(source + "  ·  " + survey.getCreationDate());
        holder.status.setText(survey.getStatus());
        holder.status.setBackgroundResource("Vigente".equals(survey.getStatus())
                ? R.drawable.bg_status_active : R.drawable.bg_status_finished);
        holder.itemView.setOnClickListener(v -> actions.onOpen(survey));
        holder.view.setVisibility(View.VISIBLE);
        holder.view.setOnClickListener(v -> actions.onOpen(survey));
        holder.edit.setVisibility(showActions ? View.VISIBLE : View.GONE);
        holder.delete.setVisibility(showActions ? View.VISIBLE : View.GONE);
        holder.export.setVisibility(showExport ? View.VISIBLE : View.GONE);
        if (showActions) {
            holder.edit.setOnClickListener(v -> actions.onEdit(survey));
            holder.delete.setOnClickListener(v -> actions.onDelete(survey));
        }
        holder.export.setOnClickListener(v -> actions.onExport(survey));
    }

    @Override public int getItemCount() { return surveys.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView title, meta, status;
        final MaterialButton view, edit, delete, export;
        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.surveyTitle);
            meta = itemView.findViewById(R.id.surveyMeta);
            status = itemView.findViewById(R.id.surveyStatus);
            view = itemView.findViewById(R.id.viewSurvey);
            edit = itemView.findViewById(R.id.editSurvey);
            delete = itemView.findViewById(R.id.deleteSurvey);
            export = itemView.findViewById(R.id.exportSurvey);
        }
    }
}
