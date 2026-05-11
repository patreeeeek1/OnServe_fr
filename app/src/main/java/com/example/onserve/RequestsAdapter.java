package com.example.onserve;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.RequestViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(HelpRequest request);
    }

    private List<HelpRequest> requests;
    private OnDeleteClickListener deleteClickListener;

    public RequestsAdapter(List<HelpRequest> requests, OnDeleteClickListener deleteClickListener) {
        this.requests = requests;
        this.deleteClickListener = deleteClickListener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        HelpRequest request = requests.get(position);
        holder.tvType.setText(request.getType());
        holder.tvStatus.setText(request.getStatus());
        holder.tvDescription.setText(request.getDescription());
        holder.tvLocation.setText(request.getLocation());
        
        String dateText = request.isAsap() ? "ASAP" : request.getDate();
        if (request.getScheduledDate() != null && !request.getScheduledDate().isEmpty()) {
            dateText = "Scheduled: " + request.getScheduledDate();
            if (request.getScheduledTime() != null) {
                dateText += " (" + request.getScheduledTime() + ")";
            }
        }
        holder.tvDate.setText(dateText);

        // Details mapping
        if (request.getTimestamp() > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault());
            holder.tvSubmittedDate.setText(sdf.format(new java.util.Date(request.getTimestamp())));
        } else {
            holder.tvSubmittedDate.setText("N/A");
        }
        holder.tvUserName.setText(request.getUserName() != null ? request.getUserName() : "N/A");
        holder.tvEmailAddr.setText(request.getUserEmail());
        holder.tvPhone.setText(request.getPhoneNumber());
        holder.tvFullDescription.setText(request.getDescription());

        // Styling for Emergency
        if (request.isEmergency()) {
            holder.tvType.setTextColor(0xFFC62828); // red_emergency
            holder.tvEmergencyBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvType.setTextColor(0xFF18263A); // navy_onserve
            holder.tvEmergencyBadge.setVisibility(View.GONE);
        }

        // Toggling details
        holder.btnShowMore.setOnClickListener(v -> {
            boolean isVisible = holder.layoutDetails.getVisibility() == View.VISIBLE;
            holder.layoutDetails.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            holder.btnShowMore.setText(isVisible ? "Show more" : "Show less");
            holder.tvDescription.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        });

        // Color status based on text
        if (request.getStatus().equals("Done")) {
            holder.tvStatus.setTextColor(0xFF4CAF50); // Green
            holder.btnDelete.setVisibility(View.GONE);
            holder.tvStatus.setBackgroundResource(R.drawable.card_selected_highlight);
        } else if (request.getStatus().equals("Assigned") || request.getStatus().equals("In Progress")) {
            holder.tvStatus.setTextColor(0xFF2E7D32); // Dark Green
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.tvStatus.setBackgroundResource(R.drawable.card_selected_highlight);
        } else if (request.getStatus().equals("In Waiting Queue")) {
            holder.tvStatus.setTextColor(0xFF26ACE1); // Blue
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.tvStatus.setBackgroundResource(R.drawable.card_selected_highlight);
        } else if (request.getStatus().equals("Pending Approval")) {
            holder.tvStatus.setTextColor(0xFF666666); // Gray
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.tvStatus.setBackgroundResource(R.drawable.card_selected_highlight);
        } else {
            holder.tvStatus.setTextColor(0xFF666666); // Gray
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.tvStatus.setBackgroundResource(R.drawable.card_selected_highlight);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteClickListener != null) {
                deleteClickListener.onDeleteClick(request);
            }
        });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvStatus, tvDescription, tvLocation, tvDate, tvEmergencyBadge;
        TextView tvPhone, tvFullDescription, btnShowMore, tvUserName, tvEmailAddr, tvSubmittedDate;
        ImageView btnDelete;
        LinearLayout layoutDetails;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tv_request_type);
            tvStatus = itemView.findViewById(R.id.tv_request_status);
            tvEmergencyBadge = itemView.findViewById(R.id.tv_emergency_badge);
            tvDescription = itemView.findViewById(R.id.tv_request_description);
            tvLocation = itemView.findViewById(R.id.tv_request_location);
            tvDate = itemView.findViewById(R.id.tv_request_date);
            
            tvSubmittedDate = itemView.findViewById(R.id.tv_request_submitted_date);
            tvUserName = itemView.findViewById(R.id.tv_request_user_name);
            tvEmailAddr = itemView.findViewById(R.id.tv_request_email);
            tvPhone = itemView.findViewById(R.id.tv_request_phone);
            tvFullDescription = itemView.findViewById(R.id.tv_request_full_description);
            btnShowMore = itemView.findViewById(R.id.btn_show_more);
            btnDelete = itemView.findViewById(R.id.btn_delete_request);
            layoutDetails = itemView.findViewById(R.id.layout_request_details);
        }
    }
}
