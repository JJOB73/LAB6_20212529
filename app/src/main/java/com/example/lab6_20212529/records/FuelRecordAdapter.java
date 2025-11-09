package com.example.lab6_20212529.records;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lab6_20212529.R;
import com.example.lab6_20212529.model.FuelRecord;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FuelRecordAdapter extends RecyclerView.Adapter<FuelRecordAdapter.FuelRecordViewHolder> {

    public interface RecordActionListener {
        void onEdit(FuelRecord record);

        void onDelete(FuelRecord record);
    }

    private final List<FuelRecord> items;
    private final RecordActionListener listener;
    private final DateFormat dateFormat;
    private final NumberFormat numberFormat;
    private final NumberFormat currencyFormat;
    private final DecimalFormat mileageFormat;

    public FuelRecordAdapter(List<FuelRecord> items, RecordActionListener listener) {
        this.items = items;
        this.listener = listener;
        this.dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        this.numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        this.numberFormat.setMaximumFractionDigits(2);
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        mileageFormat = new DecimalFormat("0.##", symbols);
        mileageFormat.setGroupingUsed(false);
    }

    @NonNull
    @Override
    public FuelRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fuel_record, parent, false);
        return new FuelRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FuelRecordViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<FuelRecord> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    class FuelRecordViewHolder extends RecyclerView.ViewHolder {
        private final TextView textCode;
        private final TextView textVehicle;
        private final TextView textDate;
        private final TextView textLiters;
        private final TextView textMileage;
        private final TextView textPrice;
        private final TextView textFuelType;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;

        FuelRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            textCode = itemView.findViewById(R.id.textRecordCode);
            textVehicle = itemView.findViewById(R.id.textRecordVehicle);
            textDate = itemView.findViewById(R.id.textRecordDate);
            textLiters = itemView.findViewById(R.id.textRecordLiters);
            textMileage = itemView.findViewById(R.id.textRecordMileage);
            textPrice = itemView.findViewById(R.id.textRecordPrice);
            textFuelType = itemView.findViewById(R.id.textRecordFuelType);
            buttonEdit = itemView.findViewById(R.id.buttonEditRecord);
            buttonDelete = itemView.findViewById(R.id.buttonDeleteRecord);
        }

        void bind(FuelRecord record) {
            textCode.setText(itemView.getContext().getString(R.string.label_record_code, record.getRecordCode()));
            textVehicle.setText(itemView.getContext().getString(R.string.label_record_vehicle, record.getVehicleNickname()));
            textDate.setText(dateFormat.format(new Date(record.getDate())));
            textLiters.setText(itemView.getContext().getString(R.string.label_record_liters, numberFormat.format(record.getLiters())));
            textMileage.setText(itemView.getContext().getString(R.string.label_record_mileage_plain, formatMileage(record.getMileage())));
            textPrice.setText(itemView.getContext().getString(R.string.label_record_price, currencyFormat.format(record.getTotalPrice())));
            textFuelType.setText(itemView.getContext().getString(R.string.label_record_fuel_type, record.getFuelType()));

            buttonEdit.setOnClickListener(v -> listener.onEdit(record));
            buttonDelete.setOnClickListener(v -> listener.onDelete(record));
        }
    }

    private String formatMileage(double mileage) {
        return mileageFormat.format(mileage);
    }
}

