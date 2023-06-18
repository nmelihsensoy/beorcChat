package com.nmelihsensoy.beorc;

// https://developer.android.com/develop/ui/views/layout/recyclerview

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    private MessageData[] localDataSet;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            textView = (TextView) view.findViewById(R.id.chat_text);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView
     */
    public CustomAdapter(MessageData[] dataSet) {
        localDataSet = dataSet;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;

        if(viewType == 1){
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.bubble_left, viewGroup, false);
            return new ViewHolder(view);
        }else if(viewType == 2){
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.bubble_right, viewGroup, false);
            return new ViewHolder(view);
        }

        return null;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        final MessageData data = localDataSet[position];
        viewHolder.getTextView().setText(data.getMessage());
    }

    @Override
    public int getItemViewType(int position) {
        return localDataSet[position].getType();
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localDataSet.length;
    }
}
