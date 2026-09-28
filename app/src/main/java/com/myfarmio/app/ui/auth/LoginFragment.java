package com.myfarmio.app.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.myfarmio.app.R;
import com.myfarmio.app.ui.common.MobileUi;

public class LoginFragment extends Fragment {
    private LoginViewModel viewModel;
    private MobileUi.Sheet sheet;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_login, parent, false);
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        TextInputEditText email = view.findViewById(R.id.input_email);
        TextInputEditText password = view.findViewById(R.id.input_password);
        TextInputLayout emailLayout = view.findViewById(R.id.login_email_layout);
        TextInputLayout passwordLayout = view.findViewById(R.id.login_password_layout);
        MaterialButton submit = view.findViewById(R.id.button_submit);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            view.findViewById(R.id.button_demo).setEnabled(!busy);
            submit.setEnabled(!busy); email.setEnabled(!busy); password.setEnabled(!busy);
            submit.setText(busy ? "Ingresando…" : "Ingresar");
            view.findViewById(R.id.login_progress).setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
        });
        viewModel.errorMessage.observe(getViewLifecycleOwner(), message -> {
            boolean present = message != null && !message.isEmpty();
            view.findViewById(R.id.text_error_general).setVisibility(present ? View.VISIBLE : View.GONE);
            MobileUi.text(view, R.id.text_error_general, message);
        });
        viewModel.loginSuccess.observe(getViewLifecycleOwner(), success -> {
            if (Boolean.TRUE.equals(success) && Navigation.findNavController(view).getCurrentDestination() != null
                && Navigation.findNavController(view).getCurrentDestination().getId() == R.id.loginFragment) {
                Navigation.findNavController(view).navigate(R.id.action_login_to_main);
            }
        });
        submit.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.isLoading.getValue())) return;
            String mail = email.getText() == null ? "" : email.getText().toString().trim();
            String secret = password.getText() == null ? "" : password.getText().toString();
            emailLayout.setError(null); passwordLayout.setError(null);
            boolean validEmail = Patterns.EMAIL_ADDRESS.matcher(mail).matches();
            if (!validEmail) emailLayout.setError("Ingresá un correo válido.");
            if (secret.isEmpty()) passwordLayout.setError("Ingresá tu contraseña.");
            if (!validEmail || secret.isEmpty()) {
                if (!validEmail) email.requestFocus(); else password.requestFocus();
                return;
            }
            hideKeyboard();
            // Preserve existing login service, session storage and error mapping.
            viewModel.login(mail, secret, requireContext());
        });
        password.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { submit.performClick(); return true; }
            return false;
        });
        view.findViewById(R.id.button_recovery_help).setOnClickListener(v -> {
            hideKeyboard();
            if (sheet != null) sheet.dismiss();
            sheet = new MobileUi.Sheet(requireContext(), "Recuperar acceso");
            sheet.row("Desde la versión web", "La recuperación de contraseña todavía no está conectada en Android. Usá la opción de recuperación de la web de MyFarmio.");
            sheet.row("Tu contraseña", "No se envía ninguna solicitud al abrir esta ayuda.");
            // TODO: Conectar recuperación cuando exista el flujo autorizado en Android.
            sheet.show();
        });
        view.findViewById(R.id.button_demo).setOnClickListener(v -> {
            if (Boolean.TRUE.equals(viewModel.isLoading.getValue())) return;
            emailLayout.setError(null); passwordLayout.setError(null);
            hideKeyboard();
            // Uses the pre-existing demo branch; never simulates a real account login.
            viewModel.login(LoginViewModel.DEMO_EMAIL, LoginViewModel.DEMO_PASSWORD, requireContext());
        });
        MobileUi.enter(submit);
    }
    private void hideKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = requireActivity().getCurrentFocus();
        if (keyboard != null && focused != null) keyboard.hideSoftInputFromWindow(focused.getWindowToken(), 0);
    }
    @Override public void onDestroyView() {
        if (sheet != null) { sheet.dismiss(); sheet = null; }
        super.onDestroyView();
    }
}
