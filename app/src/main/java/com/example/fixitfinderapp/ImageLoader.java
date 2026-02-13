package com.example.fixitfinderapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

public final class ImageLoader {

    private ImageLoader() {
    }

    public static void load(ImageView view, String uriString, int placeholderResId) {
        if (view == null) {
            return;
        }
        if (TextUtils.isEmpty(uriString)) {
            if (placeholderResId != 0) {
                view.setImageResource(placeholderResId);
            }
            return;
        }
        view.setTag(uriString);
        Uri uri = Uri.parse(uriString);
        String scheme = uri.getScheme();
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            new Thread(() -> {
                try (InputStream input = new URL(uriString).openStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap != null) {
                        view.post(() -> {
                            Object tag = view.getTag();
                            if (uriString.equals(tag)) {
                                view.setImageBitmap(bitmap);
                            }
                        });
                    }
                } catch (Exception ignored) {
                    // Best-effort image load; keep placeholder if any.
                }
            }).start();
        } else {
            view.setImageURI(uri);
        }
    }
}
