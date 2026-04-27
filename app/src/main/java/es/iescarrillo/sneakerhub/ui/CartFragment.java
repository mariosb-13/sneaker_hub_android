package es.iescarrillo.sneakerhub.ui;

import android.app.AlertDialog;
import android.os.Bundle;
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

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private TextView tvTotalPrice, tvEmptyMessage;
    private Button btnCheckout;
    private LinearLayout llEmptyCart;
    private ConstraintLayout layoutLoadingCart;

    private List<DetalleCarrito> cartList;
    private double totalAcumulado = 0;

    private PaymentSheet paymentSheet;
    private FirebaseFunctions mFunctions;
    private String paymentIntentClientSecret;
    private FirebaseFirestore dbFirestore;

    private String tempStreet, tempCity, tempZip, tempDoor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PaymentConfiguration.init(requireContext(), "pk_test_51TJ7gdAVMjolobzNIGzBUR8GhTClqe3tkieFZWB4RhxBMShwX1u1UsMqZqyRrogtfGWH5A2z7nSLkRc5C8NJ6cW400sMfwNlbL");
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);
        mFunctions = FirebaseFunctions.getInstance("europe-west1");
        dbFirestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), "firestore");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCartItems = view.findViewById(R.id.rvCart);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        btnCheckout = view.findViewById(R.id.btnCheckout);
        llEmptyCart = view.findViewById(R.id.llEmptyCart);
        layoutLoadingCart = view.findViewById(R.id.layoutLoadingCart);
        tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);

        cartList = new ArrayList<>();
        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CartAdapter(cartList, new CartAdapter.OnCartItemChangeListener() {
            @Override
            public void onItemChanged(DetalleCarrito item) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("cart")
                            .child(user.getUid()).child(item.getDetalleCartId());
                    if (item.getCantidad() <= 0) ref.removeValue();
                    else ref.child("cantidad").setValue(item.getCantidad());
                }
            }

            @Override
            public void onItemDeleted(DetalleCarrito item) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user != null) {
                    FirebaseDatabase.getInstance().getReference("cart")
                            .child(user.getUid()).child(item.getDetalleCartId()).removeValue();
                }
            }
        });
        rvCartItems.setAdapter(adapter);

        escucharCarrito();

        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> verificarStockAntesDeContinuar());
        }
    }

    private void escucharCarrito() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            if (layoutLoadingCart != null) layoutLoadingCart.setVisibility(View.GONE);
            rvCartItems.setVisibility(View.GONE);
            if (tvEmptyMessage != null) tvEmptyMessage.setText("Inicia sesión para usar el carrito");
            llEmptyCart.setVisibility(View.VISIBLE);
            if (btnCheckout != null) btnCheckout.setEnabled(false);
            tvTotalPrice.setText("0.00 €");
            return;
        }

        if (tvEmptyMessage != null) tvEmptyMessage.setText("Tu carrito está vacío");

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
                        tvTotalPrice.setText(String.format(Locale.getDefault(), "%.2f €", totalAcumulado));

                        boolean vacio = cartList.isEmpty();
                        rvCartItems.setVisibility(vacio ? View.GONE : View.VISIBLE);
                        llEmptyCart.setVisibility(vacio ? View.VISIBLE : View.GONE);
                        btnCheckout.setEnabled(!vacio);

                        if (layoutLoadingCart != null) layoutLoadingCart.setVisibility(View.GONE);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

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
                                if (!errorStock[0]) comprobarDireccionAntesDelPago();
                                else resetBoton();
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { resetBoton(); }
                    });
        }
    }

    private void comprobarDireccionAntesDelPago() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        btnCheckout.setText("Comprobando envío...");
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                DataSnapshot addressNode = snapshot.child("address");
                if (!addressNode.exists()) {
                    mostrarDialogoDireccion();
                    return;
                }
                tempZip = getSafeString(addressNode.child("zipCode"));
                tempCity = getSafeString(addressNode.child("city"));
                tempStreet = getSafeString(addressNode.child("street"));
                tempDoor = getSafeString(addressNode.child("door"));

                if (tempZip == null || tempCity == null || tempStreet == null || tempZip.trim().isEmpty()) {
                    mostrarDialogoDireccion();
                } else {
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
                Toast.makeText(getContext(), "Datos obligatorios faltantes", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                solicitarPagoStripe();
            }
        });
    }

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
                            // Fallo al generar el clientSecret
                            irPantallaCancelado();
                        }
                    } else {
                        // Fallo en la llamada a Cloud Functions
                        irPantallaCancelado();
                    }
                });
    }

    private void onPaymentSheetResult(final PaymentSheetResult result) {
        if (result instanceof PaymentSheetResult.Completed) {
            procederAGuardarPedido(tempStreet, tempCity, tempZip, tempDoor);
        } else {
            // Si el usuario cierra el pop-up, cancela o la tarjeta falla
            irPantallaCancelado();
        }
    }

    private void procederAGuardarPedido(String street, String city, String zip, String door) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (layoutLoadingCart != null) layoutLoadingCart.setVisibility(View.VISIBLE);
        if (btnCheckout != null) btnCheckout.setText("Procesando pedido...");

        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(user.getUid());
        String orderId = ordersRef.push().getKey();

        // Generamos y enviamos el correo ANTES de vaciar el carrito
        enviarCorreoFirestore(user, orderId);

        // Preparamos los artículos comprados para guardarlos
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

        // Actualizar dirección en el perfil
        DatabaseReference addressRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid()).child("address");
        Map<String, Object> profileUpdate = new HashMap<>();
        profileUpdate.put("street", street);
        profileUpdate.put("city", city);
        profileUpdate.put("zipCode", zip);
        profileUpdate.put("door", door);
        addressRef.updateChildren(profileUpdate);

        // Guardar pedido en Firebase y navegar al éxito
        ordersRef.child(orderId).setValue(order).addOnCompleteListener(task -> {
            vaciarCarritoYStock();
            irPantallaExito();
        });
    }

    private void enviarCorreoFirestore(FirebaseUser user, String orderId) {
        String emailDestino = user.getEmail();
        if (emailDestino == null || emailDestino.isEmpty()) return;

        try {
            StringBuilder productosHtml = new StringBuilder();
            for (DetalleCarrito item : cartList) {
                String tallaFix = item.getTallaElegida() != null ? item.getTallaElegida().replace("_", ".") : "";
                String precioLinea = String.format(Locale.getDefault(), "%.2f", (item.getPrice() * item.getCantidad()));
                String imageUrl = item.getImageUrl() != null ? item.getImageUrl() : "";
                String nombre = item.getName() != null ? item.getName() : "";

                productosHtml.append("<div style=\"display: flex; align-items: center; border-bottom: 1px solid #f0f0f0; padding: 15px 0;\">")
                        .append("<img src=\"").append(imageUrl).append("\" style=\"width: 70px; height: 70px; object-fit: contain; margin-right: 15px;\" alt=\"").append(nombre).append("\">")
                        .append("<div style=\"flex-grow: 1; text-align: left;\">")
                        .append("<p style=\"margin: 0; font-weight: bold; color: #333333; font-size: 14px;\">").append(nombre).append("</p>")
                        .append("<p style=\"margin: 4px 0 0 0; color: #888888; font-size: 11px;\">Talla: ").append(tallaFix).append(" | Cantidad: ").append(item.getCantidad()).append("</p>")
                        .append("</div>")
                        .append("<div style=\"font-weight: bold; color: #333333; font-size: 14px; white-space: nowrap; margin-left: 10px;\">")
                        .append(precioLinea).append(" €")
                        .append("</div>")
                        .append("</div>");
            }

            String totalStr = String.format(Locale.getDefault(), "%.2f", totalAcumulado);
            String shortOrderId = orderId != null && orderId.length() > 20 ? orderId.substring(1, 20) : orderId;

            String htmlTemplate = "<div style=\"background-color: #fcfcfc; padding: 40px 10px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;\">" +
                    "<div style=\"max-width: 550px; margin: 0 auto; background-color: #ffffff; border-radius: 4px; overflow: hidden; border: 1px solid #eeeeee;\">" +
                    "<div style=\"text-align: center; padding: 40px 30px 30px 30px;\">" +
                    "<img src=\"https://firebasestorage.googleapis.com/v0/b/sneakerhub-3862d.firebasestorage.app/o/SneakerHub.png?alt=media&token=a42e0979-51b2-4a72-ad48-b8a9974ad37a\" alt=\"SneakerHub\" style=\"width: 250px; margin-bottom: 40px;\">" +
                    "<h1 style=\"color: #333333; font-size: 24px; margin: 0 0 15px 0; font-weight: bold;\">¡Gracias por tu compra!</h1>" +
                    "<p style=\"color: #666666; font-size: 15px; margin: 0;\">Tu pedido <strong>#" + shortOrderId + "</strong> se ha procesado correctamente.</p>" +
                    "</div>" +
                    "<div style=\"padding: 0 40px;\">" + productosHtml.toString() + "</div>" +
                    "<div style=\"padding: 30px 40px;\">" +
                    "<div style=\"background-color: #f9f9f9; border: 1px dashed #dddddd; border-radius: 8px; padding: 20px; text-align: right;\">" +
                    "<span style=\"color: #333333; font-size: 18px; margin-right: 10px;\">Total pagado: </span>" +
                    "<strong style=\"color: #000000; font-size: 22px;\">" + totalStr + " €</strong>" +
                    "</div>" +
                    "</div>" +
                    "<div style=\"text-align: center; padding: 0 40px 40px 40px;\">" +
                    "<p style=\"color: #888888; font-size: 13px; margin-bottom: 30px;\">En breve te enviaremos la información de seguimiento.</p>" +
                    "<a href=\"https://sneakerhub.com\" style=\"background-color: #000000; color: #ffffff; text-decoration: none; padding: 15px 40px; font-weight: bold; font-size: 13px; border-radius: 4px; display: inline-block; text-transform: uppercase;\">SEGUIR COMPRANDO</a>" +
                    "</div>" +
                    "</div>" +
                    "</div>";

            Map<String, Object> mailMap = new HashMap<>();
            mailMap.put("to", emailDestino);
            Map<String, Object> message = new HashMap<>();
            message.put("subject", "¡Gracias por tu compra en SneakerHub! 🚀");
            message.put("html", htmlTemplate);
            mailMap.put("message", message);

            // Disparar en segundo plano y olvidarse (Fire and forget)
            dbFirestore.collection("mail").add(mailMap);

        } catch (Exception e) {
        }
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

    private void irPantallaExito() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new SuccessFragment(), null);
        }
    }

    private void irPantallaCancelado() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new CancelFragment(), null);
        }
    }

    private void resetBoton() {
        btnCheckout.setEnabled(true);
        btnCheckout.setText("Pasar por caja");
    }
}