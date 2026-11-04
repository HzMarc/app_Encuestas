package com.example.app_encuestas;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PaletteAdapter extends RecyclerView.Adapter<PaletteAdapter.Holder> {
    public interface OnColorClick {
        void onClick(int position);
    }

    private final String[] colors;
    private final OnColorClick listener;

    public PaletteAdapter(String[] colors, OnColorClick listener) {
        this.colors = colors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_palette_color, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.role.setText(PaletteManager.ROLES[position]);
        holder.hex.setText(colors[position].toUpperCase());
        GradientDrawable background = new GradientDrawable();
        background.setColor(PaletteManager.color(colors[position], 0xFFFFFFFF));
        background.setCornerRadius(10);
        holder.swatch.setBackground(background);
        holder.itemView.setOnClickListener(v -> listener.onClick(position));
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View swatch;
        final TextView role;
        final TextView hex;

        Holder(@NonNull View itemView) {
            super(itemView);
            swatch = itemView.findViewById(R.id.colorSwatch);
            role = itemView.findViewById(R.id.colorRole);
            hex = itemView.findViewById(R.id.colorHex);
        }
    }
}
