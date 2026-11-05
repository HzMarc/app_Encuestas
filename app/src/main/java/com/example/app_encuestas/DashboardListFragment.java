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
    private DashboardSurveyAdapter adapter;

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
        int tab = getArguments() == null ? 0 : getArguments().getInt(ARG_TAB, 0);
        adapter = new DashboardSurveyAdapter(tab == 0
                ? createMockSurveys() : new ArrayList<>());
        recycler.setAdapter(adapter);
        if (tab == 1) loadRecentLogins();
    }

    @Override public void onResume() {
        super.onResume();
        if (getArguments() != null && getArguments().getInt(ARG_TAB, 0) == 1) {
            loadRecentLogins();
        }
    }

    private void loadRecentLogins() {
        if (adapter == null || !isAdded()) return;
        LoginHistoryStore.loadRecent(requireActivity(), records -> {
            if (!isAdded() || adapter == null) return;
            List<DashboardSurvey> items = new ArrayList<>();
            for (LoginHistoryStore.UserRecord record : records) {
                items.add(new DashboardSurvey(record.name,
                        record.email + " · " + record.formattedDate()));
            }
            if (items.isEmpty()) {
                items.add(new DashboardSurvey("Aún no hay inicios de sesión",
                        "Los accesos recientes aparecerán aquí."));
            }
            adapter.replaceItems(items);
        });
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
