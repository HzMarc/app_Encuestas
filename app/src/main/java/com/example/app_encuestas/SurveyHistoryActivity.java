package com.example.app_encuestas;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SurveyHistoryActivity extends AppCompatActivity {
    private static final String[] DEFAULT_CATEGORIES = {
            "Servicios", "Seguridad", "Transporte", "Espacios públicos", "Atención ciudadana"
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_history);
        MaterialToolbar toolbar = findViewById(R.id.historyToolbar);
        ScreenChrome.setup(this, findViewById(R.id.historyRoot), toolbar, R.id.nav_history);
        toolbar.setTitle("Histórico");
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
        MockSurveyData.loadGoogleForms(this);

        RecyclerView categoriesGrid = findViewById(R.id.historyCategories);
        categoriesGrid.setLayoutManager(new GridLayoutManager(this, 2));
        categoriesGrid.setAdapter(new CategoryFolderAdapter(getCategories(), category -> {
            Intent intent = new Intent(this, CategorySurveysActivity.class);
            intent.putExtra(CategorySurveysActivity.EXTRA_CATEGORY, category);
            startActivity(intent);
        }));
    }

    @Override protected void onResume() {
        super.onResume();
        PaletteTheme.apply(this);
    }

    private List<CategoryFolderAdapter.CategoryFolder> getCategories() {
        Set<String> names = new LinkedHashSet<>();
        for (String category : DEFAULT_CATEGORIES) names.add(category);
        for (Survey survey : MockSurveyData.getSurveys()) names.add(survey.getCategory());

        List<CategoryFolderAdapter.CategoryFolder> result = new ArrayList<>();
        for (String name : names) {
            int count = 0;
            for (Survey survey : MockSurveyData.getSurveys()) {
                if (name.equals(survey.getCategory())) count++;
            }
            result.add(new CategoryFolderAdapter.CategoryFolder(name, count));
        }
        return result;
    }
}
