package com.example.app_encuestas;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateSurveyActivity extends AppCompatActivity {
    public static final String EXTRA_FORM_ID = "extra_form_id";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_CATEGORY = "extra_category";
    public static final String EXTRA_RESPONDER_URI = "extra_responder_uri";
    public static final String EXTRA_QUESTIONS = "extra_questions";
    public static final String EXTRA_DESCRIPTION = "extra_description";
    public static final String EXTRA_DRAFT_ID = "extra_draft_id";
    public static final String EXTRA_UPLOAD_DRAFT = "extra_upload_draft";

    private static final int REQUEST_FORMS_AUTHORIZATION = 6102;
    private static final String DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    private static final ExecutorService FORMS_EXECUTOR = Executors.newSingleThreadExecutor();

    private TextInputEditText titleInput;
    private TextInputEditText descriptionInput;
    private android.widget.LinearLayout questionsContainer;
    private TextView questionCount;
    private Spinner categorySpinner;
    private MaterialButton createButton;
    private MaterialButton saveDraftButton;
    private MaterialButton addQuestionButton;
    private final List<QuestionRow> questionRows = new ArrayList<>();
    private SurveyDraft pendingDraft;
    private String editingDraftId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_create_survey);
        PaletteTheme.apply(this);

        View root = findViewById(R.id.createSurveyRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top,
                    view.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        MaterialToolbar toolbar = findViewById(R.id.createSurveyToolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        titleInput = findViewById(R.id.newSurveyTitle);
        descriptionInput = findViewById(R.id.newSurveyDescription);
        questionsContainer = findViewById(R.id.surveyQuestionsContainer);
        questionCount = findViewById(R.id.surveyQuestionCount);
        categorySpinner = findViewById(R.id.newSurveyCategory);
        createButton = findViewById(R.id.submitNewSurvey);
        saveDraftButton = findViewById(R.id.saveSurveyDraft);
        addQuestionButton = findViewById(R.id.addSurveyQuestion);
        setInputTextColors(titleInput);
        setInputTextColors(descriptionInput);

        setSpinnerItems(categorySpinner, new String[]{
                "Servicios", "Seguridad", "Transporte", "Espacios públicos", "Atención ciudadana"
        });
        addQuestionButton.setOnClickListener(view -> addQuestionRow());
        editingDraftId = getIntent().getStringExtra(EXTRA_DRAFT_ID);
        SurveyDraft savedDraft = SurveyDraftStore.getDraft(this, editingDraftId);
        if (savedDraft != null) {
            titleInput.setText(savedDraft.title);
            descriptionInput.setText(savedDraft.description);
            for (GoogleFormsApi.SurveyQuestion question : savedDraft.questions) addQuestionRow(question);
            selectCategory(savedDraft.category);
        }
        if (questionRows.isEmpty()) addQuestionRow();
        createButton.setOnClickListener(view -> validateAndAuthorize());
        saveDraftButton.setOnClickListener(view -> saveDraft());
        if (getIntent().getBooleanExtra(EXTRA_UPLOAD_DRAFT, false)) validateAndAuthorize();
    }

    private void validateAndAuthorize() {
        pendingDraft = readFormDraft();
        if (pendingDraft == null) return;
        setBusy(true, "Conectando con Google…");

        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DRIVE_FILE_SCOPE)))
                .build();
        Identity.getAuthorizationClient(this).authorize(request)
                .addOnSuccessListener(this, this::handleAuthorizationResult)
                .addOnFailureListener(this, error -> showCreateError(authErrorMessage(error)));
    }

    private SurveyDraft readFormDraft() {
        String title = textOf(titleInput);
        if (title.isEmpty()) {
            titleInput.setError("Escribe el título de la encuesta");
            titleInput.requestFocus();
            return null;
        }
        List<GoogleFormsApi.SurveyQuestion> questions = new ArrayList<>();
        for (QuestionRow row : questionRows) {
            String question = textOf(row.input);
            if (question.isEmpty()) {
                row.input.setError("Escribe la pregunta");
                row.input.requestFocus();
                return null;
            }
            boolean yesNo = row.answerType.getSelectedItemPosition() == 1;
            questions.add(new GoogleFormsApi.SurveyQuestion(question,
                    yesNo ? "MULTIPLE_CHOICE" : "SHORT", yesNo ? "Sí, No" : ""));
        }
        return new SurveyDraft(editingDraftId == null ? UUID.randomUUID().toString() : editingDraftId,
                title, textOf(descriptionInput), categorySpinner.getSelectedItem().toString(),
                System.currentTimeMillis(), questions);
    }

    private void saveDraft() {
        SurveyDraft draft = readFormDraft();
        if (draft == null) return;
        SurveyDraftStore.save(this, draft);
        Intent result = new Intent();
        result.putExtra(EXTRA_DRAFT_ID, draft.id);
        setResult(RESULT_OK, result);
        Toast.makeText(this, "Borrador guardado en este dispositivo", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String authErrorMessage(Exception error) {
        String details = error.getLocalizedMessage();
        String normalized = details == null ? "" : details.toLowerCase(java.util.Locale.ROOT);
        if (normalized.contains("access_denied") || normalized.contains("403")
                || normalized.contains("not completed the verification")) {
            return "Google bloqueó esta cuenta porque la app está en modo de prueba. "
                    + "En Google Cloud > Google Auth Platform > Público, agrega "
                    + "este correo en Usuarios de prueba y vuelve a intentarlo.";
        }
        return "No se pudo conectar con Google: " + (details == null ? "intenta otra vez." : details);
    }

    private void addQuestionRow() {
        addQuestionRow(null);
    }

    private void addQuestionRow(@Nullable GoogleFormsApi.SurveyQuestion question) {
        View rowView = getLayoutInflater().inflate(R.layout.item_create_survey_question,
                questionsContainer, false);
        TextView label = rowView.findViewById(R.id.surveyQuestionLabel);
        TextInputEditText input = rowView.findViewById(R.id.surveyQuestionInput);
        Spinner answerType = rowView.findViewById(R.id.surveyQuestionAnswerType);
        MaterialButton removeButton = rowView.findViewById(R.id.removeSurveyQuestion);
        setSpinnerItems(answerType, new String[]{"Texto corto", "Sí / No"});
        setInputTextColors(input);
        if (question != null) {
            input.setText(question.title);
            answerType.setSelection("MULTIPLE_CHOICE".equals(question.answerType) ? 1 : 0);
        }
        QuestionRow row = new QuestionRow(rowView, label, input, answerType);
        removeButton.setOnClickListener(view -> {
            if (questionRows.size() <= 1) return;
            questionRows.remove(row);
            questionsContainer.removeView(row.view);
            refreshQuestionRows();
        });
        questionRows.add(row);
        questionsContainer.addView(rowView);
        PaletteTheme.applyToView(rowView, this);
        refreshQuestionRows();
    }

    private void refreshQuestionRows() {
        questionCount.setText("Preguntas (" + questionRows.size() + ")");
        for (int i = 0; i < questionRows.size(); i++) {
            QuestionRow row = questionRows.get(i);
            row.label.setText("Pregunta " + (i + 1));
            row.view.findViewById(R.id.removeSurveyQuestion)
                    .setVisibility(questionRows.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void handleAuthorizationResult(AuthorizationResult result) {
        if (result.hasResolution()) {
            try {
                startIntentSenderForResult(result.getPendingIntent().getIntentSender(),
                        REQUEST_FORMS_AUTHORIZATION, null, 0, 0, 0);
            } catch (Exception error) {
                showCreateError("No se pudo abrir el permiso de Google.");
            }
            return;
        }

        String accessToken = result.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            showCreateError("Google no entregó el permiso para crear formularios.");
            return;
        }
        createGoogleForm(accessToken);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_FORMS_AUTHORIZATION) return;
        if (resultCode != RESULT_OK || data == null) {
            showCreateError("No se autorizó el acceso a Google Forms.");
            return;
        }
        try {
            AuthorizationResult result = Identity.getAuthorizationClient(this)
                    .getAuthorizationResultFromIntent(data);
            handleAuthorizationResult(result);
        } catch (ApiException error) {
            showCreateError("No se pudo validar el permiso de Google.");
        }
    }

    private void createGoogleForm(String accessToken) {
        SurveyDraft draft = pendingDraft;
        if (draft == null) return;
        setBusy(true, "Creando formulario…");
        FORMS_EXECUTOR.execute(() -> {
            try {
                GoogleFormsApi.CreatedForm created = GoogleFormsApi.createSurvey(accessToken,
                        draft.title, draft.description, draft.questions);
                runOnUiThread(() -> {
                    Intent result = new Intent();
                    result.putExtra(EXTRA_FORM_ID, created.formId);
                    result.putExtra(EXTRA_TITLE, draft.title);
                    result.putExtra(EXTRA_CATEGORY, draft.category);
                    result.putExtra(EXTRA_RESPONDER_URI, created.responderUri);
                    result.putExtra(EXTRA_QUESTIONS, EditSurveyActivity.encodeQuestions(draft.questions));
                    result.putExtra(EXTRA_DESCRIPTION, draft.description);
                    if (editingDraftId != null) result.putExtra(EXTRA_DRAFT_ID, editingDraftId);
                    setResult(RESULT_OK, result);
                    finish();
                });
            } catch (Exception error) {
                runOnUiThread(() -> showCreateError(error.getMessage()));
            }
        });
    }

    private void showCreateError(String message) {
        setBusy(false, "Publicar encuesta");
        Toast.makeText(this, message == null || message.trim().isEmpty()
                ? "No se pudo crear la encuesta." : message, Toast.LENGTH_LONG).show();
    }

    private void setBusy(boolean busy, String label) {
        createButton.setEnabled(!busy);
        createButton.setText(label);
        saveDraftButton.setEnabled(!busy);
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void setInputTextColors(TextInputEditText input) {
        input.setTextColor(getColor(R.color.black));
        input.setHintTextColor(getColor(R.color.text_gray));
    }

    private void setSpinnerItems(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(CreateSurveyActivity.this));
                return text;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView text = (TextView) super.getDropDownView(position, convertView, parent);
                text.setTextColor(PaletteTheme.principalColor(CreateSurveyActivity.this));
                text.setBackgroundColor(PaletteTheme.surfaceColor(CreateSurveyActivity.this));
                return text;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void selectCategory(String category) {
        if (category == null) return;
        for (int i = 0; i < categorySpinner.getCount(); i++) {
            if (category.equals(categorySpinner.getItemAtPosition(i).toString())) {
                categorySpinner.setSelection(i);
                return;
            }
        }
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
