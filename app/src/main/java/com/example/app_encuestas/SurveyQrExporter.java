package com.example.app_encuestas;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;

public final class SurveyQrExporter {
    private SurveyQrExporter() { }

    public static void share(Activity activity, Survey survey, Bitmap bitmap) {
        try {
            File directory = new File(activity.getCacheDir(), "qr");
            if (!directory.exists() && !directory.mkdirs()) {
                throw new IllegalStateException("No se pudo preparar el archivo QR.");
            }
            File file = new File(directory, safeName(survey) + ".png");
            try (FileOutputStream output = new FileOutputStream(file)) {
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw new IllegalStateException("No se pudo generar la imagen QR.");
                }
            }
            Uri uri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", file);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("image/png");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.putExtra(Intent.EXTRA_SUBJECT, "Código QR · " + survey.getTitle());
            send.putExtra(Intent.EXTRA_TEXT, survey.getTitle() + "\n" + survey.getQrCodeString());
            send.setClipData(ClipData.newUri(activity.getContentResolver(),
                    "Código QR de " + survey.getTitle(), uri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(send, "Compartir código QR"));
        } catch (Exception error) {
            Toast.makeText(activity, "No se pudo compartir el QR: " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    public static void print(Activity activity, Survey survey, Bitmap bitmap) {
        PrintManager printer = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
        if (printer == null) {
            Toast.makeText(activity, "La impresión no está disponible en este dispositivo.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        printer.print(safeName(survey), new QrPrintAdapter(survey.getTitle(), bitmap), null);
    }

    private static String safeName(Survey survey) {
        String name = (survey.getTitle() == null ? "encuesta" : survey.getTitle())
                .replaceAll("[^A-Za-z0-9_-]", "_");
        return "QR_" + (name.isEmpty() ? survey.getId() : name);
    }

    private static final class QrPrintAdapter extends PrintDocumentAdapter {
        private final String title;
        private final Bitmap bitmap;

        QrPrintAdapter(String title, Bitmap bitmap) {
            this.title = title;
            this.bitmap = bitmap;
        }

        @Override public void onLayout(PrintAttributes oldAttributes,
                                       PrintAttributes newAttributes,
                                       CancellationSignal cancellationSignal,
                                       LayoutResultCallback callback, Bundle extras) {
            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }
            PrintDocumentInfo info = new PrintDocumentInfo.Builder(title + "_QR.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();
            callback.onLayoutFinished(info, true);
        }

        @Override public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                      CancellationSignal cancellationSignal,
                                      WriteResultCallback callback) {
            PdfDocument document = new PdfDocument();
            try {
                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    return;
                }
                PdfDocument.Page page = document.startPage(
                        new PdfDocument.PageInfo.Builder(595, 842, 1).create());
                page.getCanvas().drawColor(Color.WHITE);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                paint.setColor(0xFF142A43);
                paint.setTextSize(24);
                paint.setTypeface(android.graphics.Typeface.create(
                        android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD));
                page.getCanvas().drawText(title, 48, 62, paint);
                paint.setColor(0xFF526579);
                paint.setTextSize(12);
                page.getCanvas().drawText("Escanea para responder la encuesta", 48, 88, paint);
                Rect target = new Rect(90, 140, 505, 555);
                page.getCanvas().drawBitmap(bitmap, null, target, null);
                document.finishPage(page);
                try (FileOutputStream output = new FileOutputStream(destination.getFileDescriptor())) {
                    document.writeTo(output);
                }
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (Exception error) {
                callback.onWriteFailed(error.getMessage());
            } finally {
                document.close();
                try { destination.close(); } catch (Exception ignored) { }
            }
        }
    }
}
