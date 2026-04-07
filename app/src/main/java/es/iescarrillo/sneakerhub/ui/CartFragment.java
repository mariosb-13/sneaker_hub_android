package es.iescarrillo.sneakerhub.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.functions.FirebaseFunctions;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import es.iescarrillo.sneakerhub.R;
import es.iescarrillo.sneakerhub.adapters.CartAdapter;
import es.iescarrillo.sneakerhub.models.DetalleCarrito;
import es.iescarrillo.sneakerhub.models.Order;
import es.iescarrillo.sneakerhub.models.SneakerCopy;

public class CartFragment extends Fragment {

    private RecyclerView rvCartItems;
    private CartAdapter adapter;
    private TextView tvTotalPrice;
    private Button btnCheckout;
    private LinearLayout llEmptyCart;
    private ConstraintLayout layoutLoadingCart;

    private List<DetalleCarrito> cartList;
    private double totalAcumulado = 0;

    private PaymentSheet paymentSheet;
    private FirebaseFunctions mFunctions;
    private String paymentIntentClientSecret;

    // Variables temporales para la dirección
    private String tempStreet, tempCity, tempZip, tempDoor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PaymentConfiguration.init(requireContext(), "pk_test_51TJ7gdAVMjolobzNIGzBUR8GhTClqe3tkieFZWB4RhxBMShwX1u1UsMqZqyRrogtfGWH5A2z7nSLkRc5C8NJ6cW400sMfwNlbL");
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
        mFunctions = FirebaseFunctions.getInstance("europe-west1");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCartItems = view.findViewById(R.id.rvCartItems);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        btnCheckout = view.findViewById(R.id.btnCheckout);
        llEmptyCart = view.findViewById(R.id.llEmptyCart);
        layoutLoadingCart = view.findViewById(R.id.layoutLoadingCart);

