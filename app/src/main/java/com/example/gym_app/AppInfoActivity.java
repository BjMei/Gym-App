package com.example.gym_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppInfoActivity extends IronxActivity {

    private final DateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY);
    private final ExecutorService backupExecutor = Executors.newSingleThreadExecutor();
    private final ActivityResultLauncher<String> exportLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.CreateDocument("application/json"),
                    this::exportBackup
            );
    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.OpenDocument(),
                    this::prepareImport
            );

    private AppDataBackupManager backupManager;
    private MaterialButton btnExportData;
    private MaterialButton btnImportData;
    private TextView tvBackupStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        applyWindowInsets();

        backupManager = new AppDataBackupManager(this);
        btnExportData = findViewById(R.id.btnExportData);
        btnImportData = findViewById(R.id.btnImportData);
        tvBackupStatus = findViewById(R.id.tvBackupStatus);

        findViewById(R.id.btnBackAppInfo).setOnClickListener(v -> finish());
        btnExportData.setOnClickListener(v -> exportLauncher.launch(createBackupFileName()));
        btnImportData.setOnClickListener(v ->
                importLauncher.launch(new String[]{"application/json", "text/json", "text/plain"})
        );
        populateAppInformation();
    }

    @Override
    protected void onDestroy() {
        backupExecutor.shutdown();
        super.onDestroy();
    }

    private void applyWindowInsets() {
        View rootLayout = findViewById(R.id.rootAppInfoLayout);
        int basePaddingLeft = rootLayout.getPaddingLeft();
        int basePaddingTop = rootLayout.getPaddingTop();
        int basePaddingRight = rootLayout.getPaddingRight();
        int basePaddingBottom = rootLayout.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    basePaddingLeft + systemBars.left,
                    basePaddingTop + systemBars.top,
                    basePaddingRight + systemBars.right,
                    basePaddingBottom + systemBars.bottom
            );
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(rootLayout);
    }

    private void populateAppInformation() {
        PackageManager packageManager = getPackageManager();
        ApplicationInfo applicationInfo = getApplicationInfo();

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);

            LinearLayout appCard = findViewById(R.id.appInfoAppCard);
            addInfoRow(appCard, "APP-NAME",
                    packageManager.getApplicationLabel(applicationInfo).toString());
            addInfoRow(appCard, "VERSION",
                    packageInfo.versionName != null ? packageInfo.versionName : "Nicht verfügbar");
            addInfoRow(appCard, "BUILD-NUMMER",
                    String.valueOf(PackageInfoCompat.getLongVersionCode(packageInfo)));
            addInfoRow(appCard, "BUILD-TYP", isDebugBuild(applicationInfo) ? "Debug" : "Release");
            addInfoRow(appCard, "PAKET-ID", getPackageName());

            LinearLayout installationCard = findViewById(R.id.appInfoInstallationCard);
            addInfoRow(installationCard, "ERSTE INSTALLATION",
                    formatTimestamp(packageInfo.firstInstallTime));
            addInfoRow(installationCard, "LETZTE AKTUALISIERUNG",
                    formatTimestamp(packageInfo.lastUpdateTime));

            LinearLayout deviceCard = findViewById(R.id.appInfoDeviceCard);
            addInfoRow(deviceCard, "ANDROID",
                    String.format(
                            Locale.GERMANY,
                            "%s (API %d)",
                            Build.VERSION.RELEASE,
                            Build.VERSION.SDK_INT
                    ));
            addInfoRow(deviceCard, "GERÄT", buildDeviceName());
            addInfoRow(deviceCard, "ARCHITEKTUR",
                    Build.SUPPORTED_ABIS.length > 0
                            ? Build.SUPPORTED_ABIS[0]
                            : "Nicht verfügbar");
            addInfoRow(deviceCard, "MINDEST-API",
                    String.valueOf(applicationInfo.minSdkVersion));
            addInfoRow(deviceCard, "ZIEL-API",
                    String.valueOf(applicationInfo.targetSdkVersion));

            LinearLayout dataCard = findViewById(R.id.appInfoDataCard);
            addInfoRow(dataCard, "DATENSPEICHERUNG",
                    "Trainings-, Profil- und Einstellungsdaten werden lokal auf diesem Gerät gespeichert.");
            addInfoRow(dataCard, "ANDROID-BACKUP",
                    isBackupAllowed(applicationInfo)
                            ? "Erlaubt. Android kann App-Daten abhängig von den Geräteeinstellungen sichern."
                            : "Für diese App deaktiviert.");
            addInfoRow(dataCard, "INTERNETZUGRIFF",
                    hasInternetPermission(packageManager)
                            ? "Die App besitzt eine Internetberechtigung."
                            : "Keine Internetberechtigung angefordert.");
        } catch (PackageManager.NameNotFoundException exception) {
            LinearLayout appCard = findViewById(R.id.appInfoAppCard);
            addInfoRow(appCard, "APP-INFORMATIONEN",
                    "Paketinformationen konnten nicht geladen werden.");
        }
    }

    private void exportBackup(Uri uri) {
        if (uri == null) {
            return;
        }
        setBackupBusy(true, R.string.app_info_backup_export_running);
        backupExecutor.execute(() -> {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri, "wt");
                if (outputStream == null) {
                    throw new IOException("Die Zieldatei konnte nicht geöffnet werden.");
                }
                AppDataBackupManager.ExportSummary summary =
                        backupManager.exportTo(outputStream);
                runOnUiThread(() -> {
                    setBackupBusy(false, R.string.app_info_backup_export_success);
                    tvBackupStatus.setText(getString(
                            R.string.app_info_backup_export_details,
                            summary.preferenceValues,
                            summary.internalFiles,
                            formatFileSize(summary.jsonBytes)
                    ));
                    Toast.makeText(
                            this,
                            R.string.app_info_backup_export_success,
                            Toast.LENGTH_SHORT
                    ).show();
                });
            } catch (Exception exception) {
                showBackupError(R.string.app_info_backup_export_error, exception);
            }
        });
    }

    private void prepareImport(Uri uri) {
        if (uri == null) {
            return;
        }
        setBackupBusy(true, R.string.app_info_backup_validation_running);
        backupExecutor.execute(() -> {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new IOException("Die Sicherungsdatei konnte nicht geöffnet werden.");
                }
                AppDataBackupManager.PreparedBackup preparedBackup =
                        backupManager.readAndValidate(inputStream);
                runOnUiThread(() -> {
                    setBackupBusy(false, R.string.app_info_backup_ready);
                    showImportConfirmation(preparedBackup);
                });
            } catch (Exception exception) {
                showBackupError(R.string.app_info_backup_import_invalid, exception);
            }
        });
    }

    private void showImportConfirmation(AppDataBackupManager.PreparedBackup preparedBackup) {
        String summary = getString(
                R.string.app_info_backup_import_summary,
                preparedBackup.preferenceValues,
                preparedBackup.internalFiles,
                formatFileSize(preparedBackup.jsonBytes)
        );
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_info_backup_import_confirm_title)
                .setMessage(getString(R.string.app_info_backup_import_confirm_message, summary))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.app_info_backup_import_action, (dialog, which) ->
                        importBackup(preparedBackup)
                )
                .show();
    }

    private void importBackup(AppDataBackupManager.PreparedBackup preparedBackup) {
        setBackupBusy(true, R.string.app_info_backup_import_running);
        backupExecutor.execute(() -> {
            try {
                backupManager.restore(preparedBackup);
                runOnUiThread(this::showImportSuccess);
            } catch (Exception exception) {
                showBackupError(R.string.app_info_backup_import_error, exception);
            }
        });
    }

    private void showImportSuccess() {
        setBackupBusy(false, R.string.app_info_backup_import_success);
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_info_backup_import_success_title)
                .setMessage(R.string.app_info_backup_import_success_message)
                .setCancelable(false)
                .setPositiveButton(R.string.app_info_backup_restart, (dialog, which) ->
                        restartApplication()
                )
                .show();
    }

    private void restartApplication() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finishAffinity();
    }

    private void showBackupError(int messageResource, Exception exception) {
        runOnUiThread(() -> {
            setBackupBusy(false, messageResource);
            String detail = exception.getMessage();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.app_info_backup_error_title)
                    .setMessage(detail == null || detail.trim().isEmpty()
                            ? getString(messageResource)
                            : getString(messageResource) + "\n\n" + detail)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });
    }

    private void setBackupBusy(boolean busy, int statusResource) {
        btnExportData.setEnabled(!busy);
        btnImportData.setEnabled(!busy);
        tvBackupStatus.setText(statusResource);
    }

    private String createBackupFileName() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.ROOT);
        return "IRONX_Backup_" + format.format(new Date()) + ".json";
    }

    private String formatFileSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private void addInfoRow(LinearLayout container, String label, String value) {
        if (container.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(getColor(R.color.divider));
            container.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(1)
            ));
        }

        View row = LayoutInflater.from(this).inflate(R.layout.app_info_row, container, false);
        ((TextView) row.findViewById(R.id.tvInfoLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvInfoValue)).setText(value);
        container.addView(row);
    }

    private String formatTimestamp(long timestamp) {
        return timestamp > 0 ? dateFormat.format(new Date(timestamp)) : "Nicht verfügbar";
    }

    private String buildDeviceName() {
        String manufacturer = capitalize(Build.MANUFACTURER);
        String model = Build.MODEL != null ? Build.MODEL.trim() : "";
        if (model.toLowerCase(Locale.ROOT).startsWith(
                manufacturer.toLowerCase(Locale.ROOT))) {
            return model;
        }
        return (manufacturer + " " + model).trim();
    }

    private String capitalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unbekannt";
        }
        String trimmed = value.trim();
        return trimmed.substring(0, 1).toUpperCase(Locale.GERMANY) + trimmed.substring(1);
    }

    private boolean isDebugBuild(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    private boolean isBackupAllowed(ApplicationInfo applicationInfo) {
        return (applicationInfo.flags & ApplicationInfo.FLAG_ALLOW_BACKUP) != 0;
    }

    private boolean hasInternetPermission(PackageManager packageManager) {
        return packageManager.checkPermission(
                Manifest.permission.INTERNET,
                getPackageName()
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
