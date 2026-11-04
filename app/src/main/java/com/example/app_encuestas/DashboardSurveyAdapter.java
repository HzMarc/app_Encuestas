package com.example.app_encuestas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DashboardSurveyAdapter extends RecyclerView.Adapter<DashboardSurveyAdapter.SurveyViewHolder> {
    private final List<DashboardSurvey> surveys;

    public DashboardSurveyAdapter(List<DashboardSurvey> surveys) {
        this.surveys = surveys;
    }

    @NonNull
    @Override
    public SurveyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_survey, parent, false);
        return new SurveyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SurveyViewHolder holder, int position) {
        DashboardSurvey survey = surveys.get(position);
        holder.title.setText(survey.getTitle());
        holder.subtitle.setText(survey.getSubtitle());
    }

    @Override
    public int getItemCount() {
        return surveys.size();
    }

    static class SurveyViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;

        SurveyViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvSurveyTitle);
            subtitle = itemView.findViewById(R.id.tvSurveySubtitle);
        }
    }
}
