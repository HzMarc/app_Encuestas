package com.example.app_encuestas;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

public class SurveyManagementActivity extends AppCompatActivity
        implements SurveyDeleteDialogFragment.Listener {
    private SurveyAdapter publishedAdapter;
    private SurveyDraftAdapter draftAdapter;
    private TextView emptyGoogleForms;
    private TextView emptySurveyDrafts;
    private RecyclerView recycler;
    private ActivityResultLauncher<Intent> createSurveyLauncher;
    private ActivityResultLauncher<Intent> editSurveyLauncher;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_management);
        createSurveyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Intent data = result.getData();
                    String formId = data.getStringExtra(CreateSurveyActivity.EXTRA_FORM_ID);
                    String title = data.getStringExtra(CreateSurveyActivity.EXTRA_TITLE);
                    String category = data.getStringExtra(CreateSurveyActivity.EXTRA_CATEGORY);
                    String responderUri = data.getStringExtra(CreateSurveyActivity.EXTRA_RESPONDER_URI);
                    String draftId = data.getStringExtra(CreateSurveyActivity.EXTRA_DRAFT_ID);
                    if (formId == null || title == null || responderUri == null) {
                        refreshDrafts();
                        showDrafts();
                        return;
                    }

                    String savedCategory = category == null ? "Servicios" : category;
                    Survey createdSurvey = MockSurveyData.addGoogleForm(this, formId, title,
                            savedCategory, responderUri);
                    if (draftId != null) SurveyDraftStore.delete(this, draftId);
                    MockSurveyData.updateSurvey(this, createdSurvey, title, savedCategory,
                            "Vigente", EditSurveyActivity.decodeQuestions(
                                    data.getStringExtra(CreateSurveyActivity.EXTRA_QUESTIONS)));
                    publishedAdapter.notifyDataSetChanged();
                    refreshDrafts();
                    showPublished();
                    AlertDialog successDialog = new AlertDialog.Builder(this)
                            .setTitle("Encuesta publicada")
                            .setMessage("El formulario ya está en Google Forms.")
                            .setNegativeButton("Cerrar", null)
                            .setPositiveButton("Abrir formulario", (dialog, which) -> openGoogleFormEditor(formId))
                            .create();
                    successDialog.setOnShowListener(ignored ->
                            PaletteTheme.applyToDialog(successDialog, this));
                    successDialog.show();
                });
        editSurveyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    Intent data = result.getData();
                    String id = data.getStringExtra(EditSurveyActivity.EXTRA_SURVEY_ID);
                    String title = data.getStringExtra(EditSurveyActivity.EXTRA_TITLE);
                    String category = data.getStringExtra(EditSurveyActivity.EXTRA_CATEGORY);
                    String status = data.getStringExtra(EditSurveyActivity.EXTRA_STATUS);
                    String rawQuestions = data.getStringExtra(EditSurveyActivity.EXTRA_QUESTIONS);
                    if (id == null || title == null || status == null) return;
                    for (Survey survey : MockSurveyData.getSurveys()) {
                        if (survey.getId().equals(id)) {
                            MockSurveyData.updateSurvey(this, survey, title,
                                    category == null ? survey.getCategory() : category, status,
                                    EditSurveyActivity.decodeQuestions(rawQuestions));
                            break;
                        }
                    }
                    publishedAdapter.notifyDataSetChanged();
                });
        MaterialToolbar toolbar = findViewById(R.id.surveyToolbar);
        ScreenChrome.setup(this, findViewById(R.id.surveyManagementRoot), toolbar, R.id.nav_surveys);
        toolbar.setTitle("Encuestas");
        toolbar.getMenu().clear();
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);
        MockSurveyData.loadGoogleForms(this);
        emptyGoogleForms = findViewById(R.id.emptyGoogleForms);
        emptySurveyDrafts = findViewById(R.id.emptySurveyDrafts);

        recycler = findViewById(R.id.surveyRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        publishedAdapter = new SurveyAdapter(MockSurveyData.getGoogleForms(), new SurveyAdapter.Actions() {
            @Override public void onOpen(Survey survey) { showDetails(survey); }
            @Override public void onEdit(Survey survey) { openEditSurvey(survey); }
            @Override public void onDelete(Survey survey) { confirmDelete(survey); }
        });
        draftAdapter = new SurveyDraftAdapter(SurveyDraftStore.getDrafts(this), draftAdapterActions());
        recycler.setAdapter(publishedAdapter);
        updateEmptyStates();
        findViewById(R.id.showPublishedSurveys).setOnClickListener(v -> showPublished());
        findViewById(R.id.showSurveyDrafts).setOnClickListener(v -> showDrafts());
        findViewById(R.id.btnCreateGoogleForm).setOnClickListener(v -> openCreateSurveyScreen());
    }

    @Override protected void onResume() {
        super.onResume();
        PaletteTheme.apply(this);
        refreshDrafts();
    }

    private void openCreateSurveyScreen() {
        createSurveyLauncher.launch(new Intent(this, CreateSurveyActivity.class));
    }

    private void openEditSurvey(Survey survey) {
        Intent intent = new Intent(this, EditSurveyActivity.class);
        intent.putExtra(EditSurveyActivity.EXTRA_SURVEY_ID, survey.getId());
        intent.putExtra(EditSurveyActivity.EXTRA_TITLE, survey.getTitle());
        intent.putExtra(EditSurveyActivity.EXTRA_CATEGORY, survey.getCategory());
        intent.putExtra(EditSurveyActivity.EXTRA_STATUS, survey.getStatus());
        intent.putExtra(EditSurveyActivity.EXTRA_QUESTIONS,
                EditSurveyActivity.encodeQuestions(survey.getQuestions()));
        editSurveyLauncher.launch(intent);
    }

    private SurveyDraftAdapter.Actions draftAdapterActions() {
        return new SurveyDraftAdapter.Actions() {
            @Override public void onEdit(SurveyDraft draft) {
                Intent intent = new Intent(SurveyManagementActivity.this, CreateSurveyActivity.class);
                intent.putExtra(CreateSurveyActivity.EXTRA_DRAFT_ID, draft.id);
                createSurveyLauncher.launch(intent);
            }
            @Override public void onDelete(SurveyDraft draft) { confirmDeleteDraft(draft); }
            @Override public void onUpload(SurveyDraft draft) {
                Intent intent = new Intent(SurveyManagementActivity.this, CreateSurveyActivity.class);
                intent.putExtra(CreateSurveyActivity.EXTRA_DRAFT_ID, draft.id);
                intent.putExtra(CreateSurveyActivity.EXTRA_UPLOAD_DRAFT, true);
                createSurveyLauncher.launch(intent);
            }
        };
    }

    private void refreshDrafts() {
        if (recycler == null) return;
        boolean showingDrafts = recycler.getAdapter() == draftAdapter;
        draftAdapter = new SurveyDraftAdapter(SurveyDraftStore.getDrafts(this), draftAdapterActions());
        if (showingDrafts) recycler.setAdapter(draftAdapter);
        updateEmptyStates();
    }

    private void showPublished() {
        recycler.setAdapter(publishedAdapter);
        updateEmptyStates();
    }

    private void showDrafts() {
        recycler.setAdapter(draftAdapter);
        updateEmptyStates();
    }

    private void updateEmptyStates() {
        if (emptyGoogleForms != null) emptyGoogleForms.setVisibility(
                recycler.getAdapter() == publishedAdapter && MockSurveyData.getGoogleForms().isEmpty()
                        ? android.view.View.VISIBLE : android.view.View.GONE);
        if (emptySurveyDrafts != null) emptySurveyDrafts.setVisibility(
                recycler.getAdapter() == draftAdapter && draftAdapter.getItemCount() == 0
                        ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void confirmDeleteDraft(SurveyDraft draft) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Eliminar borrador")
                .setMessage("¿Quieres eliminar el borrador «" + draft.title + "»?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (ignored, which) -> {
                    SurveyDraftStore.delete(this, draft.id);
                    refreshDrafts();
                })
                .create();
        dialog.setOnShowListener(ignored -> PaletteTheme.applyToDialog(dialog, this));
        dialog.show();
    }

    private void openGoogleFormEditor(Survey survey) { openGoogleFormEditor(survey.getGoogleFormId()); }

    private void openGoogleFormEditor(String formId) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://docs.google.com/forms/d/" + formId + "/edit"));
        startActivity(intent);
    }

    private void confirmDelete(Survey survey) {
        DialogFragment dialog = SurveyDeleteDialogFragment.newInstance(survey);
        dialog.show(getSupportFragmentManager(), "confirmSurveyDelete");
    }

    @Override public void onSurveyDeleteConfirmed(String surveyId) {
        for (Survey survey : MockSurveyData.getSurveys()) {
            if (!survey.getId().equals(surveyId)) continue;
            MockSurveyData.removeSurvey(this, survey);
            publishedAdapter.notifyDataSetChanged();
            updateEmptyStates();
            Toast.makeText(this, survey.isGoogleForm()
                    ? "Quitada de la lista; sigue en Google Forms"
                    : "Encuesta eliminada", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    private void showDetails(Survey survey) { SurveyDetailsDialog.show(this, survey, true); }
}
