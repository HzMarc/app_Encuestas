package com.example.app_encuestas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditSurveyActivity extends AppCompatActivity {
    public static final String EXTRA_SURVEY_ID = "edit_survey_id";
    public static final String EXTRA_TITLE = "edit_survey_title";
    public static final String EXTRA_CATEGORY = "edit_survey_category";
    public static final String EXTRA_STATUS = "edit_survey_status";
    public static final String EXTRA_QUESTIONS = "edit_survey_questions";

    private static final int REQUEST_FORMS_AUTHORIZATION = 6103;
    private static final String DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    private static final ExecutorService FORMS_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final String[] CATEGORIES = {
            "Servicios", "Seguridad", "Transporte", "Espacios públicos", "Atención ciudadana"
    };
    private static final String[] STATUS_OPTIONS = {
            "Abierta · acepta respuestas", "Cerrada · no acepta respuestas"
    };

    private String surveyId;
    private String accessToken;
    private String description = "";
    private GoogleFormsApi.FormSnapshot formSnapshot;
    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private Spinner categorySpinner;
    private Spinner statusSpinner;
    private TextView questionCount;
    private LinearLayout questionsContainer;
    private MaterialButton saveButton;
    private final List<QuestionRow> questionRows = new ArrayList<>();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_edit_survey);
        PaletteTheme.apply(this);

        View root = findViewById(R.id.editSurveyRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top,
                    view.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        surveyId = getIntent().getStringExtra(EXTRA_SURVEY_ID);
        titleInput = findViewById(R.id.editSurveyTitle);
        descriptionInput = findViewById(R.id.editSurveyDescription);
        categorySpinner = findViewById(R.id.editSurveyCategory);
        statusSpinner = findViewById(R.id.editSurveyStatus);
        questionCount = findViewById(R.id.editQuestionCount);
        questionsContainer = findViewById(R.id.editQuestionsContainer);
        saveButton = findViewById(R.id.saveSurveyChanges);

        ((MaterialToolbar) findViewById(R.id.editSurveyToolbar))
                .setNavigationOnClickListener(view -> finish());
        setSpinnerItems(categorySpinner, CATEGORIES);
        setSpinnerItems(statusSpinner, STATUS_OPTIONS);
        titleInput.setTextColor(getColor(R.color.black));
        titleInput.setHintTextColor(getColor(R.color.text_gray));
        descriptionInput.setTextColor(getColor(R.color.black));
        descriptionInput.setHintTextColor(getColor(R.color.text_gray));

        titleInput.setText(getIntent().getStringExtra(EXTRA_TITLE));
        selectSpinner(categorySpinner, getIntent().getStringExtra(EXTRA_CATEGORY));
        statusSpinner.setSelection("Finalizada".equals(
                getIntent().getStringExtra(EXTRA_STATUS)) ? 1 : 0);
        List<GoogleFormsApi.SurveyQuestion> initialQuestions = decodeQuestions(
                getIntent().getStringExtra(EXTRA_QUESTIONS));
        for (GoogleFormsApi.SurveyQuestion question : initialQuestions) addQuestionRow(question);
        if (questionRows.isEmpty()) addQuestionRow(null);

        findViewById(R.id.editAddQuestion).setOnClickListener(view -> addQuestionRow(null));
        saveButton.setOnClickListener(view -> saveChanges());

        if (surveyId != null && surveyId.startsWith("GF_")) {
            authorizeAndLoadForm();
        }
    }

    private void authorizeAndLoadForm() {
        setBusy(true, "Conectando con Google…");
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DRIVE_FILE_SCOPE)))
                .build();
        Identity.getAuthorizationClient(this).authorize(request)
                .addOnSuccessListener(this, this::handleAuthorizationResult)
                .addOnFailureListener(this, error -> showError(authErrorMessage(error)));
    }

    private void handleAuthorizationResult(AuthorizationResult result) {
        if (result.hasResolution()) {
            try {
                startIntentSenderForResult(result.getPendingIntent().getIntentSender(),
                        REQUEST_FORMS_AUTHORIZATION, null, 0, 0, 0);
            } catch (Exception error) {
                showError("No se pudo abrir el permiso de Google.");
            }
            return;
        }
        accessToken = result.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            showError("Google no entregó permiso para editar este formulario.");
            return;
        }
        loadGoogleForm();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FORMS_AUTHORIZATION) return;
        if (resultCode != RESULT_OK || data == null) {
            showError("No se autorizó el acceso a Google Forms.");
            return;
        }
        try {
            AuthorizationResult result = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(data);
            handleAuthorizationResult(result);
        } catch (ApiException error) {
            showError("No se pudo validar el permiso de Google.");
        }
    }

    private void loadGoogleForm() {
        setBusy(true, "Cargando formulario…");
        String formId = surveyId.substring(3);
        FORMS_EXECUTOR.execute(() -> {
            try {
                GoogleFormsApi.FormSnapshot loaded = GoogleFormsApi.getForm(accessToken, formId);
                runOnUiThread(() -> {
                    formSnapshot = loaded;
                    titleInput.setText(loaded.title);
                    descriptionInput.setText(loaded.description);
                    statusSpinner.setSelection(loaded.isAcceptingResponses ? 0 : 1);
                    questionsContainer.removeAllViews();
                    questionRows.clear();
                    for (GoogleFormsApi.FormQuestion question : loaded.questions) {
                        addQuestionRow(new GoogleFormsApi.SurveyQuestion(question.title,
                                question.answerType, question.choices));
                    }
                    if (questionRows.isEmpty()) addQuestionRow(null);
                    setBusy(false, "Guardar cambios");
                });
            } catch (Exception error) {
                runOnUiThread(() -> showError(error.getMessage()));
            }
        });
    }

    private void saveChanges() {
        String title = titleInput.getText() == null ? "" : titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            titleInput.setError("Escribe el título de la encuesta");
            titleInput.requestFocus();
            return;
        }
        List<GoogleFormsApi.SurveyQuestion> questions = new ArrayList<>();
        for (QuestionRow row : questionRows) {
            String questionTitle = row.input.getText() == null
                    ? "" : row.input.getText().toString().trim();
            if (questionTitle.isEmpty()) {
                row.input.setError("Escribe la pregunta");
                row.input.requestFocus();
                return;
            }
            boolean yesNo = row.answerType.getSelectedItemPosition() == 1;
            questions.add(new GoogleFormsApi.SurveyQuestion(questionTitle,
                    yesNo ? "MULTIPLE_CHOICE" : "SHORT", yesNo ? "Sí, No" : ""));
        }

        String category = categorySpinner.getSelectedItem().toString();
        String status = statusSpinner.getSelectedItemPosition() == 0 ? "Vigente" : "Finalizada";
        description = descriptionInput.getText() == null
                ? "" : descriptionInput.getText().toString().trim();
        if (surveyId == null || !surveyId.startsWith("GF_")) {
            returnResult(title, category, status, questions);
            return;
        }
        if (formSnapshot == null || accessToken == null) {
            showError("No se pudo cargar el formulario. Cierra y vuelve a abrir Editar.");
            return;
        }

        setBusy(true, "Guardando cambios…");
        boolean acceptingResponses = "Vigente".equals(status);
        FORMS_EXECUTOR.execute(() -> {
            try {
                GoogleFormsApi.updateForm(accessToken, formSnapshot, title, description,
                        questions, acceptingResponses);
                runOnUiThread(() -> returnResult(title, category, status, questions));
            } catch (Exception error) {
                runOnUiThread(() -> showError(error.getMessage()));
            }
        });
    }

    private void returnResult(String title, String category, String status,
                              List<GoogleFormsApi.SurveyQuestion> questions) {
        Intent result = new Intent();
        result.putExtra(EXTRA_SURVEY_ID, surveyId);
        result.putExtra(EXTRA_TITLE, title);
        result.putExtra(EXTRA_CATEGORY, category);
        result.putExtra(EXTRA_STATUS, status);
        result.putExtra(EXTRA_QUESTIONS, encodeQuestions(questions));
        setResult(RESULT_OK, result);
        finish();
    }

    private void addQuestionRow(@Nullable GoogleFormsApi.SurveyQuestion question) {
        View rowView = getLayoutInflater().inflate(R.layout.item_create_survey_question,
                questionsContainer, false);
        TextView label = rowView.findViewById(R.id.surveyQuestionLabel);
        TextInputEditText input = rowView.findViewById(R.id.surveyQuestionInput);
        Spinner answerType = rowView.findViewById(R.id.surveyQuestionAnswerType);
        MaterialButton removeButton = rowView.findViewById(R.id.removeSurveyQuestion);
        setSpinnerItems(answerType, new String[]{"Texto corto", "Sí / No"});
        input.setTextColor(getColor(R.color.black));
        input.setHintTextColor(getColor(R.color.text_gray));
        if (question != null) {
            input.setText(question.title);
            answerType.setSelection("MULTIPLE_CHOICE".equals(question.answerType) ? 1 : 0);
        }
        QuestionRow row = new QuestionRow(rowView, label, input, answerType);
        removeButton.setOnClickListener(view -> {
            if (questionRows.size() <= 1) return;
            questionRows.remove(row);
            questionsContainer.removeView(row.view);
            refreshQuestionLabels();
        });
        questionRows.add(row);
        questionsContainer.addView(rowView);
        PaletteTheme.applyToView(rowView, this);
        refreshQuestionLabels();
    }

    private void refreshQuestionLabels() {
        questionCount.setText("Preguntas (" + questionRows.size() + ")");
        for (int i = 0; i < questionRows.size(); i++) {
            QuestionRow row = questionRows.get(i);
            row.label.setText("Pregunta " + (i + 1));
            row.view.findViewById(R.id.removeSurveyQuestion)
                    .setVisibility(questionRows.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void setSpinnerItems(Spinner spinner, String[] options) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, options) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(EditSurveyActivity.this));
                return text;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getDropDownView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(EditSurveyActivity.this));
                text.setBackgroundColor(PaletteTheme.surfaceColor(EditSurveyActivity.this));
                return text;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void selectSpinner(Spinner spinner, @Nullable String value) {
        if (value == null) return;
        ArrayAdapter<?> adapter = (ArrayAdapter<?>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private void setBusy(boolean busy, String label) {
        saveButton.setEnabled(!busy);
        saveButton.setText(label);
    }

    private void showError(String message) {
        setBusy(false, "Guardar cambios");
        Toast.makeText(this, message == null || message.trim().isEmpty()
                ? "No se pudieron guardar los cambios." : message, Toast.LENGTH_LONG).show();
    }

    private String authErrorMessage(Exception error) {
        String details = error.getLocalizedMessage();
        String normalized = details == null ? "" : details.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("access_denied") || normalized.contains("403")) {
            return "Agrega esta cuenta como usuario de prueba en Google Auth Platform > Público.";
        }
        return "No se pudo conectar con Google: " + details;
    }

    public static String encodeQuestions(List<GoogleFormsApi.SurveyQuestion> questions) {
        JSONArray result = new JSONArray();
        try {
            for (GoogleFormsApi.SurveyQuestion question : questions) {
                result.put(new JSONObject().put("title", question.title)
                        .put("answerType", question.answerType).put("choices", question.choices));
            }
        } catch (Exception ignored) { }
        return result.toString();
    }

    public static List<GoogleFormsApi.SurveyQuestion> decodeQuestions(@Nullable String raw) {
        List<GoogleFormsApi.SurveyQuestion> result = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return result;
        try {
            JSONArray saved = new JSONArray(raw);
            for (int i = 0; i < saved.length(); i++) {
                JSONObject question = saved.optJSONObject(i);
                if (question == null) continue;
                result.add(new GoogleFormsApi.SurveyQuestion(question.optString("title", ""),
                        question.optString("answerType", "SHORT"),
                        question.optString("choices", "")));
            }
        } catch (Exception ignored) { }
        return result;
    }

    private static final class QuestionRow {
        final View view;
        final TextView label;
        final TextInputEditText input;
        final Spinner answerType;
        QuestionRow(View view, TextView label, TextInputEditText input, Spinner answerType) {
            this.view = view;
            this.label = label;
            this.input = input;
            this.answerType = answerType;
        }
    }
}
