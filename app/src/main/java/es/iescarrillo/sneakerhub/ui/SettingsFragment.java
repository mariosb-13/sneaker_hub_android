package es.iescarrillo.sneakerhub.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import es.iescarrillo.sneakerhub.R;

public class SettingsFragment extends Fragment {

    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchNotifications;
    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        switchNotifications = view.findViewById(R.id.switchNotifications);
        TextView tvTerms = view.findViewById(R.id.tvTerms);
        TextView tvContact = view.findViewById(R.id.tvContact);

        View btnCancel = view.findViewById(R.id.btnCancel);
        View btnSave = view.findViewById(R.id.btnSave);

        sharedPreferences = requireActivity().getSharedPreferences("AppConfig", Context.MODE_PRIVATE);

        // --- CARGAR ESTADOS ACTUALES ---
        boolean isDarkMode = sharedPreferences.getBoolean("DARK_MODE", false);
        switchDarkMode.setChecked(isDarkMode);

        boolean isNotifEnabled = sharedPreferences.getBoolean("NOTIFICATIONS", true);
        switchNotifications.setChecked(isNotifEnabled);

        // --- BOTÓN CANCELAR ---
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                // Volvemos atrás sin guardar cambios
                getParentFragmentManager().popBackStack();
            });
        }

        // --- BOTÓN GUARDAR ---
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("DARK_MODE", switchDarkMode.isChecked());
                editor.putBoolean("NOTIFICATIONS", switchNotifications.isChecked());
                editor.apply();

                // Aplicar el tema seleccionado al instante
                if (switchDarkMode.isChecked()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }

                Toast.makeText(getContext(), "Configuración guardada", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            });
        }

        // Soporte y Legal
        tvTerms.setOnClickListener(v -> Toast.makeText(getContext(), "Términos y condiciones...", Toast.LENGTH_SHORT).show());
        tvContact.setOnClickListener(v -> Toast.makeText(getContext(), "Contactando con soporte...", Toast.LENGTH_SHORT).show());
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