/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.ui.view;

import ec.nbdemetra.ui.ThemeSupport;
import ec.tstoolkit.data.IReadDataBlock;
import ec.tstoolkit.data.Periodogram;
import ec.tstoolkit.data.Values;
import ec.ui.ATsView;
import ec.ui.chart.TsCharts;
import ec.util.chart.ColorScheme.KnownColor;
import ec.util.chart.swing.ChartCommand;
import ec.util.chart.swing.Charts;
import ec.util.chart.swing.ext.MatrixChartCommand;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JMenu;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;

/**
 *
 * @author Jeremy Demortier
 */
public abstract class ARPView extends ATsView {

    // CONSTANTS
    protected static final double TWO_PI = 2 * Math.PI;
    protected static final Stroke FREQ_MARKER_STROKE = new BasicStroke(5.0f);
    protected static final float FREQ_MARKER_ALPHA = .4f;
    // OTHER
    protected final ChartPanel chartPanel;
    protected ARPData data = null;

    protected ARPView() {
        this.chartPanel = new ChartPanel(createARPChart());
        Charts.avoidScaling(chartPanel);
        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);

        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);

        setTransferHandler(new TsTransferHandler());
        chartPanel.setPopupMenu(buildMenu().getPopupMenu());
    }

    public void reset() {
        XYPlot plot = getPlot();
        plot.clearDomainMarkers();
        plot.clearRangeMarkers();

        if (data == null) {
            plot.setDataset(Charts.emptyXYDataset());
            chartPanel.getChart().getTitle().setText("");
        }
    }

    //<editor-fold defaultstate="collapsed" desc="EVENT HANDLERS">
    @Override
    protected void onTsChange() {
        if (m_ts == null) {
            return;
        }
        
        switch (m_ts.hasData()) {
            case Undefined:
                setData("Loading " + m_ts.getName(), 0, null);
                break;
            case Valid:
                setData(m_ts.getName(), m_ts.getTsData().getFrequency().intValue(), m_ts.getTsData().getValues());
                break;
        }
    }
    
    @Override
    protected void onDataFormatChange() {
        // do nothing?
    }
    
    @Override
    protected void onColorSchemeChange() {
        XYPlot plot = getPlot();
        plot.setBackgroundPaint(themeSupport.getPlotColor());
        plot.setDomainGridlinePaint(themeSupport.getGridColor());
        plot.setRangeGridlinePaint(themeSupport.getGridColor());
        chartPanel.getChart().setBackgroundPaint(themeSupport.getBackColor());
        
        plot.getRenderer().setBasePaint(themeSupport.getLineColor(KnownColor.BROWN));
        
        List<Marker> markers = new ArrayList<>();
        Collection rm = plot.getRangeMarkers(Layer.FOREGROUND);
        if (rm != null) {
            markers.addAll(rm);
        }
        Collection dm = plot.getDomainMarkers(Layer.FOREGROUND);
        if (dm != null) {
            markers.addAll(dm);
        }
        for (Marker o : markers) {
            if (o instanceof ExtValueMarker) {
                ((ExtValueMarker) o).applyColorScheme(themeSupport);
            }
        }
    }
    
    protected void onARPDataChange() {
        XYPlot plot = getPlot();
        reset();
        if (data == null || data.values == null)
            return;
        
        XYSeries series = computeSeries();
        
        plot.setDataset(new XYSeriesCollection(series));
        chartPanel.getChart().getTitle().setText(data.name);
        
        if (data.freq > 0) {
            int freq2 = data.freq / 2;
            
            for (int i = 1; i <= freq2; ++i) {
                double f = i * TWO_PI / data.freq;
                addFreqMarker(f, KnownColor.BLUE);
            }
            
            double[] tdfreq = Periodogram.getTradingDaysFrequencies(data.freq);
            if (tdfreq != null) {
                for (int i = 0; i < tdfreq.length; ++i) {
                    addFreqMarker(tdfreq[i], KnownColor.RED);
                }
            }
            configureChart(series);
        }
    }
    //</editor-fold>

    abstract protected XYSeries computeSeries();

    protected XYPlot getPlot() {
        return chartPanel.getChart().getXYPlot();
    }

    protected void addFreqMarker(double f, KnownColor basicColor) {
        ExtValueMarker vm = new ExtValueMarker(f, basicColor);
        vm.setStroke(FREQ_MARKER_STROKE);
        vm.setAlpha(FREQ_MARKER_ALPHA);
        getPlot().addDomainMarker(vm);
    }

    protected void configureChart(XYSeries series) {
        // Configure range axis
        double max = series.getMaxY();
        double min = series.getMinY();
        double inset = 3 * (max - min) / 100;
        getPlot().getRangeAxis().setRange(min - inset, max + inset);

        // Configure domain axis
        NumberAxis na = (NumberAxis) getPlot().getDomainAxis();
        na.setRange(0, Math.PI);
        na.setTickUnit(new PiNumberTickUnit(Math.PI / 2));
    }

    protected JMenu buildMenu() {
        JMenu result = new JMenu();

        result.add(MatrixChartCommand.copySeries(0, 0).toAction(chartPanel)).setText("Copy series");

        JMenu export = new JMenu("Export image to");
        export.add(ChartCommand.printImage().toAction(chartPanel)).setText("Printer...");
        export.add(ChartCommand.copyImage().toAction(chartPanel)).setText("Clipboard");
        export.add(ChartCommand.saveImage().toAction(chartPanel)).setText("File...");
        result.add(export);

        return result;
    }

    public void setData(String name, int freq, IReadDataBlock values) {
        data = new ARPData(name, freq, values);
        onARPDataChange();
        onColorSchemeChange();
    }

    static JFreeChart createARPChart() {
        JFreeChart result = ChartFactory.createXYLineChart("", "", "", Charts.emptyXYDataset(), PlotOrientation.VERTICAL, false, false, false);
        result.setPadding(TsCharts.CHART_PADDING);
        result.getTitle().setFont(TsCharts.CHART_TITLE_FONT);

        XYPlot plot = result.getXYPlot();

        plot.setNoDataMessage("Drop data here");
        plot.getRangeAxis().setTickLabelPaint(TsCharts.CHART_TICK_LABEL_COLOR);
        plot.getDomainAxis().setTickLabelPaint(TsCharts.CHART_TICK_LABEL_COLOR);

        ((XYLineAndShapeRenderer) plot.getRenderer()).setAutoPopulateSeriesPaint(false);

        return result;
    }

    protected static class ARPData {

        final String name;
        final int freq;
        final Values values;

        ARPData(String name, int freq, IReadDataBlock values) {
            this.name = name;
            this.freq = freq;
            this.values = new Values(values);
        }
    }

    protected static class ExtValueMarker extends ValueMarker {

        final KnownColor color;

        public ExtValueMarker(double value, KnownColor color) {
            super(value);
            this.color = color;
        }

        void applyColorScheme(ThemeSupport support) {
            setPaint(support.getAreaColor(color));
        }
    }
}
