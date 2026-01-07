package com.example.fixitfinderapp.adapters;

import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fixitfinderapp.R;
import com.example.fixitfinderapp.models.Service;
import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.VH> {

    private final List<Service> services;

    public ServiceAdapter(List<Service> services) {
        this.services = services;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup p, int v) {
        return new VH(LayoutInflater.from(p.getContext())
                .inflate(R.layout.item_service, p, false));
    }

    @Override
    public void onBindViewHolder(VH h, int i) {
        h.name.setText(services.get(i).name);
    }

    @Override
    public int getItemCount() {
        return services.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.txtServiceName);
        }
    }
}
