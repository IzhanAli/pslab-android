package org.fossasia.pslab.experimentsetup;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.fossasia.pslab.R;
import org.fossasia.pslab.communication.ScienceLab;
import org.fossasia.pslab.others.ScienceLabCommon;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by viveksb007 on 22/7/17.
 */

public class TransistorCBSetup extends Fragment {

    private static final String ERROR_MESSAGE = "Invalid Value";
    private LineChart outputChart;
    private float initialVoltage = 0;
    private float finalVoltage = 0;
    private float emitterVoltage = 0;
    private float stepVoltage = 0;
    private float resistance = 560;
    private int totalSteps = 0;
    private ScienceLab scienceLab = ScienceLabCommon.scienceLab;
    private final Object lock = new Object();
    private ArrayList<Float> x = new ArrayList<>();
    private ArrayList<Float> y = new ArrayList<>();

    public static TransistorCBSetup newInstance() {
        TransistorCBSetup transistorCBSetup = new TransistorCBSetup();
        return transistorCBSetup;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // reusing the layout consisting Configure button and graph
        View view = inflater.inflate(R.layout.diode_setup, container, false);
        outputChart = (LineChart) view.findViewById(R.id.line_chart);
        Button btnConfigure = (Button) view.findViewById(R.id.btn_configure_dialog);
        btnConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                        .title("Configure Experiment")
                        .customView(R.layout.transistor_cb_configure_dialog, true)
                        .positiveText("Start Experiment")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                View customView = dialog.getCustomView();
                                assert customView != null;
                                TextInputEditText etInitialVoltage = (TextInputEditText) customView.findViewById(R.id.et_initial_voltage);
                                TextInputEditText etFinalVoltage = (TextInputEditText) customView.findViewById(R.id.et_final_voltage);
                                TextInputEditText etTotalSteps = (TextInputEditText) customView.findViewById(R.id.et_total_steps);
                                TextInputEditText etEmitterVoltage = (TextInputEditText) customView.findViewById(R.id.et_emitter_voltage);
                                TextInputLayout tilInitialVoltage = (TextInputLayout) customView.findViewById(R.id.text_input_layout_iv);
                                TextInputLayout tilFinalVoltage = (TextInputLayout) customView.findViewById(R.id.text_input_layout_fv);
                                TextInputLayout tilTotalSteps = (TextInputLayout) customView.findViewById(R.id.text_input_layout_total_steps);
                                TextInputLayout tilEmitterVoltage = (TextInputLayout) customView.findViewById(R.id.text_input_layout_voltage);
                                if (TextUtils.isEmpty(etInitialVoltage.getText().toString())) {
                                    tilInitialVoltage.setError(ERROR_MESSAGE);
                                    return;
                                } else
                                    tilInitialVoltage.setError(null);
                                if (TextUtils.isEmpty(etFinalVoltage.getText().toString())) {
                                    tilFinalVoltage.setError(ERROR_MESSAGE);
                                    return;
                                } else
                                    tilFinalVoltage.setError(null);
                                if (TextUtils.isEmpty(etTotalSteps.getText().toString())) {
                                    tilTotalSteps.setError(ERROR_MESSAGE);
                                    return;
                                } else
                                    tilTotalSteps.setError(null);
                                if (TextUtils.isEmpty(etEmitterVoltage.getText().toString())) {
                                    tilEmitterVoltage.setError(ERROR_MESSAGE);
                                    return;
                                } else
                                    tilEmitterVoltage.setError(null);
                                initialVoltage = Float.parseFloat(etInitialVoltage.getText().toString());
                                finalVoltage = Float.parseFloat(etFinalVoltage.getText().toString());
                                totalSteps = Integer.parseInt(etTotalSteps.getText().toString());
                                emitterVoltage = Float.parseFloat(etEmitterVoltage.getText().toString());
                                stepVoltage = (finalVoltage - initialVoltage) / totalSteps;
                                startExperiment();
                            }
                        })
                        .negativeText("Cancel")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .autoDismiss(false)
                        .build();
                dialog.show();
            }
        });
        chartInit();
        return view;
    }

    private void chartInit() {
        outputChart.setTouchEnabled(true);
        outputChart.setDragEnabled(true);
        outputChart.setScaleEnabled(true);
        outputChart.setPinchZoom(true);
        LineData data = new LineData();
        outputChart.setData(data);
    }

    private void startExperiment() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                scienceLab.setPV2(emitterVoltage);
                for (float i = initialVoltage; i < finalVoltage; i += stepVoltage) {
                    CalcDataPoints dataPoint = new CalcDataPoints(i);
                    dataPoint.execute();
                    synchronized (lock) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        updateChart();
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    private void updateChart() {
        Log.v("X-AXIS", x.toString());
        Log.v("Y-AXIS", y.toString());
        List<ILineDataSet> dataSets = new ArrayList<>();
        List<Entry> temp = new ArrayList<>();
        for (int i = 0; i < x.size(); i++) {
            temp.add(new Entry(x.get(i), y.get(i)));
        }
        LineDataSet dataSet = new LineDataSet(temp, "CB Characteristics");
        dataSet.setColor(Color.RED);
        dataSet.setDrawValues(false);
        dataSet.setDrawCircles(false);
        dataSets.add(dataSet);
        outputChart.setData(new LineData(dataSets));
        outputChart.invalidate();
    }

    private class CalcDataPoints extends AsyncTask<Void, Void, Void> {

        private float voltage;

        CalcDataPoints(float volt) {
            this.voltage = volt;
        }

        @Override
        protected Void doInBackground(Void... params) {
            scienceLab.setPV1(voltage);
            float readVoltage = (float) scienceLab.getVoltage("CH1", 5);
            x.add(readVoltage);
            y.add((voltage - readVoltage) / resistance);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            synchronized (lock) {
                lock.notify();
            }
        }
    }

}