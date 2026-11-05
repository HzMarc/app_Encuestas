package com.example.app_encuestas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.content.Context;

import java.io.OutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class SurveyReportExporter {
    private SurveyReportExporter() { }

    public static String encodeSurveys(List<Survey> surveys) {
        JSONArray result = new JSONArray();
        try {
            for (Survey survey : surveys) {
                JSONObject item = new JSONObject()
                        .put("id", survey.getId())
                        .put("title", survey.getTitle())
                        .put("category", survey.getCategory())
                        .put("date", survey.getCreationDate())
                        .put("status", survey.getStatus())
                        .put("link", survey.getQrCodeString())
                        .put("responseCount", survey.getResponseCount());
                JSONArray questions = new JSONArray();
                for (GoogleFormsApi.SurveyQuestion question : survey.getQuestions()) {
                    questions.put(new JSONObject()
                            .put("title", question.title)
                            .put("answerType", question.answerType)
                            .put("choices", question.choices));
                }
                item.put("questions", questions);
                JSONArray stats = new JSONArray();
                for (java.util.Map.Entry<String, java.util.Map<String, Integer>> entry
                        : survey.getAllQuestionResults().entrySet()) {
                    JSONObject questionStats = new JSONObject().put("question", entry.getKey());
                    JSONArray values = new JSONArray();
                    for (java.util.Map.Entry<String, Integer> value : entry.getValue().entrySet()) {
                        values.put(new JSONObject().put("label", value.getKey())
                                .put("count", value.getValue()));
                    }
                    questionStats.put("values", values);
                    stats.put(questionStats);
                }
                item.put("statistics", stats);
                result.put(item);
            }
        } catch (Exception ignored) { }
        return result.toString();
    }

    public static List<Survey> decodeSurveys(String raw) {
        java.util.ArrayList<Survey> result = new java.util.ArrayList<>();
        if (raw == null || raw.isEmpty()) return result;
        try {
            JSONArray records = new JSONArray(raw);
            for (int i = 0; i < records.length(); i++) {
                JSONObject item = records.optJSONObject(i);
                if (item == null) continue;
                Survey survey = new Survey(item.optString("id", ""),
                        item.optString("title", "Encuesta"),
                        item.optString("category", "General"),
                        item.optString("date", ""),
                        item.optString("status", "Vigente"),
                        item.optString("link", ""));
                JSONArray questions = item.optJSONArray("questions");
                java.util.ArrayList<GoogleFormsApi.SurveyQuestion> decodedQuestions = new java.util.ArrayList<>();
                if (questions != null) {
                    for (int q = 0; q < questions.length(); q++) {
                        JSONObject question = questions.optJSONObject(q);
                        if (question == null) continue;
                        decodedQuestions.add(new GoogleFormsApi.SurveyQuestion(
                                question.optString("title", ""),
                                question.optString("answerType", "SHORT"),
                                question.optString("choices", "")));
                    }
                }
                survey.update(survey.getTitle(), survey.getCategory(), survey.getStatus(), decodedQuestions);
                JSONArray stats = item.optJSONArray("statistics");
                java.util.Map<String, java.util.Map<String, Integer>> decodedStats =
                        new java.util.LinkedHashMap<>();
                if (stats != null) for (int s = 0; s < stats.length(); s++) {
                    JSONObject questionStats = stats.optJSONObject(s);
                    if (questionStats == null) continue;
                    java.util.Map<String, Integer> values = new java.util.LinkedHashMap<>();
                    JSONArray rows = questionStats.optJSONArray("values");
                    if (rows != null) for (int v = 0; v < rows.length(); v++) {
                        JSONObject value = rows.optJSONObject(v);
                        if (value != null) values.put(value.optString("label", ""),
                                value.optInt("count", 0));
                    }
                    decodedStats.put(questionStats.optString("question", ""), values);
                }
                survey.setGeneratedStatistics(item.optInt("responseCount", 0), decodedStats);
                result.add(survey);
            }
        } catch (Exception ignored) { }
        return result;
    }

    public static void writePdf(Context context, OutputStream output, String reportTitle,
                                List<Survey> surveys) throws Exception {
        PdfDocument document = new PdfDocument();
        String[] colors = PaletteManager.getActivePalette(context);
        int background = PaletteManager.color(colors[0], Color.WHITE);
        int principal = PaletteManager.color(colors[1], 0xFF142A43);
        int bodyText = PaletteManager.color(colors[2], 0xFF526579);
        int accent = PaletteManager.color(colors[3], 0xFF1684C7);
        PdfPage page = new PdfPage(document, background, principal, bodyText, accent);
        try {
            page.cover(reportTitle, surveys.size());
            for (int i = 0; i < surveys.size(); i++) {
                Survey survey = surveys.get(i);
                page.surveyHeader(i + 1, survey);
                if (survey.getQrCodeString() != null && !survey.getQrCodeString().isEmpty()) {
                    page.paragraph("Enlace: " + survey.getQrCodeString(), 9, false, page.muted());
                }
                if (!survey.isGoogleForm() && survey.getResponseCount() > 0) {
                    page.metric("RESPUESTAS DE MUESTRA", survey.getResponseCount());
                }
                page.sectionTitle("Preguntas y resultados");
                if (survey.getQuestions().isEmpty()) {
                    page.paragraph("No hay preguntas guardadas para esta encuesta.", 10, false,
                            page.muted());
                } else {
                    for (int q = 0; q < survey.getQuestions().size(); q++) {
                        GoogleFormsApi.SurveyQuestion question = survey.getQuestions().get(q);
                        page.question(q + 1, question.title,
                                "MULTIPLE_CHOICE".equals(question.answerType)
                            ? "Opción múltiple" : "Respuesta abierta");
                        if (!question.choices.isEmpty()) {
                            page.paragraph("Opciones: " + question.choices, 9, false,
                                    page.muted());
                        }
                        java.util.Map<String, Integer> results = survey.getQuestionResults(question.title);
                        int total = 0;
                        for (Integer count : results.values()) total += count == null ? 0 : count;
                        for (java.util.Map.Entry<String, Integer> result : results.entrySet()) {
                            int count = result.getValue() == null ? 0 : result.getValue();
                            page.resultBar(result.getKey(), count,
                                    total == 0 ? 0 : Math.round(count * 100f / total), total);
                        }
                    }
                }
                page.gap(14);
            }
            page.finish();
            document.writeTo(output);
        } finally {
            document.close();
        }
    }

    public static void writeExcel(OutputStream output, String reportTitle,
                                  List<Survey> surveys) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            addEntry(zip, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                            + "</Types>");
            addEntry(zip, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                            + "</Relationships>");
            addEntry(zip, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
                            + "<sheets><sheet name=\"Reporte\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            addEntry(zip, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
                            + "</Relationships>");
            addEntry(zip, "xl/worksheets/sheet1.xml", makeWorksheet(reportTitle, surveys));
            zip.finish();
        }
    }

    private static String makeWorksheet(String reportTitle, List<Survey> surveys) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        appendRow(xml, 1, reportTitle, "", "", "", "", "", "", "");
        appendRow(xml, 2, "Generado: " + timestamp(), "", "", "", "", "", "", "");
        appendRow(xml, 3, "Encuestas incluidas: " + surveys.size(), "", "", "", "", "", "", "");
        appendRow(xml, 4, "Encuesta", "Categoría", "Fecha de creación", "Estado",
                "Enlace", "Respuestas de muestra", "Pregunta", "Tipo de respuesta",
                "Opciones", "Resultado", "Conteo", "Porcentaje");
        int row = 5;
        for (Survey survey : surveys) {
            String responseCount = survey.isGoogleForm() ? "" : String.valueOf(survey.getResponseCount());
            if (survey.getQuestions().isEmpty()) {
                appendRow(xml, row++, survey.getTitle(), survey.getCategory(), survey.getCreationDate(),
                        survey.getStatus(), survey.getQrCodeString(), responseCount, "", "", "", "", "", "");
            } else {
                for (GoogleFormsApi.SurveyQuestion question : survey.getQuestions()) {
                    java.util.Map<String, Integer> results = survey.getQuestionResults(question.title);
                    int total = 0;
                    for (Integer count : results.values()) total += count == null ? 0 : count;
                    if (results.isEmpty()) {
                        appendRow(xml, row++, survey.getTitle(), survey.getCategory(),
                                survey.getCreationDate(), survey.getStatus(), survey.getQrCodeString(),
                                responseCount, question.title,
                                "MULTIPLE_CHOICE".equals(question.answerType)
                                        ? "Opción múltiple" : "Texto abierto", question.choices,
                                "", "", "");
                    } else {
                        for (java.util.Map.Entry<String, Integer> result : results.entrySet()) {
                            int count = result.getValue() == null ? 0 : result.getValue();
                            appendRow(xml, row++, survey.getTitle(), survey.getCategory(),
                                    survey.getCreationDate(), survey.getStatus(), survey.getQrCodeString(),
                                    responseCount, question.title,
                                    "MULTIPLE_CHOICE".equals(question.answerType)
                                            ? "Opción múltiple" : "Texto abierto", question.choices,
                                    result.getKey(), String.valueOf(count),
                                    total == 0 ? "0%" : Math.round(count * 100f / total) + "%");
                        }
                    }
                }
            }
        }
        return xml.append("</sheetData></worksheet>").toString();
    }

    private static void appendRow(StringBuilder xml, int row, String... values) {
        xml.append("<row r=\"").append(row).append("\">");
        for (int i = 0; i < values.length; i++) {
            xml.append("<c r=\"").append(columnName(i + 1)).append(row)
                    .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(escapeXml(values[i])).append("</t></is></c>");
        }
        xml.append("</row>");
    }

    private static String columnName(int value) {
        StringBuilder name = new StringBuilder();
        while (value > 0) {
            int remainder = (value - 1) % 26;
            name.insert(0, (char) ('A' + remainder));
            value = (value - 1) / 26;
        }
        return name.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void addEntry(ZipOutputStream zip, String path, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String timestamp() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
    }

    private static final class PdfPage {
        private final PdfDocument document;
        private PdfDocument.Page page;
        private Canvas canvas;
        private float y;
        private int pageNumber;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int background;
        private final int principal;
        private final int bodyText;
        private final int accent;
        private final int surface;
        private final int softAccent;
        private final int headerText;
        private final int mutedText;

        PdfPage(PdfDocument document, int background, int principal,
                int bodyText, int accent) {
            this.document = document;
            this.background = background;
            this.principal = principal;
            this.bodyText = bodyText;
            this.accent = accent;
            this.surface = blend(background, bodyText, isLight(background) ? 0.045f : 0.12f);
            this.softAccent = blend(surface, accent, 0.14f);
            this.headerText = readableTextColor(principal);
            this.mutedText = blend(bodyText, background, 0.2f);
            startPage();
        }

        int muted() { return mutedText; }

        void cover(String title, int count) {
            paint.setColor(principal);
            canvas.drawRoundRect(26, 26, 569, 166, 24, 24, paint);
            paint.setColor(accent);
            canvas.drawRoundRect(26, 26, 40, 166, 7, 7, paint);
            paragraphAt("APP ENCUESTAS  /  INFORME DE RESULTADOS", 10, true,
                    blend(headerText, principal, 0.3f), 56, 56, 480);
            paragraphAt(title, 24, true, headerText, 56, 93, 478);
            paragraphAt("Generado " + timestamp(), 10, false,
                    blend(headerText, principal, 0.25f), 56, 141, 320);
            paint.setColor(blend(principal, accent, 0.32f));
            canvas.drawRoundRect(420, 128, 544, 153, 13, 13, paint);
            paragraphAt(count + (count == 1 ? " encuesta" : " encuestas"),
                    10, true, readableTextColor(blend(principal, accent, 0.32f)),
                    432, 145, 105);
            y = 192;
        }

        void surveyHeader(int number, Survey survey) {
            ensureSpace(100);
            float top = y;
            paint.setColor(surface);
            canvas.drawRoundRect(28, top, 567, top + 84, 14, 14, paint);
            paint.setColor(accent);
            canvas.drawRoundRect(28, top, 35, top + 84, 3, 3, paint);
            paragraphAt(number + "  ·  " + clip(survey.getTitle(), 58), 16, true,
                    principal, 49, top + 24, 495);
            paragraphAt(clip(survey.getCategory() + "   /   " + survey.getStatus()
                            + "   /   " + survey.getCreationDate(), 74),
                    10, false, mutedText, 49, top + 51, 495);
            y = top + 100;
        }

        void metric(String label, int value) {
            ensureSpace(48);
            paint.setColor(softAccent);
            canvas.drawRoundRect(34, y - 4, 561, y + 30, 10, 10, paint);
            paragraphAt(label + "  ·  " + value, 11, true, principal, 48, y + 17, 490);
            y += 43;
        }

        void sectionTitle(String title) {
            ensureSpace(34);
            paragraphAt(title.toUpperCase(Locale.ROOT), 11, true,
                    accent, 40, y + 10, 500);
            paint.setColor(blend(surface, bodyText, 0.22f));
            canvas.drawRect(40, y + 16, 555, y + 17, paint);
            y += 27;
        }

        void question(int number, String title, String type) {
            ensureSpace(68);
            paragraphAt("PREGUNTA " + number + "   ·   " + type.toUpperCase(Locale.ROOT),
                    9, true, mutedText, 42, y + 10, 500);
            y += 25;
            paragraph(title, 11, true, principal);
            y += 8;
        }

        void resultBar(String label, int count, int percent, int total) {
            ensureSpace(41);
            paint.setColor(surface);
            canvas.drawRoundRect(38, y - 3, 557, y + 34, 9, 9, paint);
            paragraphAt(clip(label, 68), 9, false, mutedText, 49, y + 10, 410);
            paragraphAt(count + "  ·  " + percent + "%", 9, true, principal,
                    470, y + 10, 75);
            paint.setColor(blend(surface, bodyText, 0.13f));
            canvas.drawRoundRect(49, y + 18, 545, y + 24, 3, 3, paint);
            paint.setColor(accent);
            float fraction = total == 0 ? 0 : Math.max(0, Math.min(1, count / (float) total));
            canvas.drawRoundRect(49, y + 18, 49 + 496 * fraction, y + 24, 3, 3, paint);
            y += 42;
        }

        void paragraph(String value, float size, boolean bold, int color) {
            paint.setTextSize(size);
            paint.setTypeface(bold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
            paint.setColor(color);
            drawWrapped(value, size, 48, 500);
        }

        private void paragraphAt(String value, float size, boolean bold, int color,
                                 float left, float baseline, float maxWidth) {
            paint.setTextSize(size);
            paint.setTypeface(bold ? Typeface.create(Typeface.DEFAULT, Typeface.BOLD) : Typeface.DEFAULT);
            paint.setColor(color);
            String[] words = (value == null ? "" : value).split("\\s+");
            StringBuilder line = new StringBuilder();
            float lineY = baseline;
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && paint.measureText(candidate) > maxWidth) {
                    canvas.drawText(line.toString(), left, lineY, paint);
                    line.setLength(0);
                    line.append(word);
                    lineY += size + 3;
                } else {
                    if (line.length() > 0) line.append(' ');
                    line.append(word);
                }
            }
            if (line.length() > 0) canvas.drawText(line.toString(), left, lineY, paint);
        }

        private String clip(String value, int maxLength) {
            if (value == null) return "";
            String cleaned = value.replaceAll("\\s+", " ").trim();
            return cleaned.length() <= maxLength
                    ? cleaned : cleaned.substring(0, Math.max(0, maxLength - 1)) + "…";
        }

        private void drawWrapped(String value, float size, float left, float maxWidth) {
            String[] words = (value == null ? "" : value).split("\\s+");
            StringBuilder line = new StringBuilder();
            float lineHeight = size + 6;
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && paint.measureText(candidate) > maxWidth) {
                    drawLine(line.toString(), lineHeight, left);
                    line.setLength(0);
                    line.append(word);
                } else {
                    if (line.length() > 0) line.append(' ');
                    line.append(word);
                }
            }
            if (line.length() > 0) drawLine(line.toString(), lineHeight, left);
            y += 3;
        }

        void gap(float amount) { y += amount; }

        void finish() {
            if (page != null) {
                footer();
                document.finishPage(page);
                page = null;
            }
        }

        private void drawLine(String text, float lineHeight, float left) {
            ensureSpace(lineHeight);
            canvas.drawText(text, left, y, paint);
            y += lineHeight;
        }

        private void ensureSpace(float amount) {
            if (y + amount <= 780) return;
            int oldColor = paint.getColor();
            float oldSize = paint.getTextSize();
            Typeface oldTypeface = paint.getTypeface();
            startPage();
            paint.setColor(oldColor);
            paint.setTextSize(oldSize);
            paint.setTypeface(oldTypeface);
        }

        private void startPage() {
            if (page != null) {
                footer();
                document.finishPage(page);
            }
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create();
            page = document.startPage(info);
            canvas = page.getCanvas();
            canvas.drawColor(background);
            paint.setColor(accent);
            canvas.drawRect(0, 0, 595, 6, paint);
            if (pageNumber > 1) {
                paragraphAt("APP ENCUESTAS  ·  CONTINUACIÓN", 9, true,
                        mutedText, 36, 31, 520);
            }
            y = pageNumber == 1 ? 50 : 58;
        }

        private void footer() {
            paint.setColor(mutedText);
            paint.setTextSize(8);
            paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText("App Encuestas  ·  Reporte histórico", 36, 821, paint);
            canvas.drawText("Página " + pageNumber, 512, 821, paint);
        }

        private static int blend(int base, int overlay, float amount) {
            float inverse = 1f - amount;
            return Color.rgb(Math.round(Color.red(base) * inverse + Color.red(overlay) * amount),
                    Math.round(Color.green(base) * inverse + Color.green(overlay) * amount),
                    Math.round(Color.blue(base) * inverse + Color.blue(overlay) * amount));
        }

        private static int readableTextColor(int color) {
            double red = linear(Color.red(color) / 255.0);
            double green = linear(Color.green(color) / 255.0);
            double blue = linear(Color.blue(color) / 255.0);
            double luminance = 0.2126 * red + 0.7152 * green + 0.0722 * blue;
            return luminance > 0.179 ? Color.BLACK : Color.WHITE;
        }

        private static boolean isLight(int color) {
            return readableTextColor(color) == Color.BLACK;
        }

        private static double linear(double channel) {
            return channel <= 0.04045 ? channel / 12.92
                    : Math.pow((channel + 0.055) / 1.055, 2.4);
        }
    }
}
