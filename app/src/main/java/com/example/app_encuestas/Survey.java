package com.example.app_encuestas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Survey {
    private final String id;
    private String title;
    private String category;
    private final String creationDate;
    private String status;
    private final String qrCodeString;
    private List<GoogleFormsApi.SurveyQuestion> questions = new ArrayList<>();
    private int responseCount;
    private final Map<String, Map<String, Integer>> questionResults = new LinkedHashMap<>();

    public Survey(String id, String title, String category, String creationDate,
                  String status, String qrCodeString) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.creationDate = creationDate;
        this.status = status;
        this.qrCodeString = qrCodeString;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getCreationDate() { return creationDate; }
    public String getStatus() { return status; }
    public String getQrCodeString() { return qrCodeString; }
    public List<GoogleFormsApi.SurveyQuestion> getQuestions() { return questions; }
    public boolean isGoogleForm() { return id.startsWith("GF_"); }
    public String getGoogleFormId() { return isGoogleForm() ? id.substring(3) : ""; }
    public int getResponseCount() { return responseCount; }
    public Map<String, Integer> getQuestionResults(String questionTitle) {
        Map<String, Integer> values = questionResults.get(questionTitle);
        return values == null ? new LinkedHashMap<>() : new LinkedHashMap<>(values);
    }

    public Map<String, Map<String, Integer>> getAllQuestionResults() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> entry : questionResults.entrySet()) {
            copy.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return copy;
    }

    public void setGeneratedStatistics(int responseCount,
                                       Map<String, Map<String, Integer>> questionResults) {
        this.responseCount = Math.max(0, responseCount);
        this.questionResults.clear();
        for (Map.Entry<String, Map<String, Integer>> entry : questionResults.entrySet()) {
            this.questionResults.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
    }

    public void update(String title, String category, String status,
                       List<GoogleFormsApi.SurveyQuestion> questions) {
        this.title = title;
        this.category = category;
        this.status = status;
        this.questions = new ArrayList<>(questions);
    }
}
