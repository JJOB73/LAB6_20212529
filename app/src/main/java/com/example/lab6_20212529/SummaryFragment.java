package com.example.lab6_20212529;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.lab6_20212529.model.FuelRecord;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class SummaryFragment extends Fragment {

    private BarChart barChart;
    private PieChart pieChart;
    private TextView emptyView;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private CollectionReference recordsCollection;
    private ListenerRegistration recordsListener;

    private final DateFormat monthLabelFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_summary, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        barChart = view.findViewById(R.id.barChart);
        pieChart = view.findViewById(R.id.pieChart);
        emptyView = view.findViewById(R.id.textEmpty);

        setupBarChart();
        setupPieChart();

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        subscribeToRecords();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recordsListener != null) {
            recordsListener.remove();
            recordsListener = null;
        }
    }

    private void subscribeToRecords() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showMessage(getString(R.string.error_authentication));
            return;
        }

        recordsCollection = firestore.collection("users")
                .document(user.getUid())
                .collection("fuelRecords");

        recordsListener = recordsCollection.addSnapshotListener(recordsSnapshotListener);
    }

    private final EventListener<QuerySnapshot> recordsSnapshotListener = new EventListener<QuerySnapshot>() {
        @Override
        public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
            if (error != null) {
                showMessage(getString(R.string.error_loading_records));
                return;
            }

            if (value == null || value.isEmpty()) {
                showEmptyState(true);
                barChart.clear();
                pieChart.clear();
                return;
            }

            List<FuelRecord> records = new ArrayList<>();
            for (QueryDocumentSnapshot document : value) {
                FuelRecord record = document.toObject(FuelRecord.class);
                if (record != null) {
                    records.add(record);
                }
            }

            if (records.isEmpty()) {
                showEmptyState(true);
                barChart.clear();
                pieChart.clear();
            } else {
                showEmptyState(false);
                updateBarChart(records);
                updatePieChart(records);
            }
        }
    };

    private void setupBarChart() {
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);
        barChart.setScaleEnabled(false);
        barChart.setNoDataText(getString(R.string.summary_no_data));
        barChart.setExtraOffsets(12f, 12f, 12f, 32f);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(11f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setTextSize(11f);

        Legend legend = barChart.getLegend();
        legend.setEnabled(false);
    }

    private void setupPieChart() {
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setNoDataText(getString(R.string.summary_no_data));

        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setTextSize(12f);
    }

    private void updateBarChart(List<FuelRecord> records) {
        Map<Long, Float> monthTotals = new TreeMap<>();
        for (FuelRecord record : records) {
            long dateMillis = record.getDate();
            if (dateMillis == 0) {
                continue;
            }
            long key = normaliseMonth(dateMillis);
            float liters = (float) record.getLiters();
            Float current = monthTotals.get(key);
            if (current == null) {
                monthTotals.put(key, liters);
            } else {
                monthTotals.put(key, current + liters);
            }
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Long, Float> entry : monthTotals.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(monthLabelFormat.format(new Date(entry.getKey())));
            index++;
        }

        if (entries.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText(getString(R.string.summary_no_data));
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.summary_bar_legend));
        dataSet.setColor(getResources().getColor(R.color.purple_300, requireContext().getTheme()));
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(getResources().getColor(R.color.black, requireContext().getTheme()));

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);

        barChart.setData(barData);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-20f);
        barChart.animateY(600);
        barChart.invalidate();
    }

    private void updatePieChart(List<FuelRecord> records) {
        Map<String, Float> fuelTotals = new LinkedHashMap<>();
        for (FuelRecord record : records) {
            String type = record.getFuelType();
            if (TextUtils.isEmpty(type)) {
                type = getString(R.string.summary_unknown_fuel);
            }
            float liters = (float) record.getLiters();
            Float current = fuelTotals.get(type);
            if (current == null) {
                fuelTotals.put(type, liters);
            } else {
                fuelTotals.put(type, current + liters);
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : fuelTotals.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText(getString(R.string.summary_no_data));
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(pieData);
        pieChart.animateY(600);
        pieChart.invalidate();
    }

    private long normaliseMonth(long dateMillis) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(dateMillis);
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private void showEmptyState(boolean empty) {
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        barChart.setVisibility(empty ? View.GONE : View.VISIBLE);
        pieChart.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
