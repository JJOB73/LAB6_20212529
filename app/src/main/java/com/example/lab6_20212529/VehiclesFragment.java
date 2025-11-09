package com.example.lab6_20212529;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20212529.R;
import com.example.lab6_20212529.model.Vehicle;
import com.example.lab6_20212529.vehicles.VehicleAdapter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VehiclesFragment extends Fragment implements VehicleAdapter.VehicleActionListener {

    private RecyclerView recyclerView;
    private View emptyView;
    private ExtendedFloatingActionButton fabAdd;

    private VehicleAdapter adapter;
    private final List<Vehicle> vehicleList = new ArrayList<>();

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private CollectionReference vehiclesCollection;
    private CollectionReference fuelRecordsCollection;
    private ListenerRegistration listenerRegistration;

    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vehicles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerVehicles);
        emptyView = view.findViewById(R.id.textEmpty);
        fabAdd = view.findViewById(R.id.fabAddVehicle);

        adapter = new VehicleAdapter(vehicleList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        fabAdd.setOnClickListener(v -> showVehicleDialog(null));

        subscribeToVehicles();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    private void subscribeToVehicles() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showMessage(getString(R.string.error_authentication));
            return;
        }

        vehiclesCollection = firestore.collection("users")
                .document(user.getUid())
                .collection("vehicles");
        fuelRecordsCollection = firestore.collection("users")
                .document(user.getUid())
                .collection("fuelRecords");

        listenerRegistration = vehiclesCollection
                .orderBy("nickname")
                .addSnapshotListener(vehicleSnapshotListener);
    }

    private final EventListener<QuerySnapshot> vehicleSnapshotListener = (value, error) -> {
        if (error != null) {
            showMessage(getString(R.string.error_loading_vehicles));
            return;
        }

        if (value == null) {
            return;
        }

        List<Vehicle> items = new ArrayList<>();
        value.getDocuments().forEach(document -> {
            Vehicle vehicle = document.toObject(Vehicle.class);
            if (vehicle != null) {
                vehicle.setId(document.getId());
                items.add(vehicle);
            }
        });

        adapter.setItems(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    };

    private void showVehicleDialog(@Nullable Vehicle vehicle) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_vehicle, null, false);

        TextInputLayout nicknameLayout = dialogView.findViewById(R.id.inputLayoutNickname);
        TextInputLayout plateLayout = dialogView.findViewById(R.id.inputLayoutPlate);
        TextInputLayout brandLayout = dialogView.findViewById(R.id.inputLayoutBrand);
        TextInputLayout yearLayout = dialogView.findViewById(R.id.inputLayoutYear);
        TextInputLayout inspectionLayout = dialogView.findViewById(R.id.inputLayoutInspection);

        TextInputEditText nicknameEdit = dialogView.findViewById(R.id.editTextNickname);
        TextInputEditText plateEdit = dialogView.findViewById(R.id.editTextPlate);
        TextInputEditText brandEdit = dialogView.findViewById(R.id.editTextBrandModel);
        TextInputEditText yearEdit = dialogView.findViewById(R.id.editTextYear);
        TextInputEditText inspectionEdit = dialogView.findViewById(R.id.editTextInspection);

        final long[] inspectionDateMillis = {vehicle != null ? vehicle.getLastInspectionDate() : 0};
        if (vehicle != null) {
            nicknameEdit.setText(vehicle.getNickname());
            plateEdit.setText(vehicle.getPlate());
            brandEdit.setText(vehicle.getBrandModel());
            if (vehicle.getYear() > 0) {
                yearEdit.setText(String.valueOf(vehicle.getYear()));
            }
            if (inspectionDateMillis[0] > 0) {
                inspectionEdit.setText(dateFormat.format(inspectionDateMillis[0]));
            }
        }

        View.OnClickListener dateClickListener = v -> {
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.title_select_inspection)
                    .setSelection(inspectionDateMillis[0] > 0 ? inspectionDateMillis[0] : MaterialDatePicker.todayInUtcMilliseconds())
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                inspectionDateMillis[0] = selection;
                inspectionEdit.setText(dateFormat.format(selection));
            });
            picker.show(getParentFragmentManager(), "inspectionDatePicker");
        };

        inspectionLayout.setEndIconOnClickListener(dateClickListener);
        inspectionEdit.setOnClickListener(dateClickListener);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setTitle(vehicle == null ? R.string.title_add_vehicle : R.string.title_edit_vehicle)
                .setPositiveButton(vehicle == null ? R.string.action_save_vehicle : R.string.action_update_vehicle, null)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    boolean valid = true;

                    String nickname = getText(nicknameEdit);
                    String plate = getText(plateEdit);
                    String brandModel = getText(brandEdit);
                    String yearText = getText(yearEdit);

                    if (TextUtils.isEmpty(nickname)) {
                        nicknameLayout.setError(getString(R.string.error_vehicle_nickname));
                        valid = false;
                    } else {
                        nicknameLayout.setError(null);
                    }

                    if (TextUtils.isEmpty(plate)) {
                        plateLayout.setError(getString(R.string.error_vehicle_plate));
                        valid = false;
                    } else {
                        plateLayout.setError(null);
                    }

                    if (TextUtils.isEmpty(brandModel)) {
                        brandLayout.setError(getString(R.string.error_vehicle_brand));
                        valid = false;
                    } else {
                        brandLayout.setError(null);
                    }

                    int year = 0;
                    if (TextUtils.isEmpty(yearText)) {
                        yearLayout.setError(getString(R.string.error_vehicle_year));
                        valid = false;
                    } else {
                        try {
                            year = Integer.parseInt(yearText);
                            int currentYear = Calendar.getInstance().get(Calendar.YEAR) + 1;
                            if (year < 1950 || year > currentYear) {
                                yearLayout.setError(getString(R.string.error_vehicle_year_range, 1950, currentYear));
                                valid = false;
                            } else {
                                yearLayout.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            yearLayout.setError(getString(R.string.error_vehicle_year));
                            valid = false;
                        }
                    }

                    if (!valid) {
                        return;
                    }

                    saveVehicle(vehicle != null ? vehicle.getId() : null, nickname, plate.toUpperCase(Locale.getDefault()),
                            brandModel, year, inspectionDateMillis[0]);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void saveVehicle(@Nullable String id, String nickname, String plate, String brandModel,
                             int year, long inspectionDate) {
        if (vehiclesCollection == null) {
            showMessage(getString(R.string.error_authentication));
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("nickname", nickname);
        data.put("plate", plate);
        data.put("brandModel", brandModel);
        data.put("year", year);
        data.put("lastInspectionDate", inspectionDate);

        DocumentReference documentReference;
        if (TextUtils.isEmpty(id)) {
            documentReference = vehiclesCollection.document();
        } else {
            documentReference = vehiclesCollection.document(id);
        }

        documentReference.set(data)
                .addOnSuccessListener(unused -> showMessage(getString(R.string.message_vehicle_saved)))
                .addOnFailureListener(e -> showMessage(getString(R.string.error_vehicle_save)));
    }

    private void deleteVehicle(Vehicle vehicle) {
        if (vehiclesCollection == null || TextUtils.isEmpty(vehicle.getId())) {
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_delete_vehicle)
                .setMessage(getString(R.string.message_delete_vehicle, vehicle.getNickname()))
                .setPositiveButton(R.string.action_delete_vehicle, (dialog, which) -> vehiclesCollection.document(vehicle.getId())
                        .delete()
                        .addOnSuccessListener(unused -> showMessage(getString(R.string.message_vehicle_deleted)))
                        .addOnFailureListener(e -> showMessage(getString(R.string.error_vehicle_delete))))
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showMessage(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private String getText(@Nullable TextInputEditText editText) {
        return editText != null && editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    public void onEdit(Vehicle vehicle) {
        showVehicleDialog(vehicle);
    }

    @Override
    public void onDelete(Vehicle vehicle) {
        deleteVehicle(vehicle);
    }

    @Override
    public void onShowQr(Vehicle vehicle) {
        fetchLatestMileageAndShowQr(vehicle);
    }

    private void fetchLatestMileageAndShowQr(Vehicle vehicle) {
        if (fuelRecordsCollection == null) {
            showMessage(getString(R.string.error_loading_records));
            return;
        }

        fuelRecordsCollection.whereEqualTo("vehicleId", vehicle.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double lastMileage = 0;
                    if (!querySnapshot.isEmpty()) {
                        for (com.google.firebase.firestore.DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                            Double mileageValue = documentSnapshot.getDouble("mileage");
                            if (mileageValue != null && mileageValue > lastMileage) {
                                lastMileage = mileageValue;
                            }
                        }
                    }
                    showQrDialog(vehicle, lastMileage);
                })
                .addOnFailureListener(e -> showMessage(getString(R.string.error_loading_mileage)));
    }

    private void showQrDialog(Vehicle vehicle, double lastMileage) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_vehicle_qr, null, false);
        TextView textVehicleInfo = dialogView.findViewById(R.id.textVehicleInfo);
        TextView textMileageInfo = dialogView.findViewById(R.id.textMileageInfo);
        TextView textInspectionInfo = dialogView.findViewById(R.id.textInspectionInfo);
        ImageView imageQr = dialogView.findViewById(R.id.imageQr);

        textVehicleInfo.setText(getString(R.string.label_vehicle_plate, vehicle.getPlate()));
        if (lastMileage > 0) {
            textMileageInfo.setText(getString(R.string.label_record_mileage, String.format(Locale.getDefault(), "%.0f", lastMileage)));
        } else {
            textMileageInfo.setText(getString(R.string.message_vehicle_qr_no_mileage));
        }

        long inspectionDate = vehicle.getLastInspectionDate();
        String inspectionLabel = inspectionDate > 0
                ? dateFormat.format(inspectionDate)
                : getString(R.string.message_vehicle_qr_no_inspection);
        textInspectionInfo.setText(getString(R.string.label_vehicle_last_inspection, inspectionLabel));

        String payload = createQrPayload(vehicle, lastMileage, inspectionDate);
        Bitmap bitmap = generateQrBitmap(payload, 512);
        if (bitmap == null) {
            showMessage(getString(R.string.error_qr_generation));
            return;
        }
        imageQr.setImageBitmap(bitmap);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_vehicle_qr)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String createQrPayload(Vehicle vehicle, double mileage, long inspectionDate) {
        JSONObject json = new JSONObject();
        try {
            json.put("placa", vehicle.getPlate());
            json.put("kilometraje", mileage);
            String revision = inspectionDate > 0 ? isoDateFormat.format(inspectionDate) : "";
            json.put("revision", revision);
            return json.toString();
        } catch (JSONException e) {
            return vehicle.getPlate() + "|" + mileage + "|" + inspectionDate;
        }
    }

    @Nullable
    private Bitmap generateQrBitmap(String content, int size) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }
}

