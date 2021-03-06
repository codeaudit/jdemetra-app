/*
 * Copyright 2013 National Bank of Belgium
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved 
 * by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * http://ec.europa.eu/idabc/eupl
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and 
 * limitations under the Licence.
 */
package ec.ui.view;

import ec.nbdemetra.ui.ComponentFactory;
import ec.nbdemetra.ui.NbComponents;
import ec.tss.html.HtmlUtil;
import ec.tss.html.implementation.HtmlRevisionsDocument;
import ec.tstoolkit.data.DescriptiveStatistics;
import ec.tstoolkit.timeseries.TsPeriodSelector;
import ec.tstoolkit.timeseries.analysis.DiagnosticInfo;
import ec.tstoolkit.timeseries.analysis.RevisionHistory;
import ec.tstoolkit.timeseries.simplets.TsData;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import ec.ui.AHtmlView;
import ec.ui.ATsView;
import ec.ui.chart.ChartPopup;
import ec.ui.chart.TsCharts;
import ec.util.chart.ColorScheme;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * View of the Revision History of a serie.
 *
 * @author Mats Maggi
 */
public class RevisionSaSeriesView extends ATsView implements ClipboardOwner {

    private static final int S_INDEX = 1;
    private static final int REV_INDEX = 0;
    private final JChartPanel chartpanel_;  // Panel displaying chart
    private final AHtmlView documentpanel_;
    private final XYLineAndShapeRenderer sRenderer;
    private final XYLineAndShapeRenderer revRenderer;
    private final JFreeChart mainChart;
    private String info_ = "sa";
    private RevisionHistory history_;
    private DiagnosticInfo diag_ = DiagnosticInfo.RelativeDifference;
    private int years_ = 4;
    private int minyears_ = 5;
    private int threshold_ = 2;
    private int lastIndexSelected = -1;
    private ChartPopup popup;
    private TsPeriod firstPeriod;
    private TsData sRef;
    private Range range;

