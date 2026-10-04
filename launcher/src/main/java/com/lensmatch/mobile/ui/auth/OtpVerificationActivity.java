package com.lensmatch.mobile.ui.auth;

import android.content.Intent; 
import android.graphics.Color; 
import android.os.Bundle; 
import android.os.CountDownTimer; 
import android.text.Editable; 
import android.text.TextWatcher; 
import android.view.View; 
import android.widget.ProgressBar; 
import android.widget.TextView; 
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity; 
import androidx.appcompat.app.AppCompatDelegate; 
import androidx.core.view.WindowCompat; 
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.appbar.MaterialToolbar; 
import com.google.android.material.button.MaterialButton; 
import com.google.android.material.textfield.TextInputEditText; 
import com.google.firebase.auth.FirebaseAuth; 
import com.google.firebase.auth.FirebaseUser; 
import com.lensmatch.mobile.R; 
import com.lensmatch.mobile.data.AppState; 
import com.lensmatch.mobile.service.AuthApiService; 
import com.lensmatch.mobile.ui.MainActivity; 
import com.lensmatch.mobile.ui.guidelines.AppGuidelinesActivity; 
import com.lensmatch.mobile.utils.StatusBarUtils;

import java.util.Locale;

public class OtpVerificationActivity extends AppCompatActivity {
    private TextView tvEmailSubtitle; 
    private TextInputEditText etOtpCode; 
    private TextView tvResendTimer; 
    private TextView btnResendOtp; 
    private ProgressBar progressOtp; 
    private MaterialButton btnVerifyOtp; 
    private CountDownTimer countDownTimer; 
    private static final long RESEND_COOLDOWN_MS = 60000; // 60 seconds 
    
