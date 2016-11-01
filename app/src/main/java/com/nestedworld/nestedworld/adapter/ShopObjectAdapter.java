package com.nestedworld.nestedworld.adapter;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nestedworld.nestedworld.R;
import com.nestedworld.nestedworld.network.http.models.response.object.ShopObjectsResponse;


public class ShopObjectAdapter extends ArrayAdapter<ShopObjectsResponse.ShopObject> {

    @LayoutRes
    private final static int layoutRes = R.layout.item_shop_object;

    /*
    ** Constructor
     */
    public ShopObjectAdapter(@NonNull final Context context) {
        super(context, layoutRes);
    }

    /*
    ** Life cycle
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;

        //Check if an existing view is being reused, otherwise inflate the view
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(layoutRes, parent, false);
        }

        //Get current object
        ShopObjectsResponse.ShopObject currentObject = getItem(position);
        if (currentObject == null) {
            return view;
        }

        populateView(view, currentObject);

        return view;
    }

    /*
    ** Internal method
     */
    private void populateView(@NonNull final View view, @NonNull final ShopObjectsResponse.ShopObject shopObject) {
        //Retrieve widget
        TextView textViewObjectName = (TextView) view.findViewById(R.id.textview_object_name);
        TextView textViewObjectKind = (TextView) view.findViewById(R.id.textview_object_kind);
        TextView textViewObjectPower = (TextView) view.findViewById(R.id.textview_object_power);
        TextView textViewObjectPrenium = (TextView) view.findViewById(R.id.textview_object_prenium);
        TextView textViewObjectDescription = (TextView) view.findViewById(R.id.textview_object_description);
        TextView textViewObjectPrice = (TextView) view.findViewById(R.id.textview_object_price);
        ImageView imageViewObject = (ImageView) view.findViewById(R.id.imageView_object);

        //Populate widget
        textViewObjectName.setText("Name: " + shopObject.name);
        textViewObjectKind.setText("Kind: " + shopObject.kind);
        textViewObjectPower.setText("Power: " + shopObject.power);
        textViewObjectPrenium.setText("Is prenium : " + (shopObject.premium ? "yes" : "no"));
        textViewObjectDescription.setText("Description : " + shopObject.description);
        textViewObjectPrice.setText("Price : " + String.valueOf(shopObject.price));
        Glide.with(getContext()).load(shopObject.image).into(imageViewObject);
    }
}