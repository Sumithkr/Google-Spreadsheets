package com.example.googlespreadsheets;

import android.app.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ListviewAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private List<String> fileInfo;

    public ListviewAdapter(Activity context, List<String> fileInfo) {
        super(context, R.layout.listview_design, fileInfo);
        // TODO Auto-generated constructor stub

        this.context=context;
        this.fileInfo=fileInfo;

    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView= inflater.inflate(R.layout.listview_design, null,true);

        TextView titleText = rowView.findViewById(R.id.textView);

        titleText.setText(fileInfo.get(position));

        return rowView;

    };
}
