package es.iescarrillo.sneakerhub.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.adapters.OrderDetailAdapter;
import es.iescarrillo.sneakerhub.models.Order;

public class OrderDetailFragment extends Fragment {
    private Order order;

    public OrderDetailFragment() {}
    public OrderDetailFragment(Order order) { this.order = order; }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- CAPTURAMOS EL GESTO/BOTÓN ATRÁS DEL SISTEMA ---
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                volverAlHistorial();
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_order_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (order == null) return;

        TextView tvOrderId = view.findViewById(R.id.tvDetailOrderId);
        TextView tvTotal = view.findViewById(R.id.tvDetailFullTotal);
        TextView tvAddress = view.findViewById(R.id.tvDetailAddress);
        TextView tvPayment = view.findViewById(R.id.tvDetailPayment);
        RecyclerView rvProducts = view.findViewById(R.id.rvDetailProducts);

        tvOrderId.setText("Referencia: " + order.getOrder_id());
        tvTotal.setText(String.format("%.2f €", order.getTotal()));

        String fullAddress = order.getAddress();
        if (order.getDoor() != null && !order.getDoor().isEmpty()) {
            fullAddress += ", " + order.getDoor();
        }
        tvAddress.setText(fullAddress + "\n" + order.getZipCode() + ", " + order.getCity());
        tvPayment.setText("Método de pago: " + order.getPaymentMethod());

        rvProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProducts.setAdapter(new OrderDetailAdapter(order.getPurchased_sneakers()));
    }

    private void volverAlHistorial() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new OrderHistoryFragment(), null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            View topBar = getActivity().findViewById(R.id.topBar);
            if (topBar != null) topBar.setVisibility(View.GONE);
        }
    }
}