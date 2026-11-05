package com.example.app_encuestas;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.EnumMap;
import java.util.Map;

/** Shared, scrollable and screen-reader-friendly read-only survey details. */
public final class SurveyDetailsDialog {
    private SurveyDetailsDialog() { }

    public static void show(Activity activity, Survey survey, boolean includeQrCode) {
        View content = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_survey_details, null, false);
        ((TextView) content.findViewById(R.id.detailsSurveyCategory))
                .setText(survey.getCategory());
        ((TextView) content.findViewById(R.id.detailsSurveyStatus))
                .setText("Estado: " + survey.getStatus());
        ((TextView) content.findViewById(R.id.detailsSurveyDate))
                .setText("Fecha de creación: " + survey.getCreationDate());
        ((TextView) content.findViewById(R.id.detailsSurveyId))
                .setText("Identificador: " + survey.getId());
        TextView responseSummary = content.findViewById(R.id.detailsSurveyResponses);
        if (!survey.isGoogleForm() && survey.getResponseCount() > 0) {
            responseSummary.setText("Resultados de muestra · " + survey.getResponseCount()
                    + " respuestas");
            responseSummary.setVisibility(View.VISIBLE);
        }

        LinearLayout questions = content.findViewById(R.id.detailsQuestionsContainer);
        TextView heading = content.findViewById(R.id.detailsQuestionsHeading);
        TextView empty = content.findViewById(R.id.detailsNoQuestions);
        heading.setText("Preguntas (" + survey.getQuestions().size() + ")");
        if (survey.getQuestions().isEmpty()) {
            empty.setVisibility(View.VISIBLE);
        } else {
            for (int i = 0; i < survey.getQuestions().size(); i++) {
                GoogleFormsApi.SurveyQuestion question = survey.getQuestions().get(i);
                View row = LayoutInflater.from(activity).inflate(
                        R.layout.item_survey_question_detail, questions, false);
                ((TextView) row.findViewById(R.id.detailsQuestionNumber))
                        .setText("Pregunta " + (i + 1));
                ((TextView) row.findViewById(R.id.detailsQuestionTitle))
                        .setText(question.title);
                LinearLayout resultRows = row.findViewById(R.id.detailsQuestionResults);
                TextView options = row.findViewById(R.id.detailsQuestionOptions);
                if ("MULTIPLE_CHOICE".equals(question.answerType)) {
                    if (question.choices != null && !question.choices.trim().isEmpty()) {
                        options.setText("Opciones: " + question.choices);
                    } else {
                        options.setText("Tipo de respuesta: Opción múltiple");
                    }
                } else {
                    options.setText("Tipo de respuesta: Texto abierto");
                }
                Map<String, Integer> distribution = survey.getQuestionResults(question.title);
                if (!survey.isGoogleForm() && !distribution.isEmpty()) {
                    int max = 1;
                    int total = 0;
                    for (Integer count : distribution.values()) {
                        max = Math.max(max, count == null ? 0 : count);
                        total += count == null ? 0 : count;
                    }
                    for (Map.Entry<String, Integer> result : distribution.entrySet()) {
                        View resultRow = LayoutInflater.from(activity).inflate(
                                R.layout.item_survey_stat_result, resultRows, false);
                        int count = result.getValue() == null ? 0 : result.getValue();
                        int percent = total == 0 ? 0 : Math.round(count * 100f / total);
                        ((TextView) resultRow.findViewById(R.id.statResultLabel))
                                .setText(result.getKey() + " · " + count + " (" + percent + "%)");
                        ProgressBar progress = resultRow.findViewById(R.id.statResultProgress);
                        progress.setMax(max);
                        progress.setProgress(count);
                        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(
                                PaletteTheme.accentColor(activity)));
                        resultRows.addView(resultRow);
                    }
                }
                PaletteTheme.applyToView(row, activity);
                questions.addView(row);
            }
        }

        if (includeQrCode && survey.isGoogleForm() && survey.getQrCodeString() != null
                && !survey.getQrCodeString().trim().isEmpty()) {
            View qrSection = content.findViewById(R.id.detailsQrSection);
            qrSection.setVisibility(View.VISIBLE);
            ((TextView) content.findViewById(R.id.detailsSurveyUrl))
                    .setText(survey.getQrCodeString());
            ImageView qr = content.findViewById(R.id.detailsSurveyQr);
            qr.setContentDescription("Código QR para responder: " + survey.getTitle());
            try {
                Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
                hints.put(EncodeHintType.MARGIN, 1);
                BitMatrix matrix = new MultiFormatWriter().encode(
                        survey.getQrCodeString(), BarcodeFormat.QR_CODE, 420, 420, hints);
                Bitmap bitmap = Bitmap.createBitmap(420, 420, Bitmap.Config.RGB_565);
                for (int x = 0; x < 420; x++) {
                    for (int y = 0; y < 420; y++) {
                        bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                    }
                }
                qr.setImageBitmap(bitmap);
                content.findViewById(R.id.detailsShareQr).setOnClickListener(view ->
                        SurveyQrExporter.share(activity, survey, bitmap));
                content.findViewById(R.id.detailsPrintQr).setOnClickListener(view ->
                        SurveyQrExporter.print(activity, survey, bitmap));
            } catch (Exception ignored) {
                qr.setVisibility(View.GONE);
                content.findViewById(R.id.detailsShareQr).setVisibility(View.GONE);
                content.findViewById(R.id.detailsPrintQr).setVisibility(View.GONE);
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(survey.getTitle())
                .setView(content)
                .setPositiveButton("Cerrar", null)
                .create();
        dialog.setOnShowListener(ignored -> PaletteTheme.applyToDialog(dialog, activity));
        dialog.show();
    }
}
