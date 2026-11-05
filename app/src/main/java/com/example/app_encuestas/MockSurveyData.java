package com.example.app_encuestas;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

public final class MockSurveyData {
    private static final List<Survey> SURVEYS = createSurveys();
    private static final List<Survey> GOOGLE_SURVEYS = new ArrayList<>();
    private static final String FORMS_PREFS = "google_forms_created";
    private static final String FORMS_KEY = "forms";
    private static boolean googleFormsLoaded;

    private MockSurveyData() { }

    public static List<Survey> getSurveys() {
        return SURVEYS;
    }

    public static synchronized void loadGoogleForms(Context context) {
        if (googleFormsLoaded) return;
        googleFormsLoaded = true;
        String raw = context.getSharedPreferences(FORMS_PREFS, Context.MODE_PRIVATE)
                .getString(FORMS_KEY, "[]");
        try {
            JSONArray records = new JSONArray(raw);
            for (int i = records.length() - 1; i >= 0; i--) {
                JSONObject item = records.getJSONObject(i);
                String formId = item.optString("formId", "");
                if (formId.isEmpty() || containsGoogleForm(formId)) continue;
                Survey survey = new Survey("GF_" + formId,
                        item.optString("title", "Encuesta"),
                        item.optString("category", "General"),
                        item.optString("date", ""),
                        item.optString("status", "Vigente"), item.optString("responderUri", ""));
                JSONArray savedQuestions = item.optJSONArray("questions");
                if (savedQuestions != null) {
                    List<GoogleFormsApi.SurveyQuestion> questions = new ArrayList<>();
                    for (int q = 0; q < savedQuestions.length(); q++) {
                        JSONObject saved = savedQuestions.optJSONObject(q);
                        if (saved != null) questions.add(new GoogleFormsApi.SurveyQuestion(
                                saved.optString("title", ""), saved.optString("answerType", "SHORT"),
                                saved.optString("choices", "")));
                    }
                    survey.update(survey.getTitle(), survey.getCategory(), survey.getStatus(), questions);
                }
                SURVEYS.add(0, survey);
                GOOGLE_SURVEYS.add(0, survey);
            }
        } catch (Exception ignored) { }
    }

