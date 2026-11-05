package com.example.app_encuestas;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class SurveyDeleteDialogFragment extends DialogFragment {
    public interface Listener {
        void onSurveyDeleteConfirmed(String surveyId);
    }

    private static final String ARG_ID = "survey_id";
    private static final String ARG_TITLE = "survey_title";
    private static final String ARG_GOOGLE_FORM = "google_form";

    public static SurveyDeleteDialogFragment newInstance(Survey survey) {
        SurveyDeleteDialogFragment dialog = new SurveyDeleteDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, survey.getId());
        args.putString(ARG_TITLE, survey.getTitle());
        args.putBoolean(ARG_GOOGLE_FORM, survey.isGoogleForm());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        String id = args.getString(ARG_ID, "");
        String title = args.getString(ARG_TITLE, "esta encuesta");
        boolean googleForm = args.getBoolean(ARG_GOOGLE_FORM);
        String message = googleForm
                ? "¿Quieres quitar “" + title + "” de esta lista? El formulario y sus respuestas seguirán en Google Forms."
                : "¿Quieres eliminar “" + title + "”?";
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(googleForm ? "Quitar encuesta" : "Eliminar encuesta")
                .setMessage(message)
                .setNegativeButton("Cancelar", (action, which) -> dismiss())
                .setPositiveButton(googleForm ? "Quitar" : "Eliminar", (action, which) -> {
                    if (getActivity() instanceof Listener) {
                        ((Listener) getActivity()).onSurveyDeleteConfirmed(id);
                    }
                })
                .create();
        dialog.setOnShowListener(ignored -> PaletteTheme.applyToDialog(dialog, requireActivity()));
        return dialog;
    }
}
