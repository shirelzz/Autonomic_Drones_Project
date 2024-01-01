package com.dji.sdk.sample.demo.accurateLandingController;

import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.dji.sdk.sample.R;

import java.util.Objects;

public class FragmentMap extends Fragment {

    private PresentMap presentMap;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Instantiate PresentMap class within YourFragment
        presentMap = new PresentMap(savedInstanceState, getContext(), Objects.requireNonNull(getActivity()));

        // Call methods from PresentMap class as needed
        presentMap.initializeMap(savedInstanceState);
        presentMap.onResume();

        return view;
    }

    // Override other necessary lifecycle methods if needed
    @Override
    public void onResume() {
        super.onResume();
        presentMap.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        presentMap.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presentMap.onDestroy();
    }
}


