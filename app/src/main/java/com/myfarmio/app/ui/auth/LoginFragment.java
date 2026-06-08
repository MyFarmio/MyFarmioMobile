package com.myfarmio.app.ui.auth;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.myfarmio.app.R;

public class LoginFragment extends Fragment {

    private LoginViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText inputEmail = view.findViewById(R.id.input_email);
        TextInputEditText inputPassword = view.findViewById(R.id.input_password);
        MaterialButton buttonSubmit = view.findViewById(R.id.button_submit);
        LinearLayout buttonGoogle = view.findViewById(R.id.button_google);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.isLoading.observe(getViewLifecycleOwner(), loading -> {
            boolean busy = loading != null && loading;
            buttonSubmit.setEnabled(!busy);
            buttonGoogle.setEnabled(!busy);
            buttonSubmit.setText(busy ? "" : getString(R.string.enter_system));
            buttonSubmit.setIcon(busy ? null : AppCompatResources.getDrawable(requireContext(), R.drawable.ic_flecha_derecha_circulo));
            buttonSubmit.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_END);
            buttonSubmit.setIconPadding(10);
            buttonSubmit.setIconSize(20);
        });

        viewModel.errorMessage.observe(getViewLifecycleOwner(), msg -> {
            View errorView = view.findViewById(R.id.text_error_general);
            if (msg != null && !msg.isEmpty()) {
                errorView.setVisibility(View.VISIBLE);
                ((android.widget.TextView) errorView).setText(msg);
            } else {
                errorView.setVisibility(View.GONE);
            }
        });

        viewModel.loginSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                NavController navController = Navigation.findNavController(view);
                navController.navigate(R.id.action_login_to_main);
            }
        });

        buttonSubmit.setOnClickListener(v -> {
            hideKeyboard();

            String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
            String password = inputPassword.getText() != null ? inputPassword.getText().toString() : "";

            if (email.isEmpty() || password.isEmpty()) {
                viewModel.errorMessage.setValue(getString(R.string.login_error_fields));
                return;
            }

            viewModel.login(email, password, requireContext());
        });

        buttonGoogle.setOnClickListener(v ->
            Toast.makeText(requireContext(), getString(R.string.google_button_soon), Toast.LENGTH_SHORT).show()
        );
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View focused = getActivity() != null ? getActivity().getCurrentFocus() : null;
        if (focused != null && imm != null) {
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }
}
