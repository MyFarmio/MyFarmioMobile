package com.myfarmio.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.myfarmio.app.auth.SessionManager;

public class LoginFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText editEmail    = view.findViewById(R.id.input_email);
        TextInputEditText editPassword = view.findViewById(R.id.input_password);
        MaterialButton    buttonLogin  = view.findViewById(R.id.button_submit);
        TextView          textError    = view.findViewById(R.id.text_error_general);

        buttonLogin.setOnClickListener(v -> {
            String email    = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            // Ocultar error previo
            textError.setVisibility(View.GONE);

            if (email.isEmpty() || password.isEmpty()) {
                textError.setVisibility(View.VISIBLE);
                textError.setText("Completá todos los campos.");
                return;
            }

            // Usuario hardcodeado para pruebas
            if (email.equals("admin@myfarmio.com") && password.equals("farmio123")) {
                SessionManager.getInstance(requireContext()).saveSession(
                        "fake_token_test",
                        "fake_refresh_test",
                        "user-test-001",
                        email,
                        "My Farmio Admin",
                        "org-test-001",
                        "MyFarmio Test"
                );
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_login_to_main);
            } else {
                textError.setVisibility(View.VISIBLE);
                textError.setText("El correo o la contraseña no son correctos.");
            }
        });
    }
}