package com.example.gym_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

final class DeleteConfirmationDialog {

    private DeleteConfirmationDialog() {
    }

    static void show(
            @NonNull IronxActivity activity,
            @NonNull String itemName,
            @NonNull Runnable onConfirm
    ) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.catalog_delete_title)
                .setMessage(activity.getString(
                        R.string.catalog_delete_message,
                        itemName
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.delete,
                        (dialog, which) -> onConfirm.run()
                )
                .show();
    }
}