    public static synchronized Survey addGoogleForm(Context context, String formId,
                                                     String title, String category,
                                                     String responderUri) {
        loadGoogleForms(context);
        Survey survey = new Survey("GF_" + formId, title, category,
                new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime()),
                "Vigente", responderUri);
        SURVEYS.add(0, survey);
        GOOGLE_SURVEYS.add(0, survey);
        saveGoogleForms(context);
        return survey;
    }

    public static List<Survey> getGoogleForms() {
        return GOOGLE_SURVEYS;
    }

    public static synchronized void removeSurvey(Context context, Survey survey) {
        SURVEYS.remove(survey);
        GOOGLE_SURVEYS.remove(survey);
        if (survey.isGoogleForm()) saveGoogleForms(context);
    }

    public static synchronized void updateSurvey(Context context, Survey survey, String title,
                                                  String category, String status,
                                                  List<GoogleFormsApi.SurveyQuestion> questions) {
        survey.update(title, category, status, questions);
        if (survey.isGoogleForm()) saveGoogleForms(context);
    }

    private static boolean containsGoogleForm(String formId) {
        for (Survey survey : SURVEYS) {
            if (survey.isGoogleForm() && survey.getGoogleFormId().equals(formId)) return true;
        }
        return false;
    }

    private static void saveGoogleForms(Context context) {
        JSONArray records = new JSONArray();
        for (Survey survey : SURVEYS) {
            if (!survey.isGoogleForm()) continue;
            JSONObject item = new JSONObject();
            try {
                item.put("formId", survey.getGoogleFormId());
                item.put("title", survey.getTitle());
                item.put("category", survey.getCategory());
                item.put("date", survey.getCreationDate());
                item.put("responderUri", survey.getQrCodeString());
                item.put("status", survey.getStatus());
                JSONArray questions = new JSONArray();
                for (GoogleFormsApi.SurveyQuestion question : survey.getQuestions()) {
                    questions.put(new JSONObject()
                            .put("title", question.title)
                            .put("answerType", question.answerType)
                            .put("choices", question.choices));
                }
                item.put("questions", questions);
                records.put(item);
            } catch (Exception ignored) { }
        }
        context.getSharedPreferences(FORMS_PREFS, Context.MODE_PRIVATE).edit()
                .putString(FORMS_KEY, records.toString()).apply();
    }

    public static Survey create(String title, String category, String status) {
        String id = "SURVEY_" + UUID.randomUUID().toString().substring(0, 8);
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
        Survey survey = new Survey(id, title, category, date, status, "https://encuestas.local/" + id);
        SURVEYS.add(0, survey);
        return survey;
    }

    private static List<Survey> createSurveys() {
        String[] titles = {"Servicios municipales", "Seguridad ciudadana", "Transporte público", "Espacios públicos",
                "Atención al usuario", "Limpieza y recolección", "Alumbrado público", "Actividades culturales",
                "Satisfacción ciudadana", "Mercados municipales", "Parques y recreación", "Agua potable",
                "Trámites municipales", "Vialidad urbana", "Participación comunitaria", "Empleo local",
                "Salud comunitaria", "Educación", "Gestión de residuos", "Encuesta de servicio digital"};
        String[] categories = {"Servicios", "Seguridad", "Transporte", "Espacios públicos", "Atención ciudadana"};
        List<Survey> surveys = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < titles.length; i++) {
            calendar.set(Calendar.YEAR, 2022 + (i % 5));
            calendar.set(Calendar.MONTH, i % 12);
            calendar.set(Calendar.DAY_OF_MONTH, 3 + (i * 3) % 25);
            String id = i == titles.length - 1 ? "FORM_REAL" : "SURVEY_" + String.format(Locale.US, "%03d", i + 1);
            String status = i % 3 == 0 ? "Finalizada" : "Vigente";
            String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.getTime());
            surveys.add(new Survey(id, titles[i], categories[i % categories.length], date, status,
                    "https://encuestas.local/" + id));
            Survey survey = surveys.get(surveys.size() - 1);
            survey.update(survey.getTitle(), survey.getCategory(), survey.getStatus(),
                    sampleQuestions(i, survey.getCategory()));
            survey.setGeneratedStatistics(sampleResponseCount(i), sampleResults(i, survey));
        }
        return surveys;
    }

    private static List<GoogleFormsApi.SurveyQuestion> sampleQuestions(int index, String category) {
        List<GoogleFormsApi.SurveyQuestion> questions = new ArrayList<>();
        questions.add(new GoogleFormsApi.SurveyQuestion(
                "¿Cómo califica " + category.toLowerCase(Locale.ROOT) + " en su comunidad?",
                "MULTIPLE_CHOICE", "Muy bien, Bien, Regular, Mal"));
        questions.add(new GoogleFormsApi.SurveyQuestion(
                "¿Con qué frecuencia utiliza este servicio?", "MULTIPLE_CHOICE",
                "Diariamente, Semanalmente, Mensualmente, Casi nunca"));
        questions.add(new GoogleFormsApi.SurveyQuestion(
                "¿Qué cambio considera más necesario?", "SHORT", ""));
        if (index % 2 == 0) {
            questions.add(new GoogleFormsApi.SurveyQuestion(
                    "¿Recomendaría este servicio a otras personas?", "MULTIPLE_CHOICE",
                    "Sí, No, No estoy seguro"));
        }
        return questions;
    }

    private static int sampleResponseCount(int surveyIndex) {
        return 74 + (surveyIndex * 43 % 327);
    }

    private static Map<String, Map<String, Integer>> sampleResults(int surveyIndex, Survey survey) {
        Map<String, Map<String, Integer>> results = new LinkedHashMap<>();
        for (int questionIndex = 0; questionIndex < survey.getQuestions().size(); questionIndex++) {
            GoogleFormsApi.SurveyQuestion question = survey.getQuestions().get(questionIndex);
            List<String> options = new ArrayList<>();
            if ("MULTIPLE_CHOICE".equals(question.answerType)) {
                for (String option : question.choices.split(",")) {
                    if (!option.trim().isEmpty()) options.add(option.trim());
                }
            } else {
                options.add("Mejorar la atención");
                options.add("Ampliar horarios");
                options.add("Más información");
                options.add("Sin cambios");
            }
            int[] weights = new int[options.size()];
            int weightTotal = 0;
            for (int i = 0; i < weights.length; i++) {
                weights[i] = 2 + Math.floorMod(surveyIndex * 7 + questionIndex * 5 + i * 3, 9);
                weightTotal += weights[i];
            }
            Map<String, Integer> distribution = new LinkedHashMap<>();
            int assigned = 0;
            for (int i = 0; i < options.size(); i++) {
                int count = i == options.size() - 1 ? survey.getResponseCount() - assigned
                        : survey.getResponseCount() * weights[i] / weightTotal;
                distribution.put(options.get(i), count);
                assigned += count;
            }
            results.put(question.title, distribution);
        }
        return results;
    }
}
