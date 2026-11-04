package com.example.app_encuestas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DashboardListFragment extends Fragment {
    private static final String ARG_TAB = "tab";

    public static DashboardListFragment newInstance(int tab) {
        DashboardListFragment fragment = new DashboardListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB, tab);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView recycler = view.findViewById(R.id.dashboardRecycler);
        recycler.setNestedScrollingEnabled(false);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(new DashboardSurveyAdapter(createMockSurveys()));
    }

    private List<DashboardSurvey> createMockSurveys() {
        int tab = getArguments() == null ? 0 : getArguments().getInt(ARG_TAB, 0);
        List<DashboardSurvey> surveys = new ArrayList<>();
        if (tab == 0) {
            surveys.add(new DashboardSurvey("Servicios municipales", "Vigente · 120 respuestas"));
            surveys.add(new DashboardSurvey("Seguridad ciudadana", "Vigente · 85 respuestas"));
            surveys.add(new DashboardSurvey("Transporte", "Vigente · 40 respuestas"));
        } else {
            surveys.add(new DashboardSurvey("Satisfacción ciudadana", "Último inicio · Hoy"));
            surveys.add(new DashboardSurvey("Atención al usuario", "Último inicio · Ayer"));
            surveys.add(new DashboardSurvey("Espacios públicos", "Último inicio · 02/11/2026"));
        }
        return surveys;
    }
}
