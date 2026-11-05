package com.example.app_encuestas;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GoogleFormsApi {
    private static final String BASE_URL = "https://forms.googleapis.com/v1/forms";

    private GoogleFormsApi() { }

    public static CreatedForm createSurvey(String accessToken, String title, String description,
                                           List<SurveyQuestion> surveyQuestions)
            throws Exception {
        JSONObject info = new JSONObject();
        info.put("title", title);
        info.put("documentTitle", title);
        JSONObject createBody = new JSONObject().put("info", info);
        JSONObject created = post(BASE_URL + "?unpublished=false", accessToken, createBody);
        String formId = created.optString("formId", "");
        String responderUri = created.optString("responderUri", "");
        if (formId.isEmpty()) throw new Exception("Google no devolvió el identificador del formulario.");

        JSONArray requests = new JSONArray();
        if (description != null && !description.trim().isEmpty()) {
            JSONObject updateInfo = new JSONObject()
                    .put("updateFormInfo", new JSONObject()
                            .put("info", new JSONObject().put("description", description.trim()))
                            .put("updateMask", "description"));
            requests.put(updateInfo);
        }
        for (int i = 0; i < surveyQuestions.size(); i++) {
            SurveyQuestion surveyQuestion = surveyQuestions.get(i);
            JSONObject question = new JSONObject().put("required", true);
            if ("MULTIPLE_CHOICE".equals(surveyQuestion.answerType)) {
                JSONArray options = new JSONArray();
                for (String option : surveyQuestion.choices.split("[,\n]")) {
                    String value = option.trim();
                    if (!value.isEmpty()) options.put(new JSONObject().put("value", value));
                }
                JSONObject choiceQuestion = new JSONObject()
                        .put("type", "RADIO")
                        .put("options", options);
                question.put("choiceQuestion", choiceQuestion);
            } else {
                question.put("textQuestion", new JSONObject().put("paragraph", false));
            }
            JSONObject item = new JSONObject()
                    .put("title", surveyQuestion.title)
                    .put("questionItem", new JSONObject().put("question", question));
            requests.put(new JSONObject()
                    .put("createItem", new JSONObject()
                            .put("item", item)
                            .put("location", new JSONObject().put("index", i))));
        }
        JSONObject updateBody = new JSONObject().put("requests", requests);
        post(BASE_URL + "/" + formId + ":batchUpdate", accessToken, updateBody);
        if (responderUri.isEmpty()) responderUri = "https://docs.google.com/forms/d/" + formId + "/viewform";
        return new CreatedForm(formId, responderUri);
    }

    public static FormSnapshot getForm(String accessToken, String formId) throws Exception {
        JSONObject form = get(BASE_URL + "/" + formId, accessToken);
        JSONObject info = form.optJSONObject("info");
        JSONArray items = form.optJSONArray("items");
        List<FormQuestion> questions = new ArrayList<>();
        if (items != null) {
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                JSONObject questionItem = item.optJSONObject("questionItem");
                JSONObject question = questionItem == null ? null : questionItem.optJSONObject("question");
                if (question == null) continue;

                String answerType = "SHORT";
                String choices = "";
                JSONObject choiceQuestion = question.optJSONObject("choiceQuestion");
                if (choiceQuestion != null) {
                    answerType = "MULTIPLE_CHOICE";
                    JSONArray options = choiceQuestion.optJSONArray("options");
                    if (options != null) {
                        StringBuilder values = new StringBuilder();
                        for (int option = 0; option < options.length(); option++) {
                            JSONObject value = options.optJSONObject(option);
                            if (value == null) continue;
                            if (values.length() > 0) values.append(", ");
                            values.append(value.optString("value", ""));
                        }
                        choices = values.toString();
                    }
                }
                questions.add(new FormQuestion(item.optString("itemId", ""),
                        question.optString("questionId", ""), item.optString("title", ""),
                        answerType, choices));
            }
        }

        JSONObject publishSettings = form.optJSONObject("publishSettings");
        JSONObject publishState = publishSettings == null
                ? null : publishSettings.optJSONObject("publishState");
        boolean published = publishState == null || publishState.optBoolean("isPublished", true);
        boolean acceptingResponses = publishState == null
                || publishState.optBoolean("isAcceptingResponses", true);
        return new FormSnapshot(formId, info == null ? "" : info.optString("title", ""),
                info == null ? "" : info.optString("description", ""), questions,
                published, acceptingResponses, publishState != null);
    }

    public static JSONArray getAllResponses(String accessToken, String formId) throws Exception {
        JSONArray allResponses = new JSONArray();
        String pageToken = null;
        do {
            String endpoint = BASE_URL + "/" + formId + "/responses?pageSize=5000";
            if (pageToken != null && !pageToken.isEmpty()) {
                endpoint += "&pageToken=" + URLEncoder.encode(pageToken, "UTF-8");
            }
            JSONObject page = get(endpoint, accessToken);
            JSONArray responses = page.optJSONArray("responses");
            if (responses != null) {
                for (int i = 0; i < responses.length(); i++) {
                    allResponses.put(responses.opt(i));
                }
            }
            pageToken = page.optString("nextPageToken", "");
        } while (!pageToken.isEmpty());
        return allResponses;
    }

    public static void updateForm(String accessToken, FormSnapshot current, String title,
                                  String description, List<SurveyQuestion> questions,
                                  boolean acceptingResponses) throws Exception {
        if (!current.supportsPublishSettings) {
            throw new Exception("Este formulario antiguo no permite cambiar el estado desde la app.");
        }
        JSONArray requests = new JSONArray();
        requests.put(new JSONObject()
                .put("updateFormInfo", new JSONObject()
                        .put("info", new JSONObject().put("title", title)
                                .put("description", description == null ? "" : description))
                        .put("updateMask", "title,description")));

        int existingCount = current.questions.size();
        int retainedCount = Math.min(existingCount, questions.size());
        for (int i = 0; i < retainedCount; i++) {
            JSONObject item = makeQuestionItem(questions.get(i));
            FormQuestion old = current.questions.get(i);
            if (!old.itemId.isEmpty()) item.put("itemId", old.itemId);
            JSONObject questionItem = item.optJSONObject("questionItem");
            JSONObject question = questionItem.optJSONObject("question");
            if (!old.questionId.isEmpty()) question.put("questionId", old.questionId);
            requests.put(new JSONObject().put("updateItem", new JSONObject()
                    .put("item", item)
                    .put("location", new JSONObject().put("index", i))
                    .put("updateMask", "title,questionItem.question")));
        }
        for (int i = existingCount - 1; i >= questions.size(); i--) {
            requests.put(new JSONObject().put("deleteItem", new JSONObject()
                    .put("location", new JSONObject().put("index", i))));
        }
        for (int i = existingCount; i < questions.size(); i++) {
            requests.put(new JSONObject().put("createItem", new JSONObject()
                    .put("item", makeQuestionItem(questions.get(i)))
                    .put("location", new JSONObject().put("index", i))));
        }
        post(BASE_URL + "/" + current.formId + ":batchUpdate", accessToken,
                new JSONObject().put("requests", requests));

        JSONObject publishSettings = new JSONObject().put("publishState", new JSONObject()
                .put("isPublished", true)
                .put("isAcceptingResponses", acceptingResponses));
        JSONObject publishBody = new JSONObject().put("publishSettings", publishSettings)
                .put("updateMask", "publishState");
        post(BASE_URL + "/" + current.formId + ":setPublishSettings", accessToken, publishBody);
    }

    private static JSONObject makeQuestionItem(SurveyQuestion surveyQuestion) throws Exception {
        JSONObject question = new JSONObject().put("required", true);
        if ("MULTIPLE_CHOICE".equals(surveyQuestion.answerType)) {
            JSONArray options = new JSONArray();
            for (String option : surveyQuestion.choices.split("[,\\n]")) {
                String value = option.trim();
                if (!value.isEmpty()) options.put(new JSONObject().put("value", value));
            }
            question.put("choiceQuestion", new JSONObject().put("type", "RADIO")
                    .put("options", options));
        } else {
            question.put("textQuestion", new JSONObject().put("paragraph", false));
        }
        return new JSONObject().put("title", surveyQuestion.title)
                .put("questionItem", new JSONObject().put("question", question));
    }

    private static JSONObject get(String endpoint, String accessToken) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        int responseCode = connection.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String response = readFully(stream);
        connection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw apiException(response, responseCode);
        }
        return new JSONObject(response);
    }

    private static JSONObject post(String endpoint, String accessToken, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream output = connection.getOutputStream()) {
            output.write(payload);
        }
        int responseCode = connection.getResponseCode();
        InputStream stream = responseCode >= 200 && responseCode < 300
                ? connection.getInputStream() : connection.getErrorStream();
        String response = readFully(stream);
        connection.disconnect();
        if (responseCode < 200 || responseCode >= 300) {
            throw apiException(response, responseCode);
        }
        return response.isEmpty() ? new JSONObject() : new JSONObject(response);
    }

    private static Exception apiException(String response, int responseCode) {
        String message = "No se pudo completar el cambio en Google Forms.";
        try {
            JSONObject error = new JSONObject(response).optJSONObject("error");
            if (error != null) message = error.optString("message", message);
        } catch (Exception ignored) { }
        return new Exception(message + " (HTTP " + responseCode + ")");
    }

    private static String readFully(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    public static final class CreatedForm {
        public final String formId;
        public final String responderUri;

        CreatedForm(String formId, String responderUri) {
            this.formId = formId;
            this.responderUri = responderUri;
        }
    }

    public static final class SurveyQuestion {
        public final String title;
        public final String answerType;
        public final String choices;

        public SurveyQuestion(String title, String answerType, String choices) {
            this.title = title;
            this.answerType = answerType;
            this.choices = choices;
        }
    }

    public static final class FormQuestion {
        public final String itemId;
        public final String questionId;
        public final String title;
        public final String answerType;
        public final String choices;

        public FormQuestion(String itemId, String questionId, String title,
                            String answerType, String choices) {
            this.itemId = itemId;
            this.questionId = questionId;
            this.title = title;
            this.answerType = answerType;
            this.choices = choices;
        }
    }

    public static final class FormSnapshot {
        public final String formId;
        public final String title;
        public final String description;
        public final List<FormQuestion> questions;
        public final boolean isPublished;
        public final boolean isAcceptingResponses;
        public final boolean supportsPublishSettings;

        FormSnapshot(String formId, String title, String description, List<FormQuestion> questions,
                     boolean isPublished, boolean isAcceptingResponses,
                     boolean supportsPublishSettings) {
            this.formId = formId;
            this.title = title;
            this.description = description;
            this.questions = questions;
            this.isPublished = isPublished;
            this.isAcceptingResponses = isAcceptingResponses;
            this.supportsPublishSettings = supportsPublishSettings;
        }
    }
}
