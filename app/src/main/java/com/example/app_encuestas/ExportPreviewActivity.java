package com.example.app_encuestas;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportPreviewActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "preview_report_title";
    public static final String EXTRA_FORMAT = "preview_report_format";
    public static final String EXTRA_SURVEYS = "preview_report_surveys";
    public static final String EXTRA_STATISTICS = "preview_statistics";
    public static final String FORMAT_PDF = "pdf";
    public static final String FORMAT_EXCEL = "excel";

    private final ActivityResultLauncher<String> savePdf = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"),
            uri -> saveReport(uri, true, false));
    private final ActivityResultLauncher<String> saveExcel = registerForActivityResult(
            new ActivityResultContracts.CreateDocument(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            uri -> saveReport(uri, false, false));

    private String reportTitle;
    private String format;
    private List<Survey> surveys = new ArrayList<>();
    private StatisticsReportExporter.Report statisticsReport;
    private LinearLayout previewContent;
    private MaterialButton downloadButton;
    private File pdfPreviewFile;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_export_preview);
        PaletteTheme.apply(this);

        View root = findViewById(R.id.exportPreviewRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), systemBars.top,
                    view.getPaddingRight(), systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        reportTitle = getIntent().getStringExtra(EXTRA_TITLE);
        if (reportTitle == null) reportTitle = "Reporte de encuestas";
        format = getIntent().getStringExtra(EXTRA_FORMAT);
        surveys = SurveyReportExporter.decodeSurveys(getIntent().getStringExtra(EXTRA_SURVEYS));
        String statsJson = getIntent().getStringExtra(EXTRA_STATISTICS);
        if (statsJson != null) statisticsReport = StatisticsReportExporter.decode(statsJson);
        previewContent = findViewById(R.id.exportPreviewContent);
        downloadButton = findViewById(R.id.downloadReportButton);

        MaterialToolbar toolbar = findViewById(R.id.exportPreviewToolbar);
        toolbar.setNavigationOnClickListener(view -> finish());
        boolean isPdf = FORMAT_PDF.equals(format);
        toolbar.setTitle(isPdf ? "Vista previa PDF" : "Vista previa Excel");
        TextView summary = findViewById(R.id.exportPreviewSummary);
        summary.setText(statisticsReport == null
                ? reportTitle + "\n" + surveys.size()
                + (surveys.size() == 1 ? " encuesta" : " encuestas")
                : reportTitle + "\n" + statisticsReport.category + " · "
                + statisticsReport.survey + "\n" + statisticsReport.summary);
        downloadButton.setText(isPdf ? "Descargar PDF" : "Descargar Excel");
        downloadButton.setOnClickListener(view -> launchSave());

        if (isPdf) showPdfPreview();
        else showExcelPreview();
    }

    private void showPdfPreview() {
        try {
            pdfPreviewFile = new File(getCacheDir(), "reporte-vista-previa.pdf");
            try (OutputStream output = new FileOutputStream(pdfPreviewFile)) {
                if (statisticsReport == null) {
                    SurveyReportExporter.writePdf(this, output, reportTitle, surveys);
                } else {
                    StatisticsReportExporter.writePdf(output, reportTitle, statisticsReport);
                }
            }
            try (ParcelFileDescriptor descriptor = ParcelFileDescriptor.open(
                    pdfPreviewFile, ParcelFileDescriptor.MODE_READ_ONLY);
                 PdfRenderer renderer = new PdfRenderer(descriptor)) {
                for (int i = 0; i < renderer.getPageCount(); i++) {
                    PdfRenderer.Page page = renderer.openPage(i);
                    Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(),
                            Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();

                    ImageView pageView = new ImageView(this);
                    pageView.setImageBitmap(bitmap);
                    pageView.setAdjustViewBounds(true);
                    pageView.setBackgroundColor(Color.WHITE);
                    pageView.setElevation(dp(2));
                    pageView.setContentDescription("Página " + (i + 1) + " del PDF");
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(dp(8), dp(8), dp(8), dp(14));
                    previewContent.addView(pageView, params);
                }
            }
        } catch (Exception error) {
            showPreviewError(error);
        }
    }

    private void showExcelPreview() {
        if (statisticsReport != null) {
            showStatisticsExcelPreview();
            return;
        }
        TextView sheetName = new TextView(this);
        sheetName.setText("Hoja: Reporte");
        sheetName.setTextColor(getColor(R.color.black));
        sheetName.setTextSize(14);
        sheetName.setTypeface(null, android.graphics.Typeface.BOLD);
        sheetName.setPadding(dp(12), dp(10), dp(12), dp(8));
        previewContent.addView(sheetName);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setFillViewport(false);
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(false);
        addTableRow(table, new String[]{"Encuesta", "Categoría", "Creada", "Estado",
                "Enlace", "Respuestas", "Pregunta", "Tipo de respuesta", "Opciones",
                "Resultado", "Conteo", "Porcentaje"}, true);
        for (Survey survey : surveys) {
            if (survey.getQuestions().isEmpty()) {
                addTableRow(table, new String[]{survey.getTitle(), survey.getCategory(),
                        survey.getCreationDate(), survey.getStatus(), survey.getQrCodeString(),
                        survey.isGoogleForm() ? "" : String.valueOf(survey.getResponseCount()),
                        "", "", "", "", "", ""}, false);
            } else {
                for (GoogleFormsApi.SurveyQuestion question : survey.getQuestions()) {
                    java.util.Map<String, Integer> results = survey.getQuestionResults(question.title);
                    int total = 0;
                    for (Integer count : results.values()) total += count == null ? 0 : count;
                    if (results.isEmpty()) {
                        addTableRow(table, new String[]{survey.getTitle(), survey.getCategory(),
                                survey.getCreationDate(), survey.getStatus(), survey.getQrCodeString(),
                                survey.isGoogleForm() ? "" : String.valueOf(survey.getResponseCount()),
                                question.title, "MULTIPLE_CHOICE".equals(question.answerType)
                                ? "Opción múltiple" : "Texto abierto", question.choices,
                                "", "", ""}, false);
                    } else {
                        for (java.util.Map.Entry<String, Integer> result : results.entrySet()) {
                            int count = result.getValue() == null ? 0 : result.getValue();
                            addTableRow(table, new String[]{survey.getTitle(), survey.getCategory(),
                                    survey.getCreationDate(), survey.getStatus(), survey.getQrCodeString(),
                                    String.valueOf(survey.getResponseCount()), question.title,
                                    "MULTIPLE_CHOICE".equals(question.answerType)
                                            ? "Opción múltiple" : "Texto abierto", question.choices,
                                    result.getKey(), String.valueOf(count),
                                    total == 0 ? "0%" : Math.round(count * 100f / total) + "%"}, false);
                        }
                    }
                }
            }
        }
        horizontal.addView(table);
        previewContent.addView(horizontal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView note = new TextView(this);
        note.setText("Desliza horizontalmente para ver todas las columnas.");
        note.setTextColor(getColor(R.color.text_gray));
        note.setTextSize(12);
        note.setPadding(dp(12), dp(10), dp(12), dp(16));
        previewContent.addView(note);
    }

    private void showStatisticsExcelPreview() {
        TextView sheetName = new TextView(this);
        sheetName.setText("Hoja: Estadísticas filtradas");
        sheetName.setTextColor(getColor(R.color.black));
        sheetName.setTextSize(14);
        sheetName.setTypeface(null, android.graphics.Typeface.BOLD);
        sheetName.setPadding(dp(12), dp(10), dp(12), dp(8));
        previewContent.addView(sheetName);
        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        TableLayout table = new TableLayout(this);
        addTableRow(table, new String[]{"Filtro", "Selección"}, true);
        addTableRow(table, new String[]{"Categoría", statisticsReport.category}, false);
        addTableRow(table, new String[]{"Encuesta", statisticsReport.survey}, false);
        addTableRow(table, new String[]{"Pregunta", statisticsReport.question}, false);
        addTableRow(table, new String[]{"Resumen", statisticsReport.summary}, false);
        addTableRow(table, new String[]{statisticsReport.legend, "Respuestas"}, true);
        for (int i = 0; i < Math.min(statisticsReport.labels.size(),
                statisticsReport.values.size()); i++) {
            addTableRow(table, new String[]{statisticsReport.labels.get(i),
                    String.valueOf(statisticsReport.values.get(i))}, false);
        }
        if (!statisticsReport.dates.isEmpty()) {
            addTableRow(table, new String[]{"Fecha", "Respuestas"}, true);
            for (java.util.Map.Entry<String, Integer> entry : statisticsReport.dates.entrySet()) {
                addTableRow(table, new String[]{entry.getKey(), String.valueOf(entry.getValue())}, false);
            }
        }
        horizontal.addView(table);
        previewContent.addView(horizontal, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void addTableRow(TableLayout table, String[] values, boolean header) {
        TableRow row = new TableRow(this);
        for (String value : values) {
            TextView cell = new TextView(this);
            cell.setText(value == null ? "" : value);
            cell.setTextSize(header ? 12 : 11);
            cell.setTextColor(header ? Color.WHITE : getColor(R.color.black));
            cell.setPadding(dp(10), dp(9), dp(10), dp(9));
            cell.setMinWidth(dp(112));
            cell.setMaxWidth(dp(220));
            cell.setBackgroundColor(header ? getColor(R.color.primary_blue)
                    : getColor(R.color.white));
            row.addView(cell, new TableRow.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        table.addView(row, new TableLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void launchSave() {
        boolean isPdf = FORMAT_PDF.equals(format);
        String baseName = reportTitle.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (baseName.isEmpty()) baseName = "Reporte_de_encuestas";
        String extension = isPdf ? ".pdf" : ".xlsx";
        String mimeType = isPdf ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        String fileName = baseName + "_"
                + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date())
                + extension;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/Encuestas");
            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, "No se pudo crear el archivo en Descargas.", Toast.LENGTH_LONG).show();
                return;
            }
            saveReport(uri, isPdf, true);
        } else if (isPdf) {
            savePdf.launch(fileName);
        } else {
            saveExcel.launch(fileName);
        }
    }

    private void saveReport(Uri uri, boolean pdf, boolean directDownload) {
        if (uri == null) return;
        try (OutputStream output = getContentResolver().openOutputStream(uri)) {
            if (output == null) throw new IllegalStateException("No se pudo abrir el archivo elegido.");
            if (pdf) {
                if (pdfPreviewFile == null || !pdfPreviewFile.exists()) {
                    throw new IllegalStateException("No se encontró el PDF de la vista previa.");
                }
                try (InputStream input = new FileInputStream(pdfPreviewFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                }
            } else {
                if (statisticsReport == null) {
                    SurveyReportExporter.writeExcel(output, reportTitle, surveys);
                } else {
                    StatisticsReportExporter.writeExcel(output, reportTitle, statisticsReport);
                }
            }
            Toast.makeText(this, directDownload
                    ? "Reporte guardado en Descargas/Encuestas"
                    : "Reporte guardado", Toast.LENGTH_LONG).show();
        } catch (Exception error) {
            if (directDownload) getContentResolver().delete(uri, null, null);
            Toast.makeText(this, "No se pudo guardar el reporte: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showPreviewError(Exception error) {
        TextView message = new TextView(this);
        message.setText("No se pudo crear la vista previa: " + error.getLocalizedMessage());
        message.setTextColor(getColor(R.color.black));
        message.setPadding(dp(16), dp(16), dp(16), dp(16));
        previewContent.addView(message);
        downloadButton.setEnabled(false);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onDestroy() {
        if (pdfPreviewFile != null && pdfPreviewFile.exists()) pdfPreviewFile.delete();
        super.onDestroy();
    }
}
