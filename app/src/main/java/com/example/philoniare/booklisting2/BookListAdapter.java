package com.example.philoniare.booklisting2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

public class BookListAdapter extends ArrayAdapter<BookItem> {
    private Context context;
    public BookListAdapter(Context context, int resource, List<BookItem> books) {
        super(context, resource, books);
        this.context = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(getContext());
            view = layoutInflater.inflate(R.layout.book_item, null);
        }

        BookItem book = getItem(position);

        if (book != null) {
            ImageView thumbnail = (ImageView) view.findViewById(R.id.book_thumbnail);
            TextView title = (TextView) view.findViewById(R.id.book_title);
            title.setText(book.getTitle());
            Glide.with(context).load(book.getThumbnail()).into(thumbnail);
        }
        return view;
    }
}