        cartList = new ArrayList<>();
        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CartAdapter(cartList, item -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cart")
                        .child(user.getUid()).child(item.getDetalleCartId());
                if (item.getCantidad() <= 0) ref.removeValue();
                else ref.child("cantidad").setValue(item.getCantidad());
            }
        });
        rvCartItems.setAdapter(adapter);

        escucharCarrito();

        if (btnCheckout != null) {
            // El proceso inicia con la verificación de stock
            btnCheckout.setOnClickListener(v -> verificarStockAntesDeContinuar());
        }
    }

    private void escucharCarrito() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseDatabase.getInstance().getReference("cart").child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        cartList.clear();
                        totalAcumulado = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            DetalleCarrito item = ds.getValue(DetalleCarrito.class);
                            if (item != null) {
                                cartList.add(item);
                                totalAcumulado += (item.getPrice() * item.getCantidad());
                            }
                        }
                        adapter.notifyDataSetChanged();
                        tvTotalPrice.setText(String.format("%.2f €", totalAcumulado));

                        boolean vacio = cartList.isEmpty();
                        rvCartItems.setVisibility(vacio ? View.GONE : View.VISIBLE);
                        llEmptyCart.setVisibility(vacio ? View.VISIBLE : View.GONE);
                        btnCheckout.setEnabled(!vacio);
                        if (layoutLoadingCart != null) layoutLoadingCart.setVisibility(View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // PASO 1: Verificar Stock
    private void verificarStockAntesDeContinuar() {
        btnCheckout.setEnabled(false);
        btnCheckout.setText("Verificando stock...");

        DatabaseReference snkRef = FirebaseDatabase.getInstance().getReference("sneakers");
        final int[] verificados = {0};
        final boolean[] errorStock = {false};

        for (DetalleCarrito item : cartList) {
            String tallaKey = item.getTallaElegida().replace(".", "_");
            snkRef.child(item.getProductId()).child("sizes").child(tallaKey)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Integer stock = snapshot.getValue(Integer.class);
                            if (stock == null || stock < item.getCantidad()) {
                                errorStock[0] = true;
                                Toast.makeText(getContext(), "Sin stock: " + item.getName(), Toast.LENGTH_LONG).show();
                            }
                            verificados[0]++;
                            if (verificados[0] == cartList.size()) {
                                if (!errorStock[0]) {
                                    // Stock OK -> PASO 2: Dirección
                                    comprobarDireccionAntesDelPago();
                                } else {
                                    resetBoton();
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { resetBoton(); }
                    });
        }
    }

    /**
     * Comprobamos si tenemos dirección
     */
    private void comprobarDireccionAntesDelPago() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        btnCheckout.setText("Comprobando envío...");

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Nodo address según tu estructura
                DataSnapshot addressNode = snapshot.child("address");

                // Verificamos si el nodo siquiera existe
                if (!addressNode.exists()) {
                    mostrarDialogoDireccion();
                    return;
                }

                tempZip = getSafeString(addressNode.child("zipCode"));
                tempCity = getSafeString(addressNode.child("city"));
                tempStreet = getSafeString(addressNode.child("street"));
                tempDoor = getSafeString(addressNode.child("door"));

                // Validación estricta: si falta algo vital, pedimos datos
                if (tempZip == null || tempCity == null || tempStreet == null ||
                        tempZip.trim().isEmpty() || tempCity.trim().isEmpty() || tempStreet.trim().isEmpty()) {
                    mostrarDialogoDireccion();
                } else {
                    // Si todo está ok, avisamos y vamos al Pago
                    Toast.makeText(getContext(), "Usando dirección guardada", Toast.LENGTH_SHORT).show();
                    solicitarPagoStripe();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { resetBoton(); }
        });
    }

    private String getSafeString(DataSnapshot ds) {
        Object value = ds.getValue();
        return (value instanceof String) ? (String) value : null;
    }

    /**
     * Muestra un diálogo para que el usuario introduzca la dirección en caso de que no tenga la direccion
     */
    private void mostrarDialogoDireccion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_address, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        EditText etZip = dialogView.findViewById(R.id.etDialogZip);
        EditText etCity = dialogView.findViewById(R.id.etDialogCity);
        EditText etStreet = dialogView.findViewById(R.id.etDialogStreet);
        EditText etDoor = dialogView.findViewById(R.id.etDialogDoor);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirmAddress);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnConfirm.setOnClickListener(v -> {
            tempZip = etZip.getText().toString().trim();
            tempCity = etCity.getText().toString().trim();
            tempStreet = etStreet.getText().toString().trim();
            tempDoor = etDoor.getText().toString().trim();

            if (tempZip.isEmpty() || tempCity.isEmpty() || tempStreet.isEmpty()) {
                Toast.makeText(getContext(), "Código Postal, Ciudad y Calle obligatorios", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                solicitarPagoStripe();
            }
        });
    }

    /**
     * Pasamos a Stripe
     */
    private void solicitarPagoStripe() {
        btnCheckout.setText("Procesando pago...");
        Map<String, Object> data = new HashMap<>();
        data.put("amount", (int)(totalAcumulado * 100));

        mFunctions.getHttpsCallable("createPaymentIntent")
                .call(data)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Map<String, Object> result = (Map<String, Object>) task.getResult().getData();
                        if (result != null && result.containsKey("clientSecret")) {
                            paymentIntentClientSecret = (String) result.get("clientSecret");
                            paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, new PaymentSheet.Configuration("SneakerHub"));
                        } else {
                            resetBoton();
                        }
                    } else {
                        resetBoton();
                    }
                });
    }

    private void onPaymentSheetResult(final PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            // PASO 4: Pago finalizado -> Guardar Pedido
            procederAGuardarPedido(tempStreet, tempCity, tempZip, tempDoor);
        } else {
            resetBoton();
        }
    }

    private void procederAGuardarPedido(String street, String city, String zip, String door) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // 1. Actualizar Perfil
        DatabaseReference addressRef = FirebaseDatabase.getInstance().getReference("users")
                .child(user.getUid()).child("address");
        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("street", street);
        profileUpdate.put("city", city);
        profileUpdate.put("zipCode", zip);
        profileUpdate.put("door", door);
        addressRef.updateChildren(profileUpdate);

        // 2. Crear Pedido
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(user.getUid());
        String orderId = ordersRef.push().getKey();

        List<SneakerCopy> items = new ArrayList<>();
        for (DetalleCarrito d : cartList) {
            SneakerCopy c = new SneakerCopy();
            c.setCopia_id(UUID.randomUUID().toString());
            c.setId_producto_original(d.getProductId());
            c.setName_snap(d.getName());
            c.setPrice_snap(d.getPrice());
            c.setTalla_elegida(d.getTallaElegida());
            c.setCantidad_comprada(d.getCantidad());
            c.setImagen_snap(d.getImageUrl());
            items.add(c);
        }

        Order order = new Order();
        order.setOrder_id(orderId);
        order.setOrder_date(System.currentTimeMillis());
        order.setTotal(totalAcumulado);
        order.setPurchased_sneakers(items);
        order.setStatus("PAID");
        order.setAddress(street);
        order.setCity(city);
        order.setZipCode(zip);
        order.setDoor(door);
        order.setPaymentMethod("Stripe (Tarjeta)");

        ordersRef.child(orderId).setValue(order).addOnSuccessListener(aVoid -> {
            vaciarCarritoYStock();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SuccessFragment(), null);
            }
        });
    }

    private void vaciarCarritoYStock() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference snkRef = FirebaseDatabase.getInstance().getReference("sneakers");
        for (DetalleCarrito item : cartList) {
            String tallaKey = item.getTallaElegida().replace(".", "_");
            snkRef.child(item.getProductId()).child("sizes").child(tallaKey)
                    .setValue(ServerValue.increment(-item.getCantidad()));
        }
        FirebaseDatabase.getInstance().getReference("cart").child(user.getUid()).removeValue();
    }

    private void resetBoton() {
        btnCheckout.setEnabled(true);
        btnCheckout.setText("Pasar por caja");
    }
}