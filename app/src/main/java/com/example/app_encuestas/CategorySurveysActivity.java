package com.example.app_encuestas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class CategorySurveysActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY = "history_category";

    private final List<Survey> filteredSurveys = new ArrayList<>();
    private List<Survey> allSurveys;
    private SurveyAdapter adapter;
    private Spinner statusFilter;
    private TextView emptyState;
    private String category;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_surveys);
        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category == null) category = "Encuestas";

        MaterialToolbar toolbar = findViewById(R.id.categorySurveysToolbar);
        ScreenChrome.setup(this, findViewById(R.id.categorySurveysRoot), toolbar, R.id.nav_history);
        toolbar.setTitle(category);
        MockSurveyData.loadGoogleForms(this);
        allSurveys = MockSurveyData.getSurveys();

        statusFilter = findViewById(R.id.categoryStatusFilter);
        emptyState = findViewById(R.id.categorySurveysEmpty);
        setStatusOptions();
        RecyclerView recycler = findViewById(R.id.categorySurveysRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SurveyAdapter(filteredSurveys, new SurveyAdapter.Actions() {
            @Override public void onOpen(Survey survey) { showSurveyInfo(survey); }
            @Override public void onEdit(Survey survey) { }
            @Override public void onDelete(Survey survey) { }
            @Override public void onExport(Survey survey) {
                List<Survey> oneSurvey = new ArrayList<>();
                oneSurvey.add(survey);
                chooseExportFormat(oneSurvey, "Encuesta - " + survey.getTitle());
            }
        }, false, true);
        recycler.setAdapter(adapter);
        ((MaterialButton) findViewById(R.id.exportCategoryReport)).setOnClickListener(view ->
                chooseExportFormat(getCategorySurveys(), "Histórico - " + category));
        statusFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        applyFilter();
    }

    @Override protected void onResume() {
        super.onResume();
        PaletteTheme.apply(this);
    }

    private void setStatusOptions() {
        String[] values = {"Todos", "Vigente", "Finalizada"};
        ArrayAdapter<String> options = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(CategorySurveysActivity.this));
                return text;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getDropDownView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(CategorySurveysActivity.this));
                text.setBackgroundColor(PaletteTheme.surfaceColor(CategorySurveysActivity.this));
                text.setPadding(dp(16), dp(12), dp(16), dp(12));
                return text;
            }
        };
        options.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilter.setAdapter(options);
    }

    private void applyFilter() {
        if (adapter == null) return;
        String status = (String) statusFilter.getSelectedItem();
        filteredSurveys.clear();
        for (Survey survey : allSurveys) {
            boolean categoryMatches = category.equals(survey.getCategory());
            boolean statusMatches = "Todos".equals(status) || status.equals(survey.getStatus());
            if (categoryMatches && statusMatches) filteredSurveys.add(survey);
        }
        adapter.notifyDataSetChanged();
        emptyState.setVisibility(filteredSurveys.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showSurveyInfo(Survey survey) {
        SurveyDetailsDialog.show(this, survey, true);
    }

    private List<Survey> getCategorySurveys() {
        List<Survey> result = new ArrayList<>();
        for (Survey survey : allSurveys) {
            if (category.equals(survey.getCategory())) result.add(survey);
        }
        return result;
    }

    private void chooseExportFormat(List<Survey> surveys, String reportTitle) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Formato del reporte")
                .setItems(new String[]{"PDF", "Excel (.xlsx)"}, (action, which) -> {
                    Intent intent = new Intent(this, ExportPreviewActivity.class);
                    intent.putExtra(ExportPreviewActivity.EXTRA_TITLE, reportTitle);
                    intent.putExtra(ExportPreviewActivity.EXTRA_FORMAT,
                            which == 0 ? ExportPreviewActivity.FORMAT_PDF
                                    : ExportPreviewActivity.FORMAT_EXCEL);
                    intent.putExtra(ExportPreviewActivity.EXTRA_SURVEYS,
                            SurveyReportExporter.encodeSurveys(surveys));
                    startActivity(intent);
                })
                .create();
        dialog.setOnShowListener(ignored -> PaletteTheme.applyToDialog(dialog, this));
        dialog.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
