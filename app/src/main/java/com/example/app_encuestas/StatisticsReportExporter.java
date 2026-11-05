package com.example.app_encuestas;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class StatisticsReportExporter {
    private StatisticsReportExporter() { }

    public static final class Report {
        public String category = "Todas las categorías";
        public String survey = "Todas las encuestas";
        public String question = "Resumen de preguntas";
        public String summary = "";
        public String legend = "Respuestas";
        public int totalResponses = -1;
        public final List<String> labels = new ArrayList<>();
        public final List<Integer> values = new ArrayList<>();
        public final Map<String, Integer> dates = new LinkedHashMap<>();

        public int total() {
            if (totalResponses >= 0) return totalResponses;
            int total = 0;
            for (Integer value : values) if (value != null) total += value;
            return total;
        }
    }

    public static String encode(Report report) {
        JSONObject data = new JSONObject();
        try {
            data.put("category", report.category).put("survey", report.survey)
                    .put("question", report.question).put("summary", report.summary)
                    .put("legend", report.legend).put("totalResponses", report.totalResponses);
            JSONArray metrics = new JSONArray();
            for (int i = 0; i < Math.min(report.labels.size(), report.values.size()); i++) {
                metrics.put(new JSONObject().put("label", report.labels.get(i))
                        .put("value", report.values.get(i)));
            }
            data.put("metrics", metrics);
            JSONArray dates = new JSONArray();
            for (Map.Entry<String, Integer> entry : report.dates.entrySet()) {
                dates.put(new JSONObject().put("date", entry.getKey())
                        .put("value", entry.getValue()));
            }
            data.put("dates", dates);
        } catch (Exception ignored) { }
        return data.toString();
    }

    public static Report decode(String raw) {
        Report report = new Report();
        if (raw == null || raw.trim().isEmpty()) return report;
        try {
            JSONObject data = new JSONObject(raw);
            report.category = data.optString("category", report.category);
            report.survey = data.optString("survey", report.survey);
            report.question = data.optString("question", report.question);
            report.summary = data.optString("summary", report.summary);
            report.legend = data.optString("legend", report.legend);
            report.totalResponses = data.optInt("totalResponses", -1);
            JSONArray metrics = data.optJSONArray("metrics");
            if (metrics != null) for (int i = 0; i < metrics.length(); i++) {
                JSONObject item = metrics.optJSONObject(i);
                if (item == null) continue;
                report.labels.add(item.optString("label", ""));
                report.values.add(item.optInt("value", 0));
            }
            JSONArray dates = data.optJSONArray("dates");
            if (dates != null) for (int i = 0; i < dates.length(); i++) {
                JSONObject item = dates.optJSONObject(i);
                if (item != null) report.dates.put(item.optString("date", ""),
                        item.optInt("value", 0));
            }
        } catch (Exception ignored) { }
        return report;
    }

    public static void writePdf(OutputStream output, String title, Report report) throws Exception {
        PdfDocument document = new PdfDocument();
        try {
            int pageNumber = 1;
            String filters = report.category + " · " + report.survey;
            PdfDocument.Page page = startPdfPage(document, pageNumber, title, filters, false);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0xFFFFFFFF);
            canvas.drawRoundRect(26, 204, 569, 304, 18, 18, paint);
            paint.setColor(0xFF142A43);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(12);
            canvas.drawText("RESUMEN APLICADO", 46, 233, paint);
            paint.setTextSize(27);
            paint.setColor(0xFF1673B8);
            canvas.drawText(String.valueOf(report.total()), 46, 275, paint);
            paint.setTypeface(Typeface.DEFAULT);
            paint.setTextSize(11);
            paint.setColor(0xFF526579);
            canvas.drawText("respuestas en los datos mostrados", 96, 274, paint);
            canvas.drawText("Pregunta: " + clip(report.question, 67), 46, 293, paint);

            float y = 332;
            paint.setColor(0xFF142A43);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(17);
            canvas.drawText(report.legend, 34, y, paint);
            y += 20;
            paint.setTypeface(Typeface.DEFAULT);
            int max = max(report.values);
            for (int i = 0; i < Math.min(report.labels.size(), report.values.size()); i++) {
                if (y + 42 > 785) {
                    drawPdfFooter(canvas, pageNumber);
                    document.finishPage(page);
                    page = startPdfPage(document, ++pageNumber, title, filters, true);
                    canvas = page.getCanvas();
                    y = 78;
                    paint.setColor(0xFF142A43);
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    paint.setTextSize(17);
                    canvas.drawText(report.legend + " · continuación", 34, y, paint);
                    y += 20;
                    paint.setTypeface(Typeface.DEFAULT);
                }
                paint.setColor(0xFFFFFFFF);
                canvas.drawRoundRect(26, y, 569, y + 36, 10, 10, paint);
                paint.setColor(0xFF526579);
                paint.setTextSize(10);
                canvas.drawText(clip(report.labels.get(i), 64), 38, y + 14, paint);
                paint.setColor(0xFFE7EEF5);
                canvas.drawRoundRect(38, y + 22, 492, y + 28, 3, 3, paint);
                paint.setColor(0xFF57A4DB);
                float fraction = max == 0 ? 0 : report.values.get(i) / (float) max;
                canvas.drawRoundRect(38, y + 22, 38 + 454 * fraction, y + 28, 3, 3, paint);
                paint.setColor(0xFF142A43);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(11);
                canvas.drawText(String.valueOf(report.values.get(i)), 512, y + 25, paint);
                paint.setTypeface(Typeface.DEFAULT);
                y += 43;
            }
            if (!report.dates.isEmpty()) {
                y += 14;
                if (y + 42 > 785) {
                    drawPdfFooter(canvas, pageNumber);
                    document.finishPage(page);
                    page = startPdfPage(document, ++pageNumber, title, filters, true);
                    canvas = page.getCanvas();
                    y = 78;
                }
                paint.setColor(0xFF142A43);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                paint.setTextSize(16);
                canvas.drawText("Participación por fecha", 34, y, paint);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(10);
                paint.setColor(0xFF526579);
                y += 22;
                for (Map.Entry<String, Integer> entry : report.dates.entrySet()) {
                    if (y + 30 > 785) {
                        drawPdfFooter(canvas, pageNumber);
                        document.finishPage(page);
                        page = startPdfPage(document, ++pageNumber, title, filters, true);
                        canvas = page.getCanvas();
                        paint.setColor(0xFF142A43);
                        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                        paint.setTextSize(16);
                        canvas.drawText("Participación por fecha · continuación", 34, 72, paint);
                        paint.setTypeface(Typeface.DEFAULT);
                        paint.setTextSize(10);
                        paint.setColor(0xFF526579);
                        y = 92;
                    }
                    paint.setColor(0xFFFFFFFF);
                    canvas.drawRoundRect(30, y - 12, 565, y + 10, 7, 7, paint);
                    paint.setColor(0xFF526579);
                    canvas.drawText(entry.getKey(), 40, y, paint);
                    paint.setColor(0xFF142A43);
                    canvas.drawText(String.valueOf(entry.getValue()) + " respuestas", 450, y, paint);
                    y += 17;
                }
            }
            drawPdfFooter(canvas, pageNumber);
            document.finishPage(page);
            document.writeTo(output);
        } finally {
            document.close();
        }
    }

    private static PdfDocument.Page startPdfPage(PdfDocument document, int pageNumber,
                                                  String title, String filters,
                                                  boolean continuation) {
        PdfDocument.Page page = document.startPage(
                new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create());
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(0xFFF4F7FB);
        paint.setColor(0xFF142A43);
        if (!continuation) {
            canvas.drawRoundRect(26, 26, 569, 188, 24, 24, paint);
            paint.setColor(0xFF57A4DB);
            canvas.drawRoundRect(26, 26, 40, 188, 7, 7, paint);
            paint.setColor(0xFFB8D9F1);
            paint.setTextSize(10);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("APP ENCUESTAS  /  INFORME DE ANÁLISIS", 58, 59, paint);
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(26);
            canvas.drawText(clip(title, 31), 58, 101, paint);
            paint.setTextSize(12);
            paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText("Generado el " + new SimpleDateFormat("dd/MM/yyyy HH:mm",
                    Locale.getDefault()).format(new Date()), 58, 128, paint);
            canvas.drawText(clip("Filtros · " + filters, 72), 58, 157, paint);
        } else {
            canvas.drawRect(0, 0, 595, 48, paint);
            paint.setColor(0xFFFFFFFF);
            paint.setTextSize(13);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("APP ENCUESTAS  ·  " + clip(title, 40), 36, 30, paint);
        }
        return page;
    }

    private static void drawPdfFooter(Canvas canvas, int pageNumber) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFF8798A9);
        paint.setTextSize(9);
        canvas.drawText("Filtros seleccionados en Estadísticas", 34, 824, paint);
        canvas.drawText("Página " + pageNumber, 512, 824, paint);
    }

    public static void writeExcel(OutputStream output, String title, Report report) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            add(zip, "[Content_Types].xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                    + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                    + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                    + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
                    + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
                    + "</Types>");
            add(zip, "_rels/.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
            add(zip, "xl/workbook.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets><sheet name=\"Estadísticas\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>");
            add(zip, "xl/_rels/workbook.xml.rels", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>");
            StringBuilder sheet = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
            int row = 1;
            row = row(sheet, row, title, "", "");
            row = row(sheet, row, "Categoría", report.category, "");
            row = row(sheet, row, "Encuesta", report.survey, "");
            row = row(sheet, row, "Pregunta", report.question, "");
            row = row(sheet, row, "Resumen", report.summary, "");
            row = row(sheet, row, "Métrica", "Valor", "Grupo");
            for (int i = 0; i < Math.min(report.labels.size(), report.values.size()); i++) {
                row = row(sheet, row, report.labels.get(i), String.valueOf(report.values.get(i)), report.legend);
            }
            row = row(sheet, row, "Fecha", "Respuestas", "Serie temporal");
            for (Map.Entry<String, Integer> entry : report.dates.entrySet()) {
                row = row(sheet, row, entry.getKey(), String.valueOf(entry.getValue()), "Participación");
            }
            add(zip, "xl/worksheets/sheet1.xml", sheet.append("</sheetData></worksheet>").toString());
            zip.finish();
        }
    }

    private static int row(StringBuilder xml, int row, String... values) {
        xml.append("<row r=\"").append(row).append("\">");
        for (int column = 0; column < values.length; column++) {
            String cell = column == 0 ? "A" : column == 1 ? "B" : "C";
            xml.append("<c r=\"").append(cell).append(row)
                    .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(escape(values[column])).append("</t></is></c>");
        }
        return row + 1;
    }

    private static void add(ZipOutputStream zip, String path, String content) throws Exception {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static int max(List<Integer> values) {
        int max = 0;
        for (Integer value : values) if (value != null && value > max) max = value;
        return max;
    }

    private static String clip(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
