package com.edillower.heymavic;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Place picker for fly to a specific location
 * @author Eddie Wang
 */
public class PlaceListFragment extends ListFragment {

    private String TAG = PlaceListFragment.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedInstanceState = getArguments();
        this.setListAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, (String[]) savedInstanceState.get("places"))
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_place_list, container, false);
        return view;
    }

    @Override
    public void onListItemClick(ListView list, View v, int position, long id) {
        ((FPVFullscreenActivity) getActivity()).getPlaceCoordinates(position);
    }
}
