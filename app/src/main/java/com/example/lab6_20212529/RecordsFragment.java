package com.example.lab6_20212529;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20212529.model.FuelRecord;
import com.example.lab6_20212529.model.Vehicle;
import com.example.lab6_20212529.records.FuelRecordAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class RecordsFragment extends Fragment implements FuelRecordAdapter.RecordActionListener {

    private RecyclerView recyclerView;
    private View emptyView;
    private ExtendedFloatingActionButton fabAddRecord;
    private MaterialAutoCompleteTextView filterVehicleAutoComplete;
    private TextInputLayout filterVehicleLayout;
    private TextInputLayout filterDateLayout;
    private TextInputEditText filterDateEditText;
    private MaterialButton buttonClearFilters;

    private FuelRecordAdapter adapter;
    private final List<FuelRecord> allRecords = new ArrayList<>();
    private final List<FuelRecord> filteredRecords = new ArrayList<>();

    private final Map<String, Vehicle> vehiclesMap = new HashMap<>();
    private final List<Vehicle> vehiclesList = new ArrayList<>();
    private final List<String> vehicleNames = new ArrayList<>();
    private final List<String> vehicleIds = new ArrayList<>();

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private CollectionReference recordsCollection;
    private CollectionReference vehiclesCollection;
    private ListenerRegistration recordsListener;
    private ListenerRegistration vehiclesListener;

    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
    private final DecimalFormat plainNumberFormat = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));

    private String filterVehicleId;
    private Long filterStartDate;
    private Long filterEndDate;

    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerRecords);
        emptyView = view.findViewById(R.id.textEmpty);
        fabAddRecord = view.findViewById(R.id.fabAddRecord);
        filterVehicleAutoComplete = view.findViewById(R.id.filterVehicleAutoComplete);
        filterVehicleLayout = view.findViewById(R.id.filterVehicleLayout);
        filterDateLayout = view.findViewById(R.id.filterDateLayout);
        filterDateEditText = view.findViewById(R.id.filterDateEditText);
        buttonClearFilters = view.findViewById(R.id.buttonClearFilters);

        adapter = new FuelRecordAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        numberFormat.setMaximumFractionDigits(2);
        plainNumberFormat.setMaximumFractionDigits(2);
        plainNumberFormat.setGroupingUsed(false);

        fabAddRecord.setOnClickListener(v -> showRecordDialog(null));
        buttonClearFilters.setOnClickListener(v -> clearFilters());

        View.OnClickListener dateFilterClick = v -> showDateRangePicker();
        filterDateLayout.setEndIconOnClickListener(dateFilterClick);
        filterDateEditText.setOnClickListener(dateFilterClick);

        filterVehicleAutoComplete.setOnItemClickListener((parent, v, position, id) -> {
            if (position == 0) {
                filterVehicleId = null;
            } else {
                filterVehicleId = vehicleIds.get(position - 1);
            }
            applyFilters();
        });

        subscribeToData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recordsListener != null) {
            recordsListener.remove();
            recordsListener = null;
        }
        if (vehiclesListener != null) {
            vehiclesListener.remove();
            vehiclesListener = null;
        }
    }

    private void subscribeToData() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showMessage(getString(R.string.error_authentication));
            return;
        }

        recordsCollection = firestore.collection("users")
                .document(user.getUid())
                .collection("fuelRecords");

        vehiclesCollection = firestore.collection("users")
                .document(user.getUid())
                .collection("vehicles");

        recordsListener = recordsCollection
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(recordsSnapshotListener);

        vehiclesListener = vehiclesCollection
                .orderBy("nickname", Query.Direction.ASCENDING)
                .addSnapshotListener(vehiclesSnapshotListener);
    }

    private final EventListener<QuerySnapshot> recordsSnapshotListener = (value, error) -> {
        if (error != null) {
            showMessage(getString(R.string.error_loading_records));
            return;
        }

        if (value == null) {
            return;
        }

        allRecords.clear();
        for (QueryDocumentSnapshot document : value) {
            FuelRecord record = document.toObject(FuelRecord.class);
            if (record != null) {
                record.setId(document.getId());
                allRecords.add(record);
            }
        }

        resolveVehicleNames();
        applyFilters();
    };

    private final EventListener<QuerySnapshot> vehiclesSnapshotListener = (value, error) -> {
        if (error != null || value == null) {
            return;
        }

        vehiclesMap.clear();
        vehiclesList.clear();
        vehicleNames.clear();
        vehicleIds.clear();

        vehicleNames.add(getString(R.string.filter_vehicle));
        for (QueryDocumentSnapshot document : value) {
            Vehicle vehicle = document.toObject(Vehicle.class);
            if (vehicle != null) {
                vehicle.setId(document.getId());
                vehiclesMap.put(vehicle.getId(), vehicle);
                vehiclesList.add(vehicle);
                vehicleNames.add(vehicle.getNickname());
                vehicleIds.add(vehicle.getId());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, vehicleNames);
        filterVehicleAutoComplete.setAdapter(adapter);

        if (filterVehicleId != null) {
            int index = vehicleIds.indexOf(filterVehicleId);
            if (index >= 0) {
                filterVehicleAutoComplete.setText(vehicleNames.get(index + 1), false);
            }
        }

        resolveVehicleNames();
        applyFilters();
    };

    private void resolveVehicleNames() {
        for (FuelRecord record : allRecords) {
            Vehicle vehicle = vehiclesMap.get(record.getVehicleId());
            if (vehicle != null) {
                record.setVehicleNickname(vehicle.getNickname());
            }
        }
    }

    private void applyFilters() {
        filteredRecords.clear();
        for (FuelRecord record : allRecords) {
            if (filterVehicleId != null && !filterVehicleId.equals(record.getVehicleId())) {
                continue;
            }

            if (filterStartDate != null && filterEndDate != null) {
                long date = record.getDate();
                if (date < filterStartDate || date > filterEndDate) {
                    continue;
                }
            }

            filteredRecords.add(record);
        }
        adapter.setItems(new ArrayList<>(filteredRecords));
        emptyView.setVisibility(filteredRecords.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void clearFilters() {
        filterVehicleId = null;
        filterStartDate = null;
        filterEndDate = null;
        filterVehicleAutoComplete.setText("");
        filterDateEditText.setText("");
        applyFilters();
    }

    private void showDateRangePicker() {
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.filter_date_range);
        if (filterStartDate != null && filterEndDate != null) {
            builder.setSelection(new Pair<>(filterStartDate, filterEndDate));
        }
        MaterialDatePicker<Pair<Long, Long>> picker = builder.build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                filterStartDate = selection.first;
                filterEndDate = selection.second;
                filterDateEditText.setText(picker.getHeaderText());
                applyFilters();
            }
        });
        picker.addOnNegativeButtonClickListener(dialog -> {
        });
        picker.show(getParentFragmentManager(), "recordsDatePicker");
    }

    private void showRecordDialog(@Nullable FuelRecord record) {
        if (vehiclesMap.isEmpty()) {
            showMessage(getString(R.string.error_record_vehicle));
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_fuel_record, null, false);

        TextInputLayout vehicleLayout = dialogView.findViewById(R.id.inputLayoutVehicle);
        TextInputLayout dateLayout = dialogView.findViewById(R.id.inputLayoutDate);
        TextInputLayout litersLayout = dialogView.findViewById(R.id.inputLayoutLiters);
        TextInputLayout mileageLayout = dialogView.findViewById(R.id.inputLayoutMileage);
        TextInputLayout priceLayout = dialogView.findViewById(R.id.inputLayoutPrice);
        TextInputLayout fuelTypeLayout = dialogView.findViewById(R.id.inputLayoutFuelType);

        MaterialAutoCompleteTextView vehicleAutoComplete = dialogView.findViewById(R.id.autoCompleteVehicle);
        TextInputEditText dateEditText = dialogView.findViewById(R.id.editTextDate);
        TextInputEditText litersEditText = dialogView.findViewById(R.id.editTextLiters);
        TextInputEditText mileageEditText = dialogView.findViewById(R.id.editTextMileage);
        TextInputEditText priceEditText = dialogView.findViewById(R.id.editTextPrice);
        MaterialAutoCompleteTextView fuelTypeAutoComplete = dialogView.findViewById(R.id.autoCompleteFuelType);

        List<String> options = new ArrayList<>();
        for (Vehicle vehicle : vehiclesList) {
            options.add(vehicle.getNickname());
        }
        ArrayAdapter<String> vehicleAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, options);
        vehicleAutoComplete.setAdapter(vehicleAdapter);

        ArrayAdapter<String> fuelAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.fuel_types));
        fuelTypeAutoComplete.setAdapter(fuelAdapter);

        final String[] selectedVehicleId = {null};
        final long[] selectedDate = {record != null ? record.getDate() : MaterialDatePicker.todayInUtcMilliseconds()};
        final String[] recordCode = {record != null ? record.getRecordCode() : null};

        if (record != null) {
            if (!TextUtils.isEmpty(record.getVehicleId())) {
                for (int i = 0; i < vehiclesList.size(); i++) {
                    Vehicle vehicle = vehiclesList.get(i);
                    if (vehicle.getId().equals(record.getVehicleId())) {
                        vehicleAutoComplete.setText(vehicle.getNickname(), false);
                        selectedVehicleId[0] = vehicle.getId();
                        break;
                    }
                }
            }
            if (record.getDate() > 0) {
                dateEditText.setText(dateFormat.format(record.getDate()));
            }
            litersEditText.setText(plainNumberFormat.format(record.getLiters()));
            mileageEditText.setText(plainNumberFormat.format(record.getMileage()));
            priceEditText.setText(plainNumberFormat.format(record.getTotalPrice()));
            if (!TextUtils.isEmpty(record.getFuelType())) {
                fuelTypeAutoComplete.setText(record.getFuelType(), false);
            }
        }

        vehicleAutoComplete.setOnItemClickListener((parent, view1, position, id) -> {
            if (position >= 0 && position < vehiclesList.size()) {
                selectedVehicleId[0] = vehiclesList.get(position).getId();
            }
        });

        View.OnClickListener dateClickListener = v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.hint_record_date)
                    .setSelection(selectedDate[0])
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = selection;
                dateEditText.setText(dateFormat.format(selection));
            });
            picker.show(getParentFragmentManager(), "recordDatePicker");
        };

        dateLayout.setEndIconOnClickListener(dateClickListener);
        dateEditText.setOnClickListener(dateClickListener);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(record == null ? R.string.title_add_record : R.string.title_edit_record)
                .setView(dialogView)
                .setPositiveButton(record == null ? R.string.action_save_record : R.string.action_update_record, null)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dlg -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    boolean valid = true;

                    String vehicleId = selectedVehicleId[0];
                    if (vehicleId == null) {
                        vehicleLayout.setError(getString(R.string.error_record_vehicle));
                        valid = false;
                    } else {
                        vehicleLayout.setError(null);
                    }

                    long date = selectedDate[0];
                    if (date == 0) {
                        dateLayout.setError(getString(R.string.error_record_date));
                        valid = false;
                    } else {
                        dateLayout.setError(null);
                    }

                    double liters = parseDouble(litersEditText.getText());
                    if (liters <= 0) {
                        litersLayout.setError(getString(R.string.error_record_liters));
                        valid = false;
                    } else {
                        litersLayout.setError(null);
                    }

                    double mileage = parseDouble(mileageEditText.getText());
                    if (mileage <= 0) {
                        mileageLayout.setError(getString(R.string.error_record_mileage));
                        valid = false;
                    } else {
                        mileageLayout.setError(null);
                    }

                    double price = parseDouble(priceEditText.getText());
                    if (price <= 0) {
                        priceLayout.setError(getString(R.string.error_record_price));
                        valid = false;
                    } else {
                        priceLayout.setError(null);
                    }

                    String fuelType = fuelTypeAutoComplete.getText() != null
                            ? fuelTypeAutoComplete.getText().toString()
                            : "";
                    if (TextUtils.isEmpty(fuelType)) {
                        fuelTypeLayout.setError(getString(R.string.error_record_fuel_type));
                        valid = false;
                    } else {
                        fuelTypeLayout.setError(null);
                    }

                    if (!valid) {
                        return;
                    }

                    double lastMileage = getLatestMileage(vehicleId, record != null ? record.getId() : null);
                    if (mileage <= lastMileage) {
                        mileageLayout.setError(getString(R.string.error_record_mileage_lower,
                                numberFormat.format(lastMileage)));
                        return;
                    } else {
                        mileageLayout.setError(null);
                    }

                    if (recordCode[0] == null) {
                        recordCode[0] = generateRecordCode();
                    }

                    saveRecord(record != null ? record.getId() : null,
                            recordCode[0],
                            vehicleId,
                            date,
                            liters,
                            mileage,
                            price,
                            fuelType);

                    dialog.dismiss();
                }));
        dialog.show();
    }

    private double parseDouble(@Nullable CharSequence text) {
        if (text == null) {
            return 0;
        }
        try {
            return Double.parseDouble(text.toString().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private double getLatestMileage(String vehicleId, @Nullable String excludeRecordId) {
        double maxMileage = 0;
        for (FuelRecord record : allRecords) {
            if (!vehicleId.equals(record.getVehicleId())) {
                continue;
            }
            if (excludeRecordId != null && excludeRecordId.equals(record.getId())) {
                continue;
            }
            if (record.getMileage() > maxMileage) {
                maxMileage = record.getMileage();
            }
        }
        return maxMileage;
    }

    private void saveRecord(@Nullable String id,
                            String recordCode,
                            String vehicleId,
                            long date,
                            double liters,
                            double mileage,
                            double totalPrice,
                            String fuelType) {
        if (recordsCollection == null) {
            showMessage(getString(R.string.error_record_save));
            return;
        }

        Vehicle vehicle = vehiclesMap.get(vehicleId);
        String vehicleNickname = vehicle != null ? vehicle.getNickname() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("recordCode", recordCode);
        data.put("vehicleId", vehicleId);
        data.put("vehicleNickname", vehicleNickname);
        data.put("date", date);
        data.put("liters", liters);
        data.put("mileage", mileage);
        data.put("totalPrice", totalPrice);
        data.put("fuelType", fuelType);

        DocumentReference documentReference = TextUtils.isEmpty(id)
                ? recordsCollection.document()
                : recordsCollection.document(id);

        documentReference.set(data)
                .addOnSuccessListener(unused -> showMessage(getString(R.string.message_record_saved)))
                .addOnFailureListener(e -> showMessage(getString(R.string.error_record_save)));
    }

    private void deleteRecord(FuelRecord record) {
        if (recordsCollection == null || TextUtils.isEmpty(record.getId())) {
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_delete_record)
                .setMessage(getString(R.string.message_delete_record, record.getRecordCode()))
                .setPositiveButton(R.string.action_delete_record, (dialog, which) -> recordsCollection.document(record.getId())
                        .delete()
                        .addOnSuccessListener(unused -> showMessage(getString(R.string.message_record_deleted)))
                        .addOnFailureListener(e -> showMessage(getString(R.string.error_record_delete))))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String generateRecordCode() {
        return String.format(Locale.getDefault(), "%05d", random.nextInt(100000));
    }

    private void showMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onEdit(FuelRecord record) {
        showRecordDialog(record);
    }

    @Override
    public void onDelete(FuelRecord record) {
        deleteRecord(record);
    }
}
