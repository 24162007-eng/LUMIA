package com.example.lumia.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lumia.R;
import com.example.lumia.models.ObstacleReport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter handling ObstacleReport display in RecyclerView for the COMMUNITY module.
 */
public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    public interface OnReportClickListener {
        void onReportClick(ObstacleReport report);
    }

    private List<ObstacleReport> reports;
    private final OnReportClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public ReportAdapter(List<ObstacleReport> reports, OnReportClickListener listener) {
        this.reports = (reports != null) ? reports : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<ObstacleReport> newReports) {
        this.reports = (newReports != null) ? newReports : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        ObstacleReport report = reports.get(position);
        holder.bind(report, listener, dateFormat);
    }

    @Override
    public int getItemCount() {
        return reports.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivReportThumbnail;
        private final TextView tvReportType;
        private final TextView tvReportDescription;
        private final TextView tvReportTimestamp;
        private final TextView tvReportStatus;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            ivReportThumbnail = itemView.findViewById(R.id.ivReportThumbnail);
            tvReportType = itemView.findViewById(R.id.tvReportType);
            tvReportDescription = itemView.findViewById(R.id.tvReportDescription);
            tvReportTimestamp = itemView.findViewById(R.id.tvReportTimestamp);
            tvReportStatus = itemView.findViewById(R.id.tvReportStatus);
        }

        public void bind(final ObstacleReport report, final OnReportClickListener listener, SimpleDateFormat dateFormat) {
            tvReportType.setText(report.getType() != null ? report.getType() : "");
            tvReportDescription.setText(report.getDescription() != null ? report.getDescription() : "");

            if (report.getTimestamp() > 0) {
                tvReportTimestamp.setText(dateFormat.format(new Date(report.getTimestamp())));
            } else {
                tvReportTimestamp.setText("");
            }

            String imageBase64 = report.getImageBase64();
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                try {
                    byte[] decodedString = Base64.decode(imageBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    if (bitmap != null) {
                        ivReportThumbnail.setImageBitmap(bitmap);
                        ivReportThumbnail.setVisibility(View.VISIBLE);
                    } else {
                        ivReportThumbnail.setVisibility(View.GONE);
                    }
                } catch (Exception e) {
                    ivReportThumbnail.setVisibility(View.GONE);
                }
            } else {
                ivReportThumbnail.setVisibility(View.GONE);
            }

            boolean isResolved = "resolved".equalsIgnoreCase(report.getStatus());
            if (isResolved) {
                tvReportStatus.setText(itemView.getContext().getString(R.string.report_status_resolved));
                tvReportStatus.setBackgroundResource(R.drawable.bg_status_resolved);
                tvReportStatus.setTextColor(Color.parseColor("#123B36"));
            } else {
                tvReportStatus.setText(itemView.getContext().getString(R.string.report_status_active));
                tvReportStatus.setBackgroundResource(R.drawable.bg_status_active);
                tvReportStatus.setTextColor(Color.WHITE);
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onReportClick(report);
                    }
                }
            });
        }
    }
}