    /**
     * Constructs a new view
     */
    public RevisionSaSeriesView() {
        setLayout(new BorderLayout());

        sRenderer = new XYLineAndShapeRenderer();
        sRenderer.setBaseShapesVisible(false);
        //sRenderer.setSeriesStroke(1, new BasicStroke(0.75f, 1, 1, 1.0f, new float[]{2f, 3f}, 0.0f));
        sRenderer.setBasePaint(themeSupport.getLineColor(ColorScheme.KnownColor.RED));

        revRenderer = new XYLineAndShapeRenderer(false, true);

        mainChart = createMainChart();

        chartpanel_ = new JChartPanel(ChartFactory.createLineChart(null, null, null, null, PlotOrientation.VERTICAL, false, false, false));

        documentpanel_ = ComponentFactory.getDefault().newHtmlView();

        JSplitPane splitpane = NbComponents.newJSplitPane(JSplitPane.VERTICAL_SPLIT, chartpanel_, NbComponents.newJScrollPane(documentpanel_));
        splitpane.setDividerLocation(0.5);
        splitpane.setResizeWeight(.5);

        popup = new ChartPopup(null, false);

        chartpanel_.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent e) {
                if (lastIndexSelected != -1) {
                    revRenderer.setSeriesShapesFilled(lastIndexSelected, false);
                }
                if (e.getEntity() != null) {
                    if (e.getEntity() instanceof XYItemEntity) {
                        XYItemEntity item = (XYItemEntity) e.getEntity();
                        if (item.getDataset().equals(mainChart.getXYPlot().getDataset(REV_INDEX))) {
                            int i = item.getSeriesIndex();

                            revRenderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, 6, 6));
                            revRenderer.setSeriesShapesFilled(i, true);
                            revRenderer.setSeriesPaint(i, themeSupport.getLineColor(ColorScheme.KnownColor.BLUE));

                            lastIndexSelected = i;

                            showRevisionPopup(e);
                        }
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
            }
        });

        chartpanel_.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(JChartPanel.ZOOM_SELECTION_CHANGED)) {
                    showSelectionPopup((Rectangle2D) evt.getNewValue());
                }
            }
        });

        this.add(splitpane, BorderLayout.CENTER);
        splitpane.setResizeWeight(0.5);

        onColorSchemeChange();
    }

    private void showSelectionPopup(Rectangle2D rectangle) {
        XYPlot plot = chartpanel_.getChart().getXYPlot();
        Rectangle2D dataArea = chartpanel_.getScreenDataArea();
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        double minX = domainAxis.java2DToValue(rectangle.getMinX(), dataArea, plot.getDomainAxisEdge());
        double maxX = domainAxis.java2DToValue(rectangle.getMaxX(), dataArea, plot.getDomainAxisEdge());

        Date startDate = new Date((long) minX);
        Date endDate = new Date((long) maxX);
        TsPeriod start = new TsPeriod(firstPeriod.getFrequency(), startDate);
        TsPeriod end = new TsPeriod(firstPeriod.getFrequency(), endDate);

        if (end.minus(start) == 0) {
            return;
        }

        TsPeriodSelector sel = new TsPeriodSelector();
        sel.between(start.firstday(), end.lastday());
        List<TsData> listSeries = history_.Select(info_, startDate, endDate);
        List<TsData> revSeries = new ArrayList<>();

        for (TsData t : listSeries) {
            revSeries.add(t.select(sel));
        }

        Point pt = new Point((int) rectangle.getX(), (int) rectangle.getY());
        pt.translate(3, 3);
        SwingUtilities.convertPointToScreen(pt, chartpanel_);
        popup.setLocation(pt);
        popup.setChartTitle(info_.toUpperCase() + " First estimations");
        popup.setTsData(sRef.select(sel), revSeries);
        popup.setVisible(true);
        chartpanel_.repaint();
    }

    private void showRevisionsDocument(TsData s) {
        HtmlRevisionsDocument document = new HtmlRevisionsDocument(s, diag_);
        document.setThreshold(threshold_);
        documentpanel_.loadContent(HtmlUtil.toString(document));
    }

    private TsData revisions() {
        if (sRef == null) {
            sRef = history_.referenceSeries(info_);
        }
        int freq = sRef.getDomain().getFrequency().intValue();
        int l = years_ * freq + 1;
        int n0 = sRef.getDomain().getLength() - l;
        if (n0 < minyears_ * freq) {
            n0 = minyears_ * freq;
        }
        TsPeriod start = sRef.getDomain().get(n0);
        TsPeriod end = sRef.getDomain().getLast();
        int n = end.minus(start);
        if (n <= 0) {
            return null;
        }
        TsData rev = new TsData(start, n);
        for (int i = 0; i < n; ++i) {
            double r = history_.seriesRevision(info_, rev.getDomain().get(i), diag_);
            if (diag_ != DiagnosticInfo.AbsoluteDifference && diag_ != DiagnosticInfo.PeriodToPeriodDifference) {
                r *= 100;
            }
            rev.set(i, r);
        }
        return rev;
    }

    private void showRevisionPopup(ChartMouseEvent e) {
        XYItemEntity entity = (XYItemEntity) e.getEntity();
        TsPeriod start = firstPeriod.plus(entity.getSeriesIndex());
        popup.setTsData(history_.tsRevision(info_, start, start), null);
        Point p = e.getTrigger().getLocationOnScreen();
        p.translate(3, 3);
        popup.setLocation(p);
        popup.setChartTitle(info_.toUpperCase() + "[" + start.toString() + "] Revisions");
        popup.setVisible(true);
    }

    /**
     * Get information of the type of the view. Generally "sa" (seasonally
     * adjusted) or "t" (Trend)
     *
     * @return Type of the view
     */
    public String getInfo() {
        return info_;
    }

    /**
     * Sets the type of the view
     *
     * @param info_ Type of the view
     */
    public void setInfo(String info_) {
        this.info_ = info_;
    }

    private void addSeries(TimeSeriesCollection chartSeries, TsData data) {
        TimeSeries chartTs = new TimeSeries("");
        for (int i = 0; i < data.getDomain().getLength(); ++i) {
            if (DescriptiveStatistics.isFinite(data.get(i))) {
                Day day = new Day(data.getDomain().get(i).middle());
                chartTs.addOrUpdate(day, data.get(i));
            }
        }
        chartSeries.addSeries(chartTs);
    }

    private void addStart(TimeSeriesCollection chartSeries, String name, TsPeriod start) {
        TsData ts = history_.series(name, start);
        if (ts != null) {
            TimeSeries chartTs = new TimeSeries("");
            int pos = start.minus(ts.getStart());
            Day day = new Day(start.middle());
            chartTs.add(day, ts.get(pos));
            chartSeries.addSeries(chartTs);
        }
    }

    private void showResults() {
        if (history_ == null) {
            return;
        }

        lastIndexSelected = -1;

        final TimeSeriesCollection chartSeries = new TimeSeriesCollection();
        sRef = history_.referenceSeries(info_);
        TsPeriodSelector selector = new TsPeriodSelector();
        int n = sRef.getDomain().getLength();
        int freq = sRef.getDomain().getFrequency().intValue();
        int l = years_ * freq + 1;
        int n0 = n - l;
        if (n0 < minyears_ * freq) {
            n0 = minyears_ * freq;
        }
        if (n0 < n) {
            firstPeriod = sRef.getDomain().get(n0);
            selector.from(sRef.getDomain().get(n0).firstday());
        } else {
            firstPeriod = sRef.getStart();
        }
        addSeries(chartSeries, sRef.select(selector));

        final TimeSeriesCollection startSeries = new TimeSeriesCollection();
        TsDomain dom = sRef.getDomain();
        for (int i = n0; i < n - 1; ++i) {
            addStart(startSeries, info_, dom.get(i));
        }

        if (startSeries.getSeriesCount() == 0 || chartSeries.getSeriesCount() == 0) {
            chartpanel_.setChart(mainChart);
            return;
        }

        setRange(chartSeries, startSeries);

        XYPlot plot = mainChart.getXYPlot();
        plot.setDataset(S_INDEX, chartSeries);

        plot.setDataset(REV_INDEX, startSeries);

        for (int i = 0; i < startSeries.getSeriesCount(); i++) {
            revRenderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, 6, 6));
            revRenderer.setSeriesShapesFilled(i, false);
            revRenderer.setSeriesPaint(i, themeSupport.getLineColor(ColorScheme.KnownColor.BLUE));
        }
        plot.setRenderer(REV_INDEX, revRenderer);

        setRange(chartSeries, startSeries);
        configureAxis(plot);

        plot.mapDatasetToDomainAxis(S_INDEX, REV_INDEX);
        plot.mapDatasetToRangeAxis(S_INDEX, REV_INDEX);
        plot.mapDatasetToDomainAxis(REV_INDEX, REV_INDEX);
        plot.mapDatasetToRangeAxis(REV_INDEX, REV_INDEX);

        chartpanel_.setChart(mainChart);

        showRevisionsDocument(revisions());
    }

    private JFreeChart createMainChart() {
        XYPlot plot = new XYPlot();
        configureAxis(plot);

        plot.setRenderer(S_INDEX, sRenderer);

        plot.setRenderer(REV_INDEX, revRenderer);

        plot.setNoDataMessage("Not enough data to compute revision history !");
        JFreeChart result = new JFreeChart("", TsCharts.CHART_TITLE_FONT, plot, false);
        result.setPadding(TsCharts.CHART_PADDING);
        return result;
    }

    private void configureAxis(XYPlot plot) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM-yyyy");
        DateAxis dateAxis = new DateAxis();
        dateAxis.setTickMarkPosition(DateTickMarkPosition.MIDDLE);
        dateAxis.setDateFormatOverride(sdf);
        plot.setDomainAxis(dateAxis);
        NumberAxis yaxis = new NumberAxis();
        if (range != null) {
            yaxis.setRange(range);
        }
        plot.setRangeAxis(yaxis);
    }

    /**
     * Sets the Revision History processor used by the view to display results
     * in different graphs
     *
     * @param history Revision History to pass to the view
     */
    public void setHistory(RevisionHistory history) {
        if (this.history_ == null || !this.history_.equals(history)) {
            this.history_ = history;
            chartpanel_.setChart(null);
            showResults();
        }
    }

    @Override
    protected void onTsChange() {
        // Do nothing
    }

    @Override
    protected void onDataFormatChange() {
        // Do nothing
    }

    @Override
    protected void onColorSchemeChange() {
        sRenderer.setBasePaint(themeSupport.getLineColor(ColorScheme.KnownColor.RED));
        revRenderer.setBasePaint(themeSupport.getLineColor(ColorScheme.KnownColor.BLUE));

        XYPlot mainPlot = mainChart.getXYPlot();
        for (int i = 0; i < mainPlot.getSeriesCount(); i++) {
            revRenderer.setSeriesShape(i, new Ellipse2D.Double(-3, -3, 6, 6));
            revRenderer.setSeriesShapesFilled(i, false);
            revRenderer.setSeriesPaint(i, themeSupport.getLineColor(ColorScheme.KnownColor.BLUE));
        }

        mainPlot.setBackgroundPaint(themeSupport.getPlotColor());
        mainPlot.setDomainGridlinePaint(themeSupport.getGridColor());
        mainPlot.setRangeGridlinePaint(themeSupport.getGridColor());
        mainChart.setBackgroundPaint(themeSupport.getBackColor());
    }

    private void setRange(TimeSeriesCollection chartSeries, TimeSeriesCollection startSeries) {
        double min, max;
        Range chart = chartSeries.getRangeBounds(true);
        Range start = startSeries.getRangeBounds(true);
        min = chart.getLowerBound();
        max = chart.getUpperBound();

        if (min > start.getLowerBound()) {
            min = start.getLowerBound();
        }

        if (max < start.getUpperBound()) {
            max = start.getUpperBound();
        }

        min -= (Math.abs(min) * .03);
        max += (Math.abs(max) * .03);

        range = new Range(min, max);
    }
}
