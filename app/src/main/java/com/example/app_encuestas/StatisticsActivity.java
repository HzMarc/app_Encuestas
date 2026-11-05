package com.example.app_encuestas;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.appbar.MaterialToolbar;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StatisticsActivity extends AppCompatActivity {
    private static final int REQUEST_FORMS_AUTHORIZATION = 6104;
    private static final String DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    private static final ExecutorService STATS_EXECUTOR = Executors.newSingleThreadExecutor();

    private final List<Survey> googleSurveys = new ArrayList<>();
    private Spinner categoryFilter;
    private Spinner surveyFilter;
    private Spinner questionFilter;
    private TextView questionLabel;
    private View questionContainer;
    private TextView summaryTitle;
    private TextView summary;
    private BarChart chart;
    private LineChart trendChart;
    private PieChart pieChart;
    private View exportButton;
    private String accessToken;
    private GoogleFormsApi.FormSnapshot cachedForm;
    private JSONArray cachedResponses;
    private int requestSequence;
    private boolean ignoreSurveySelection;
    private boolean ignoreQuestionSelection;
    private final List<String> reportLabels = new ArrayList<>();
    private final List<Integer> reportValues = new ArrayList<>();
    private final Map<String, Integer> reportDates = new LinkedHashMap<>();
    private String reportLegend = "Respuestas";

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        MockSurveyData.loadGoogleForms(this);
        googleSurveys.addAll(MockSurveyData.getGoogleForms());

        MaterialToolbar toolbar = findViewById(R.id.statisticsToolbar);
        ScreenChrome.setup(this, findViewById(R.id.statisticsRoot), toolbar, R.id.nav_statistics);
        toolbar.setTitle("Estadísticas");
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);

        categoryFilter = findViewById(R.id.statisticsCategoryFilter);
        surveyFilter = findViewById(R.id.statisticsSurveyFilter);
        questionFilter = findViewById(R.id.statisticsQuestionFilter);
        questionLabel = findViewById(R.id.statisticsQuestionLabel);
        questionContainer = findViewById(R.id.statisticsQuestionContainer);
        summaryTitle = findViewById(R.id.statisticsSummaryTitle);
        summary = findViewById(R.id.statisticsSummary);
        chart = findViewById(R.id.statisticsChart);
        trendChart = findViewById(R.id.statisticsTrendChart);
        pieChart = findViewById(R.id.statisticsPieChart);
        exportButton = findViewById(R.id.exportStatisticsButton);
        exportButton.setEnabled(false);
        exportButton.setOnClickListener(view -> chooseExportFormat());
        configureChart();
        configureFilters();

        if (googleSurveys.isEmpty()) {
            summary.setText("Todavía no hay formularios de Google Forms creados desde esta app.");
            chart.setNoDataText("Crea una encuesta en Google Forms para ver sus respuestas.");
            chart.invalidate();
            drawTrendChart(new TreeMap<>());
            drawPieChart(Collections.emptyList(), Collections.emptyList());
        } else {
            authorize();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        PaletteTheme.apply(this);
    }

    private void configureFilters() {
        List<String> categories = new ArrayList<>();
        categories.add("Todas las categorías");
        for (Survey survey : googleSurveys) {
            if (!categories.contains(survey.getCategory())) categories.add(survey.getCategory());
        }
        setSpinnerItems(categoryFilter, categories);
        updateSurveyOptions();
        setSpinnerItems(questionFilter, Collections.singletonList("Resumen de preguntas"));

        categoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSurveyOptions();
                refreshChart();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        surveyFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (ignoreSurveySelection) return;
                boolean oneSurveySelected = position > 0;
                questionContainer.setVisibility(oneSurveySelected ? View.VISIBLE : View.GONE);
                questionLabel.setVisibility(oneSurveySelected ? View.VISIBLE : View.GONE);
                questionFilter.setVisibility(oneSurveySelected ? View.VISIBLE : View.GONE);
                if (oneSurveySelected) setQuestionOptions(Collections.singletonList("Resumen de preguntas"));
                cachedForm = null;
                cachedResponses = null;
                refreshChart();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        questionFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!ignoreQuestionSelection) refreshChart();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void updateSurveyOptions() {
        String selectedCategory = (String) categoryFilter.getSelectedItem();
        List<String> labels = new ArrayList<>();
        labels.add("Todas las encuestas");
        for (Survey survey : googleSurveys) {
            if ("Todas las categorías".equals(selectedCategory)
                    || selectedCategory.equals(survey.getCategory())) {
                labels.add(survey.getTitle());
            }
        }
        ignoreSurveySelection = true;
        setSpinnerItems(surveyFilter, labels);
        surveyFilter.setSelection(0);
        ignoreSurveySelection = false;
        questionLabel.setVisibility(View.GONE);
        questionContainer.setVisibility(View.GONE);
        questionFilter.setVisibility(View.GONE);
    }

    private void setQuestionOptions(List<String> labels) {
        ignoreQuestionSelection = true;
        setSpinnerItems(questionFilter, labels);
        questionFilter.setSelection(0);
        ignoreQuestionSelection = false;
    }

    private void setSpinnerItems(Spinner spinner, List<String> values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(StatisticsActivity.this));
                return text;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getDropDownView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(StatisticsActivity.this));
                text.setBackgroundColor(PaletteTheme.surfaceColor(StatisticsActivity.this));
                text.setPadding(dp(14), dp(12), dp(14), dp(12));
                return text;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void authorize() {
        summaryTitle.setText("Conectando con Google");
        summary.setText("Autoriza el acceso para cargar respuestas, fechas y distribuciones.");
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DRIVE_FILE_SCOPE)))
                .build();
        Identity.getAuthorizationClient(this).authorize(request)
                .addOnSuccessListener(this, this::handleAuthorization)
                .addOnFailureListener(this, error -> showAuthorizationError(error.getLocalizedMessage()));
    }

    private void handleAuthorization(AuthorizationResult result) {
        if (result.hasResolution()) {
            try {
                startIntentSenderForResult(result.getPendingIntent().getIntentSender(),
                        REQUEST_FORMS_AUTHORIZATION, null, 0, 0, 0);
            } catch (Exception error) {
                showAuthorizationError("No se pudo abrir el permiso de Google.");
            }
            return;
        }
        accessToken = result.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            showAuthorizationError("Google no entregó permiso para leer las respuestas.");
            return;
        }
        refreshChart();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FORMS_AUTHORIZATION) return;
        if (resultCode != RESULT_OK || data == null) {
            showAuthorizationError("No se autorizó el acceso a Google Forms.");
            return;
        }
        try {
            handleAuthorization(Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(data));
        } catch (ApiException error) {
            showAuthorizationError("No se pudo validar el permiso de Google.");
        }
    }

    private void refreshChart() {
        if (accessToken == null || accessToken.isEmpty()) return;
        exportButton.setEnabled(false);
        int requestId = ++requestSequence;
        String category = (String) categoryFilter.getSelectedItem();
        int surveyPosition = surveyFilter.getSelectedItemPosition();
        summaryTitle.setText("Cargando resultados…");
        summary.setText("Consultando las respuestas de Google Forms.");

        if (surveyPosition <= 0) {
            List<Survey> targets = getFilteredSurveys(category);
            STATS_EXECUTOR.execute(() -> loadCategoryTotals(requestId, category, targets));
            return;
        }

        Survey selectedSurvey = getFilteredSurveys(category).get(surveyPosition - 1);
        int questionPosition = questionFilter.getSelectedItemPosition();
        if (cachedForm != null && cachedForm.formId.equals(selectedSurvey.getGoogleFormId())
                && cachedResponses != null) {
            showSurveyQuestionStats(requestId, selectedSurvey, cachedForm, cachedResponses,
                    questionPosition);
        } else {
            STATS_EXECUTOR.execute(() -> loadSurveyStats(requestId, selectedSurvey));
        }
    }

    private void loadCategoryTotals(int requestId, String category, List<Survey> targets) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        Map<String, Integer> dailyTotals = new TreeMap<>();
        int responseTotal = 0;
        int failures = 0;
        for (Survey survey : targets) {
            try {
                JSONArray responses = GoogleFormsApi.getAllResponses(accessToken,
                        survey.getGoogleFormId());
                String key = "Todas las categorías".equals(category)
                        ? survey.getCategory() : survey.getTitle();
                totals.put(key, totals.containsKey(key) ? totals.get(key) + responses.length()
                        : responses.length());
                responseTotal += responses.length();
                addResponseDates(dailyTotals, responses);
            } catch (Exception error) {
                failures++;
            }
        }

        List<String> labels = new ArrayList<>(totals.keySet());
        List<Integer> values = new ArrayList<>();
        for (String label : labels) values.add(totals.get(label));
        final int total = responseTotal;
        final int failed = failures;
        runOnUiThread(() -> {
            if (requestId != requestSequence) return;
            String grouping = "Todas las categorías".equals(category) ? "por categoría" : "por encuesta";
            summaryTitle.setText(total + (total == 1 ? " respuesta" : " respuestas"));
            String text = "Datos agrupados " + grouping + ".";
            if (failed > 0) text += " " + failed + " encuesta(s) no se pudieron leer.";
            summary.setText(text);
            drawChart(labels, values, "Respuestas");
            drawPieChart(labels, values);
            drawTrendChart(dailyTotals);
            exportButton.setEnabled(true);
        });
    }

    private void loadSurveyStats(int requestId, Survey survey) {
        try {
            GoogleFormsApi.FormSnapshot form = GoogleFormsApi.getForm(accessToken,
                    survey.getGoogleFormId());
            JSONArray responses = GoogleFormsApi.getAllResponses(accessToken,
                    survey.getGoogleFormId());
            runOnUiThread(() -> {
                if (requestId != requestSequence) return;
                cachedForm = form;
                cachedResponses = responses;
                List<String> questions = new ArrayList<>();
                questions.add("Resumen de preguntas");
                for (GoogleFormsApi.FormQuestion question : form.questions) {
                    questions.add(question.title);
                }
                setQuestionOptions(questions);
                summaryTitle.setText(responses.length()
                        + (responses.length() == 1 ? " respuesta" : " respuestas"));
                summary.setText("Resultados de “" + survey.getTitle() + "”.");
                showSurveyQuestionStats(requestId, survey, form, responses, 0);
            });
        } catch (Exception error) {
            runOnUiThread(() -> {
                if (requestId != requestSequence) return;
                exportButton.setEnabled(false);
                summaryTitle.setText("No se pudieron cargar los resultados");
                summary.setText(error.getLocalizedMessage() == null
                        ? "Revisa el permiso de Google Forms y vuelve a intentarlo."
                        : error.getLocalizedMessage());
                drawChart(Collections.emptyList(), Collections.emptyList(), "Respuestas");
                drawPieChart(Collections.emptyList(), Collections.emptyList());
                drawTrendChart(new TreeMap<>());
            });
        }
    }

    private void showSurveyQuestionStats(int requestId, Survey survey,
                                         GoogleFormsApi.FormSnapshot form,
                                         JSONArray responses, int questionPosition) {
        if (requestId != requestSequence) return;
        if (form.questions.isEmpty()) {
            summaryTitle.setText(responses.length() + " respuestas");
            summary.setText("El formulario no tiene preguntas compatibles para graficar.");
            drawChart(Collections.emptyList(), Collections.emptyList(), "Respuestas");
            drawPieChart(Collections.emptyList(), Collections.emptyList());
            drawTrendChart(responseTrend(responses));
            exportButton.setEnabled(true);
            return;
        }
        if (questionPosition <= 0) {
            List<String> labels = new ArrayList<>();
            List<Integer> values = new ArrayList<>();
            for (GoogleFormsApi.FormQuestion question : form.questions) {
                labels.add(shortLabel(question.title));
                values.add(countAnswered(responses, question.questionId));
            }
            summary.setText("Barras y segmentos comparan cuántas personas respondieron cada pregunta.");
            drawChart(labels, values, "Respuestas por pregunta");
            drawPieChart(labels, values);
        } else {
            GoogleFormsApi.FormQuestion question = form.questions.get(questionPosition - 1);
            Map<String, Integer> distribution = getAnswerDistribution(question, responses);
            List<String> labels = new ArrayList<>(distribution.keySet());
            List<Integer> values = new ArrayList<>();
            for (String label : labels) values.add(distribution.get(label));
            summary.setText("Opciones más seleccionadas para: " + question.title);
            drawChart(labels, values, "Respuestas");
            drawPieChart(labels, values);
        }
        drawTrendChart(responseTrend(responses));
        exportButton.setEnabled(true);
    }

    private int countAnswered(JSONArray responses, String questionId) {
        int count = 0;
        for (int i = 0; i < responses.length(); i++) {
            JSONObject response = responses.optJSONObject(i);
            JSONObject answers = response == null ? null : response.optJSONObject("answers");
            if (answers != null && answers.has(questionId)) count++;
        }
        return count;
    }

    private Map<String, Integer> getAnswerDistribution(GoogleFormsApi.FormQuestion question,
                                                       JSONArray responses) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if ("MULTIPLE_CHOICE".equals(question.answerType)) {
            for (String choice : question.choices.split(",")) {
                String trimmed = choice.trim();
                if (!trimmed.isEmpty()) counts.put(trimmed, 0);
            }
        }
        for (int i = 0; i < responses.length(); i++) {
            JSONObject response = responses.optJSONObject(i);
            JSONObject answers = response == null ? null : response.optJSONObject("answers");
            JSONObject answer = answers == null ? null : answers.optJSONObject(question.questionId);
            if (answer == null) continue;
            JSONObject answerContainer = answer.optJSONObject("choiceAnswers");
            String answerArrayName = "answers";
            if (answerContainer == null) answerContainer = answer.optJSONObject("textAnswers");
            JSONArray values = answerContainer == null ? null : answerContainer.optJSONArray(answerArrayName);
            if (values == null) continue;
            for (int j = 0; j < values.length(); j++) {
                JSONObject value = values.optJSONObject(j);
                String text = value == null ? "" : value.optString("value", "").trim();
                if (text.isEmpty()) continue;
                counts.put(text, counts.containsKey(text) ? counts.get(text) + 1 : 1);
            }
        }
        if (!"MULTIPLE_CHOICE".equals(question.answerType) && counts.size() > 5) {
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
            entries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
            counts.clear();
            for (int i = 0; i < Math.min(5, entries.size()); i++) {
                counts.put(entries.get(i).getKey(), entries.get(i).getValue());
            }
        }
        return counts;
    }

    private List<Survey> getFilteredSurveys(String category) {
        List<Survey> result = new ArrayList<>();
        for (Survey survey : googleSurveys) {
            if ("Todas las categorías".equals(category) || category.equals(survey.getCategory())) {
                result.add(survey);
            }
        }
        return result;
    }

    private void configureChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setLabelRotationAngle(-28f);
        chart.setFitBars(true);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setNoDataText("No hay respuestas para mostrar.");

        trendChart.getDescription().setEnabled(false);
        trendChart.getLegend().setEnabled(false);
        trendChart.getAxisRight().setEnabled(false);
        trendChart.getAxisLeft().setAxisMinimum(0f);
        trendChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        trendChart.getXAxis().setGranularity(1f);
        trendChart.getXAxis().setLabelRotationAngle(-25f);
        trendChart.setNoDataText("Google Forms no devolvió fechas de respuesta.");
        trendChart.setTouchEnabled(true);
        trendChart.setDragEnabled(true);
        trendChart.setScaleEnabled(false);

        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(false);
        pieChart.setHoleRadius(58f);
        pieChart.setTransparentCircleRadius(63f);
        pieChart.setCenterText("Respuestas");
        pieChart.setNoDataText("No hay respuestas para distribuir.");
        pieChart.getLegend().setWordWrapEnabled(true);
        pieChart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        pieChart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
    }

    private void drawChart(List<String> labels, List<Integer> values, String legend) {
        reportLabels.clear();
        reportLabels.addAll(labels);
        reportValues.clear();
        reportValues.addAll(values);
        reportLegend = legend;
        if (labels.isEmpty()) {
            chart.clear();
            chart.setNoDataText("No hay respuestas para mostrar.");
            chart.invalidate();
            return;
        }
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) entries.add(new BarEntry(i, values.get(i)));
        String[] colorPalette = PaletteManager.getActivePalette(this);
        int color = PaletteManager.color(colorPalette[3], Color.rgb(87, 164, 219));
        BarDataSet set = new BarDataSet(entries, legend);
        set.setColor(color);
        set.setDrawValues(true);
        chart.setData(new BarData(set));
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setLabelCount(labels.size());
        chart.getAxisLeft().setGranularity(1f);
        chart.setNoDataText("No hay respuestas para mostrar.");
        chart.invalidate();
    }

    private void drawPieChart(List<String> labels, List<Integer> values) {
        List<PieEntry> entries = new ArrayList<>();
        for (int i = 0; i < Math.min(labels.size(), values.size()); i++) {
            if (values.get(i) > 0) entries.add(new PieEntry(values.get(i), labels.get(i)));
        }
        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("No hay respuestas suficientes para mostrar la distribución.");
            pieChart.invalidate();
            return;
        }
        PieDataSet set = new PieDataSet(entries, "Respuestas");
        set.setColors(activeChartColors(entries.size()));
        set.setSliceSpace(3f);
        set.setSelectionShift(5f);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);
        set.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        pieChart.setData(new PieData(set));
        pieChart.setCenterText(entries.size() + (entries.size() == 1 ? " opción" : " opciones"));
        pieChart.highlightValues(null);
        pieChart.invalidate();
    }

    private void drawTrendChart(Map<String, Integer> dailyTotals) {
        reportDates.clear();
        reportDates.putAll(dailyTotals);
        if (dailyTotals.isEmpty()) {
            trendChart.clear();
            trendChart.setNoDataText("Google Forms no devolvió fechas de respuesta.");
            trendChart.invalidate();
            return;
        }
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat labelFormatter = new SimpleDateFormat("dd/MM", Locale.getDefault());
        int index = 0;
        for (Map.Entry<String, Integer> day : dailyTotals.entrySet()) {
            entries.add(new Entry(index++, day.getValue()));
            try {
                Date date = parser.parse(day.getKey());
                labels.add(date == null ? day.getKey() : labelFormatter.format(date));
            } catch (ParseException error) {
                labels.add(day.getKey());
            }
        }
        LineDataSet set = new LineDataSet(entries, "Respuestas por día");
        set.setColor(PaletteTheme.accentColor(this));
        set.setCircleColor(PaletteTheme.accentColor(this));
        set.setCircleRadius(4f);
        set.setLineWidth(2.5f);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawValues(false);
        set.setDrawFilled(true);
        set.setFillColor(PaletteTheme.accentColor(this));
        set.setFillAlpha(35);
        set.setValueTextColor(PaletteTheme.textColor(this));
        trendChart.setData(new LineData(set));
        trendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        trendChart.getXAxis().setLabelCount(Math.min(labels.size(), 6), false);
        trendChart.invalidate();
    }

    private int[] activeChartColors(int count) {
        int accent = PaletteTheme.accentColor(this);
        int principal = PaletteTheme.principalColor(this);
        int text = PaletteTheme.textColor(this);
        int[] available = {accent, principal, text, Color.rgb(46, 125, 50),
                Color.rgb(245, 124, 0), Color.rgb(0, 121, 107), Color.rgb(123, 31, 162)};
        int[] result = new int[Math.max(count, 1)];
        for (int i = 0; i < result.length; i++) result[i] = available[i % available.length];
        return result;
    }

    private Map<String, Integer> responseTrend(JSONArray responses) {
        Map<String, Integer> totals = new TreeMap<>();
        addResponseDates(totals, responses);
        return totals;
    }

    private void addResponseDates(Map<String, Integer> totals, JSONArray responses) {
        SimpleDateFormat dayFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat[] formats = {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US),
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US)
        };
        for (int i = 0; i < responses.length(); i++) {
            JSONObject response = responses.optJSONObject(i);
            String submitted = response == null ? "" : response.optString("lastSubmittedTime", "");
            Date date = null;
            for (SimpleDateFormat format : formats) {
                try {
                    date = format.parse(submitted);
                    if (date != null) break;
                } catch (ParseException ignored) { }
            }
            if (date == null) continue;
            String key = dayFormatter.format(date);
            totals.put(key, totals.containsKey(key) ? totals.get(key) + 1 : 1);
        }
    }

    private String shortLabel(String label) {
        if (label == null) return "";
        return label.length() > 18 ? label.substring(0, 17) + "…" : label;
    }

    private void chooseExportFormat() {
        StatisticsReportExporter.Report report = new StatisticsReportExporter.Report();
        Object category = categoryFilter.getSelectedItem();
        Object survey = surveyFilter.getSelectedItem();
        Object question = questionFilter.getSelectedItem();
        report.category = category == null ? "Todas las categorías" : category.toString();
        report.survey = survey == null ? "Todas las encuestas" : survey.toString();
        report.question = question == null ? "Resumen de preguntas" : question.toString();
        report.summary = summaryTitle.getText() + ". " + summary.getText();
        try {
            String digits = summaryTitle.getText().toString().replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) report.totalResponses = Integer.parseInt(digits);
        } catch (NumberFormatException ignored) { }
        report.legend = reportLegend;
        report.labels.addAll(reportLabels);
        report.values.addAll(reportValues);
        report.dates.putAll(reportDates);
        String reportTitle = "Estadísticas - " + report.category;

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Descargar estadísticas")
                .setItems(new String[]{"PDF", "Excel (.xlsx)"}, (action, which) -> {
                    Intent intent = new Intent(this, ExportPreviewActivity.class);
                    intent.putExtra(ExportPreviewActivity.EXTRA_TITLE, reportTitle);
                    intent.putExtra(ExportPreviewActivity.EXTRA_FORMAT,
                            which == 0 ? ExportPreviewActivity.FORMAT_PDF
                                    : ExportPreviewActivity.FORMAT_EXCEL);
                    intent.putExtra(ExportPreviewActivity.EXTRA_STATISTICS,
                            StatisticsReportExporter.encode(report));
                    startActivity(intent);
                })
                .setNegativeButton("Cancelar", null)
                .create();
        dialog.setOnShowListener(ignored -> PaletteTheme.applyToDialog(dialog, this));
        dialog.show();
    }

    private void showAuthorizationError(String detail) {
        String message = detail == null || detail.trim().isEmpty()
                ? "No se pudo conectar con Google Forms."
                : "No se pudieron leer las respuestas. " + detail;
        summaryTitle.setText("No se pudieron cargar las estadísticas");
        summary.setText(message);
        drawChart(Collections.emptyList(), Collections.emptyList(), "Respuestas");
        drawPieChart(Collections.emptyList(), Collections.emptyList());
        drawTrendChart(new TreeMap<>());
        exportButton.setEnabled(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
