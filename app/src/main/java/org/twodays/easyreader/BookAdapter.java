package org.twodays.easyreader;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.ViewHolder> {

    private List<Book> books;
    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnSelectionChangedListener selectionListener;
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    private boolean inActionMode = false;

    public interface OnItemClickListener {
        void onItemClick(Book book, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(Book book, int position);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public BookAdapter(List<Book> books, OnItemClickListener listener, OnItemLongClickListener longClickListener) {
        this.books = books;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Book book = books.get(position);
        holder.tvName.setText(book.getName());

        // 根据选中状态设置背景
        if (inActionMode && selectedItems.get(position, false)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.selected_item_bg));
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (inActionMode) {
                toggleSelection(position);
            } else {
                if (listener != null) {
                    listener.onItemClick(book, position);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                return longClickListener.onItemLongClick(book, position);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return books.size();
    }

    public void toggleSelection(int position) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position);
        } else {
            selectedItems.put(position, true);
        }
        notifyItemChanged(position);
        if (selectionListener != null) {
            selectionListener.onSelectionChanged(selectedItems.size());
        }
    }

    public void setInActionMode(boolean inActionMode) {
        this.inActionMode = inActionMode;
        if (!inActionMode) {
            selectedItems.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isInActionMode() {
        return inActionMode;
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public SparseBooleanArray getSelectedItems() {
        return selectedItems;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_book_name);
        }
    }
}