package com.example.app_encuestas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CategoryFolderAdapter extends RecyclerView.Adapter<CategoryFolderAdapter.Holder> {
    public interface OnCategoryClick {
        void onClick(String category);
    }

    private final List<CategoryFolder> categories;
    private final OnCategoryClick onClick;

    public CategoryFolderAdapter(List<CategoryFolder> categories, OnCategoryClick onClick) {
        this.categories = categories;
        this.onClick = onClick;
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_category, parent, false);
        return new Holder(view);
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        if (holder.itemView.getContext() instanceof android.app.Activity) {
            PaletteTheme.applyToView(holder.itemView,
                    (android.app.Activity) holder.itemView.getContext());
        }
        CategoryFolder category = categories.get(position);
        holder.name.setText(category.name);
        holder.count.setText(category.count == 1
                ? "1 encuesta" : category.count + " encuestas");
        holder.itemView.setOnClickListener(view -> onClick.onClick(category.name));
    }

    @Override public int getItemCount() { return categories.size(); }

    static final class CategoryFolder {
        final String name;
        final int count;
        CategoryFolder(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView count;
        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.historyCategoryName);
            count = itemView.findViewById(R.id.historyCategoryCount);
        }
    }
}
