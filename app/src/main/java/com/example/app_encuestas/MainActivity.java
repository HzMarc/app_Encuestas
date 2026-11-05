package com.example.app_encuestas;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        setupToolbarMenu();
        ((TextView) findViewById(R.id.tvActiveCount)).setText("12");
        applySystemBarInsets();

        setupTabs();
        setupChart();
        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyActivePalette();
        LoginHistoryStore.loadTodayCount(this, count ->
                ((TextView) findViewById(R.id.tvResponsesCount)).setText(String.valueOf(count)));
    }

    private void applyActivePalette() {
        PaletteTheme.apply(this);
    }

    private void setupToolbarMenu() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(null);
        LogoManager.loadInto(this, findViewById(R.id.toolbarAppLogo));
        toolbar.inflateMenu(R.menu.toolbar_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_users) {
                startActivity(new Intent(this, UsersActivity.class));
                return true;
            }
            if (item.getItemId() == R.id.action_settings) {
                openSettings();
                return true;
            }
            if (item.getItemId() == R.id.action_logout) {
                logout();
                return true;
            }
            return false;
        });
    }

    private void openSettings() {
        Intent intent = new Intent(MainActivity.this, ConfiguracionActivity.class);
        intent.setClass(MainActivity.this, ConfiguracionActivity.class);
        try {
            startActivity(intent);
        } catch (RuntimeException error) {
            Toast.makeText(this, "No se pudo abrir Configuración",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboardRoot),
                (view, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(view.getPaddingLeft(), systemBars.top,
                            view.getPaddingRight(), 0);
                    BottomNavigationView navigation = findViewById(R.id.bottomNavigation);
                    navigation.setPadding(navigation.getPaddingLeft(), navigation.getPaddingTop(),
                            navigation.getPaddingRight(), systemBars.bottom);
                    return insets;
                });
        ViewCompat.requestApplyInsets(findViewById(R.id.dashboardRoot));
    }

    private void setupTabs() {
        TabLayout tabs = findViewById(R.id.dashboardTabs);
        ViewPager2 pager = findViewById(R.id.dashboardPager);
        pager.setAdapter(new DashboardPagerAdapter(this));
        new TabLayoutMediator(tabs, pager,
                (tab, position) -> tab.setText(position == 0 ? "Encuestas vigentes" : "Inicios de sesión"))
                .attach();
    }

    private void setupChart() {
        BarChart chart = findViewById(R.id.quickChart);
        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 120));
        entries.add(new BarEntry(1, 85));
        entries.add(new BarEntry(2, 40));

        BarDataSet dataSet = new BarDataSet(entries, "Respuestas");
        dataSet.setColor(Color.rgb(87, 164, 219));
        dataSet.setDrawValues(false);
        chart.setData(new BarData(dataSet));
        chart.getAxisLeft().setTextColor(Color.rgb(107, 114, 128));
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setTextColor(Color.rgb(107, 114, 128));
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setLabelCount(3);
        chart.getLegend().setEnabled(false);
        Description description = new Description();
        description.setText("");
        chart.setDescription(description);
        chart.setFitBars(true);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.getAxisLeft().setGridColor(Color.LTGRAY);
        chart.getXAxis().setGridColor(Color.TRANSPARENT);
        chart.invalidate();
    }

    private void setupBottomNavigation() {
        BottomNavigationView navigation = findViewById(R.id.bottomNavigation);
        navigation.setSelectedItemId(R.id.nav_home);
        navigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            }
            if (id == R.id.nav_profile) {
                ScreenChrome.navigate(this, new Intent(this, PerfilActivity.class));
                return true;
            }
            if (id == R.id.nav_surveys) {
                ScreenChrome.navigate(this, new Intent(this, SurveyManagementActivity.class));
                return true;
            }
            if (id == R.id.nav_history) {
                ScreenChrome.navigate(this, new Intent(this, SurveyHistoryActivity.class));
                return true;
            }
            if (id == R.id.nav_statistics) {
                ScreenChrome.navigate(this, new Intent(this, StatisticsActivity.class));
                return true;
            }
            Toast.makeText(this, "Sección disponible próximamente", Toast.LENGTH_SHORT).show();
            navigation.setSelectedItemId(R.id.nav_home);
            return false;
        });
    }

    private void logout() {
        UserManager.clearSession(this);
        FirebaseAuth.getInstance().signOut();
        try {
            GoogleSignIn.getClient(
                    this,
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            ).signOut();
        } catch (Exception ignored) {
        }

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
