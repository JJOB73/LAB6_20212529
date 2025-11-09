package com.example.lab6_20212529.vehicles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20212529.R;
import com.example.lab6_20212529.model.Vehicle;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

public class VehicleAdapter extends RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder> {

    public interface VehicleActionListener {
        void onEdit(Vehicle vehicle);

        void onDelete(Vehicle vehicle);

        void onShowQr(Vehicle vehicle);
    }

    private final List<Vehicle> vehicles;
    private final VehicleActionListener listener;
    private final DateFormat dateFormat;

    public VehicleAdapter(List<Vehicle> vehicles, VehicleActionListener listener) {
        this.vehicles = vehicles;
        this.listener = listener;
        this.dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
    }

    @NonNull
    @Override
    public VehicleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vehicle, parent, false);
        return new VehicleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VehicleViewHolder holder, int position) {
        Vehicle vehicle = vehicles.get(position);
        holder.bind(vehicle);
    }

    @Override
    public int getItemCount() {
        return vehicles.size();
    }

    public void setItems(List<Vehicle> newItems) {
        vehicles.clear();
        vehicles.addAll(newItems);
        notifyDataSetChanged();
    }

    class VehicleViewHolder extends RecyclerView.ViewHolder {
        private final TextView textNickname;
        private final TextView textPlate;
        private final TextView textBrandModel;
        private final TextView textYear;
        private final TextView textInspection;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;
        private final ImageButton buttonQr;

        VehicleViewHolder(@NonNull View itemView) {
            super(itemView);
            textNickname = itemView.findViewById(R.id.textNickname);
            textPlate = itemView.findViewById(R.id.textPlate);
            textBrandModel = itemView.findViewById(R.id.textBrandModel);
            textYear = itemView.findViewById(R.id.textYear);
            textInspection = itemView.findViewById(R.id.textLastInspection);
            buttonEdit = itemView.findViewById(R.id.buttonEdit);
            buttonDelete = itemView.findViewById(R.id.buttonDelete);
            buttonQr = itemView.findViewById(R.id.buttonQr);
        }

        void bind(Vehicle vehicle) {
            textNickname.setText(vehicle.getNickname());
            textPlate.setText(itemView.getContext().getString(R.string.label_vehicle_plate, vehicle.getPlate()));
            textBrandModel.setText(itemView.getContext().getString(R.string.label_vehicle_brand_model, vehicle.getBrandModel()));
            textYear.setText(itemView.getContext().getString(R.string.label_vehicle_year, vehicle.getYear()));

            if (vehicle.getLastInspectionDate() > 0) {
                String formattedDate = dateFormat.format(new Date(vehicle.getLastInspectionDate()));
                textInspection.setText(itemView.getContext().getString(R.string.label_vehicle_last_inspection, formattedDate));
                textInspection.setVisibility(View.VISIBLE);
            } else {
                textInspection.setVisibility(View.GONE);
            }

            buttonEdit.setOnClickListener(v -> listener.onEdit(vehicle));
            buttonDelete.setOnClickListener(v -> listener.onDelete(vehicle));
            buttonQr.setOnClickListener(v -> listener.onShowQr(vehicle));
        }
    }
}

