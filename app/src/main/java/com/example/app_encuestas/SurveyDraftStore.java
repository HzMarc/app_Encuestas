package com.example.app_encuestas;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SurveyDraftStore {
    private static final String PREFS = "survey_drafts_local";
    private static final String KEY_DRAFTS = "drafts";

    private SurveyDraftStore() { }

    public static synchronized List<SurveyDraft> getDrafts(Context context) {
        JSONArray records = readRecords(context);
        List<SurveyDraft> result = new ArrayList<>();
        for (int i = records.length() - 1; i >= 0; i--) {
            JSONObject item = records.optJSONObject(i);
            if (item == null) continue;
            List<GoogleFormsApi.SurveyQuestion> questions = new ArrayList<>();
            JSONArray savedQuestions = item.optJSONArray("questions");
            if (savedQuestions != null) {
                for (int q = 0; q < savedQuestions.length(); q++) {
                    JSONObject saved = savedQuestions.optJSONObject(q);
                    if (saved != null) questions.add(new GoogleFormsApi.SurveyQuestion(
                            saved.optString("title", ""), saved.optString("answerType", "SHORT"),
                            saved.optString("choices", "")));
                }
            }
            result.add(new SurveyDraft(item.optString("id", ""), item.optString("title", ""),
                    item.optString("description", ""), item.optString("category", "Servicios"),
                    item.optLong("updatedAt", 0), questions));
        }
        return result;
    }

    public static synchronized SurveyDraft getDraft(Context context, String id) {
        if (id == null) return null;
        for (SurveyDraft draft : getDrafts(context)) if (id.equals(draft.id)) return draft;
        return null;
    }

    public static synchronized void save(Context context, SurveyDraft draft) {
        List<SurveyDraft> drafts = getDrafts(context);
        drafts.removeIf(existing -> existing.id.equals(draft.id));
        drafts.add(draft);
        writeRecords(context, drafts);
    }

    public static synchronized void delete(Context context, String id) {
        List<SurveyDraft> drafts = getDrafts(context);
        drafts.removeIf(existing -> existing.id.equals(id));
        writeRecords(context, drafts);
    }

    private static JSONArray readRecords(Context context) {
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_DRAFTS, "[]");
        try { return new JSONArray(raw); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private static void writeRecords(Context context, List<SurveyDraft> drafts) {
        JSONArray records = new JSONArray();
        for (SurveyDraft draft : drafts) {
            try {
                JSONObject item = new JSONObject()
                        .put("id", draft.id)
                        .put("title", draft.title)
                        .put("description", draft.description)
                        .put("category", draft.category)
                        .put("updatedAt", draft.updatedAt);
                JSONArray questions = new JSONArray();
                for (GoogleFormsApi.SurveyQuestion question : draft.questions) {
                    questions.put(new JSONObject().put("title", question.title)
                            .put("answerType", question.answerType).put("choices", question.choices));
                }
                item.put("questions", questions);
                records.put(item);
            } catch (Exception ignored) { }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_DRAFTS, records.toString()).apply();
    }
}