    @Override 
    protected void onCreate(Bundle savedInstanceState) { 
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO); 
        super.onCreate(savedInstanceState); 
        setContentView(R.layout.activity_otp_verification); 
        View root = findViewById(R.id.otp_root); 
        StatusBarUtils.applyWindowInsets(root); 
        getWindow().setStatusBarColor(Color.WHITE); 
        getWindow().setNavigationBarColor(Color.WHITE); 
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()); 
        if (controller != null) { 
            controller.setAppearanceLightStatusBars(true); 
            controller.setAppearanceLightNavigationBars(true); 
        } 
        MaterialToolbar toolbar = findViewById(R.id.toolbar_otp); 
        toolbar.setNavigationOnClickListener(v -> handleCancelAndReturn()); 
        
        tvEmailSubtitle = findViewById(R.id.tv_otp_email_subtitle); 
        etOtpCode = findViewById(R.id.et_otp_code); 
        tvResendTimer = findViewById(R.id.tv_resend_timer); 
        btnResendOtp = findViewById(R.id.btn_resend_otp); 
        progressOtp = findViewById(R.id.progress_otp); 
        btnVerifyOtp = findViewById(R.id.btn_verify_otp); 
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); 
        if (user != null && user.getEmail() != null) { 
            tvEmailSubtitle.setText("We sent a 6-digit verification code to " + user.getEmail() + ". Please enter it below to complete login."); 
        } 
        
        etOtpCode.addTextChangedListener(new TextWatcher() { 
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {} 
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                boolean isValid = s != null && s.toString().trim().length() == 6; 
                btnVerifyOtp.setEnabled(isValid); 
            } 
            @Override public void afterTextChanged(Editable s) {} 
        }); 
        
        btnVerifyOtp.setOnClickListener(v -> executeVerifyOtp()); 
        btnResendOtp.setOnClickListener(v -> executeResendOtp()); 
        startResendTimer(); 
        
        // If activity was launched without a pre-sent OTP request (e.g. app restart bypass check), request fresh OTP 
        boolean autoSend = getIntent().getBooleanExtra("auto_send_otp", false); 
        if (autoSend) { 
            executeSendOtpInitial(); 
        } 
    } 
    
    private void startResendTimer() { 
        if (btnResendOtp != null) btnResendOtp.setVisibility(View.GONE); 
        if (tvResendTimer != null) tvResendTimer.setVisibility(View.VISIBLE); 
        if (countDownTimer != null) countDownTimer.cancel(); 
        countDownTimer = new CountDownTimer(RESEND_COOLDOWN_MS, 1000) { 
            @Override public void onTick(long millisUntilFinished) { 
                long totalSeconds = millisUntilFinished / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                if (tvResendTimer != null) { 
                    tvResendTimer.setText(String.format(Locale.US, "Resend code in %02d:%02d", minutes, seconds)); 
                } 
            } 
            @Override public void onFinish() { 
                if (tvResendTimer != null) tvResendTimer.setVisibility(View.GONE); 
                if (btnResendOtp != null) btnResendOtp.setVisibility(View.VISIBLE); 
            } 
        }.start(); 
    } 
    
    private void executeSendOtpInitial() { 
        setLoading(true); 
        AuthApiService.sendOtp(new AuthApiService.Callback<Void>() { 
            @Override public void onSuccess(Void result) { 
                if (isFinishing() || isDestroyed()) return; 
                setLoading(false); 
                Toast.makeText(OtpVerificationActivity.this, "Verification code sent to your email.", Toast.LENGTH_SHORT).show(); 
            } 
            @Override public void onError(String errorMessage) { 
                if (isFinishing() || isDestroyed()) return; 
                setLoading(false); 
                Toast.makeText(OtpVerificationActivity.this, errorMessage != null ? errorMessage : "Failed to send verification code.", Toast.LENGTH_LONG).show(); 
            } 
        }); 
    } 
    
    private void executeResendOtp() { 
        setLoading(true); 
        AuthApiService.resendOtp(new AuthApiService.Callback<Void>() { 
            @Override public void onSuccess(Void result) { 
                if (isFinishing() || isDestroyed()) return; 
                setLoading(false); 
                if (etOtpCode != null) etOtpCode.setText(""); 
                startResendTimer(); 
                Toast.makeText(OtpVerificationActivity.this, "A new verification code has been sent to your email.", Toast.LENGTH_SHORT).show(); 
            } 
            @Override public void onError(String errorMessage) { 
                if (isFinishing() || isDestroyed()) return; 
                setLoading(false); 
                Toast.makeText(OtpVerificationActivity.this, errorMessage != null ? errorMessage : "Failed to resend code.", Toast.LENGTH_LONG).show(); 
            } 
        }); 
    } 
    
    private void executeVerifyOtp() { 
        String code = etOtpCode.getText() != null ? etOtpCode.getText().toString().trim() : ""; 
        if (code.length() != 6) return; 
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser(); 
        if (user == null || user.getUid() == null) { 
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show(); 
            handleCancelAndReturn(); 
            return; 
        } 
        setLoading(true); 
        AuthApiService.verifyOtp(code, new AuthApiService.Callback<Boolean>() { 
            @Override public void onSuccess(Boolean success) { 
                if (isFinishing() || isDestroyed()) return; 
                setLoading(false); 
                AppState.getInstance().setOtpVerifiedUid(user.getUid()); 
                Toast.makeText(OtpVerificationActivity.this, "Email verified successfully!", Toast.LENGTH_SHORT).show(); 
                proceedToMain(); 
            } 
            @Override public void onError(String errorMessage) { 
                if (isFinishing() || isDestroyed()) return; 
                setLoading(false); 
                Toast.makeText(OtpVerificationActivity.this, errorMessage != null ? errorMessage : "Invalid verification code.", Toast.LENGTH_LONG).show(); 
            } 
        }); 
    } 
    
    private void proceedToMain() { 
        if (!AppState.getInstance().hasSeenGuidelines()) { 
            Intent intent = new Intent(this, AppGuidelinesActivity.class); 
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); 
            startActivity(intent); 
            finish(); 
            return; 
        } 
        AppState.getInstance().setLastActiveTab(R.id.nav_home); 
        Intent intent = new Intent(this, MainActivity.class); 
        intent.putExtra("open_tab", R.id.nav_home); 
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); 
        startActivity(intent); 
        finish(); 
    } 
    
    private void handleCancelAndReturn() { 
        Intent intent = new Intent(this, LoginActivity.class); 
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); 
        startActivity(intent); 
        finish(); 
    } 
    
    private void setLoading(boolean loading) { 
        if (progressOtp != null) progressOtp.setVisibility(loading ? View.VISIBLE : View.GONE); 
        if (btnVerifyOtp != null) btnVerifyOtp.setEnabled(!loading && etOtpCode != null && etOtpCode.getText() != null && etOtpCode.getText().length() == 6); 
        if (btnResendOtp != null) btnResendOtp.setEnabled(!loading); 
        if (etOtpCode != null) etOtpCode.setEnabled(!loading); 
    } 
    
    @Override 
    protected void onDestroy() { 
        if (countDownTimer != null) { 
            countDownTimer.cancel(); 
        } 
        super.onDestroy(); 
    } 
    
    @Override 
    public void onBackPressed() { 
        super.onBackPressed(); 
        handleCancelAndReturn(); 
    }
}
