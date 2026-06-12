package com.example.gym_app;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AppInfoActivity extends IronxActivity {

    private final DateFormat dateFormat =
            new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.GERMANY);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_info);
        applyWindowInsets();

        findViewById(R.id.btnBackAppInfo).setOnClickListener(v -> finish());
        populateAppInformation();
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
