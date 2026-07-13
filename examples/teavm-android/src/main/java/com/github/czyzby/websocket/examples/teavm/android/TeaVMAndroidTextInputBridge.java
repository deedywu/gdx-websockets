package com.github.czyzby.websocket.examples.teavm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputType;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import java.util.concurrent.ConcurrentHashMap;

/** Android-side dialog bridge used by the TeaVM Android example. */
public final class TeaVMAndroidTextInputBridge {
    private static final int STATE_PENDING = 0;
    private static final int STATE_INPUT = 1;
    private static final int STATE_CANCELED = 2;
    private static final int STATE_ERROR = 3;

    private static final ConcurrentHashMap<Long, Result> RESULTS = new ConcurrentHashMap<Long, Result>();
    private static volatile Activity activity;

    private TeaVMAndroidTextInputBridge() {
    }

    public static void setActivity(final Activity currentActivity) {
        activity = currentActivity;
    }

    public static boolean showTextInput(final long requestId, final String title, final String initialText,
            final String hint) {
        final Activity currentActivity = activity;
        if (currentActivity == null || currentActivity.isFinishing()) {
            RESULTS.put(Long.valueOf(requestId), Result.error());
            return false;
        }

        RESULTS.put(Long.valueOf(requestId), Result.pending());
        currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showTextInputOnUiThread(currentActivity, requestId, title, initialText, hint);
            }
        });
        return true;
    }

    public static int getTextInputState(final long requestId) {
        final Result result = RESULTS.get(Long.valueOf(requestId));
        return result == null ? STATE_PENDING : result.state;
    }

    public static String consumeTextInput(final long requestId) {
        final Result result = RESULTS.remove(Long.valueOf(requestId));
        return result == null || result.text == null ? "" : result.text;
    }

    private static void showTextInputOnUiThread(final Activity currentActivity, final long requestId,
            final String title, final String initialText, final String hint) {
        if (currentActivity.isFinishing()) {
            RESULTS.put(Long.valueOf(requestId), Result.canceled());
            return;
        }

        final EditText editText = new EditText(currentActivity);
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        editText.setText(initialText == null ? "" : initialText);
        editText.setHint(hint == null ? "" : hint);
        editText.setSelectAllOnFocus(false);
        editText.setSelection(editText.getText().length());

        final int horizontalPadding = Math.round(currentActivity.getResources().getDisplayMetrics().density * 20f);
        final FrameLayout container = new FrameLayout(currentActivity);
        container.setPadding(horizontalPadding, 0, horizontalPadding, 0);
        container.addView(editText, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final AlertDialog dialog = new AlertDialog.Builder(currentActivity)
                .setTitle(title == null || title.length() == 0 ? "Text Input" : title)
                .setView(container)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int which) {
                        RESULTS.put(Long.valueOf(requestId), Result.input(editText.getText().toString()));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int which) {
                        RESULTS.put(Long.valueOf(requestId), Result.canceled());
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(final DialogInterface dialogInterface) {
                        RESULTS.put(Long.valueOf(requestId), Result.canceled());
                    }
                })
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                editText.requestFocus();
                final Window window = dialog.getWindow();
                if (window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        dialog.show();
    }

    private static final class Result {
        private final int state;
        private final String text;

        private Result(final int state, final String text) {
            this.state = state;
            this.text = text;
        }

        private static Result pending() {
            return new Result(STATE_PENDING, "");
        }

        private static Result input(final String text) {
            return new Result(STATE_INPUT, text == null ? "" : text);
        }

        private static Result canceled() {
            return new Result(STATE_CANCELED, "");
        }

        private static Result error() {
            return new Result(STATE_ERROR, "");
        }
    }
}
