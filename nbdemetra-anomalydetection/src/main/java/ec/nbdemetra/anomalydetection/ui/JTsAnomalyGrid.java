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
package ec.nbdemetra.anomalydetection.ui;

import ec.nbdemetra.ui.properties.l2fprod.ColorChooser;
import ec.tss.Ts;
import ec.tss.TsCollection;
import ec.tstoolkit.modelling.DefaultTransformationType;
import ec.tstoolkit.modelling.arima.IPreprocessor;
import ec.tstoolkit.modelling.arima.PreprocessingModel;
import ec.tstoolkit.modelling.arima.tramo.TramoException;
import ec.tstoolkit.modelling.arima.tramo.TramoSpecification;
import ec.tstoolkit.timeseries.regression.OutlierEstimation;
import ec.tstoolkit.timeseries.regression.OutlierType;
import ec.ui.grid.JTsGrid;
import ec.ui.grid.TsGridObs;
import static ec.tstoolkit.timeseries.simplets.TsDataTableInfo.Empty;
import static ec.tstoolkit.timeseries.simplets.TsDataTableInfo.Missing;
import static ec.tstoolkit.timeseries.simplets.TsDataTableInfo.Valid;
import ec.ui.interfaces.ITsGrid;
import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JToolTip;
import static javax.swing.SwingConstants.TRAILING;
import javax.swing.SwingWorker;
import static javax.swing.SwingWorker.StateValue.DONE;
import static javax.swing.SwingWorker.StateValue.PENDING;
import static javax.swing.SwingWorker.StateValue.STARTED;
import javax.swing.table.DefaultTableCellRenderer;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A grid component used to display outliers found in time series. The outliers
 * are highlighted in the grid in color corresponding to the outlier's type
 *
 * @author Mats Maggi
 */
public class JTsAnomalyGrid extends JComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(JTsAnomalyGrid.class);
    public static final String SPEC_CHANGE_PROPERTY = "Spec Change";
    public static final String CRITICAL_VALUE_PROPERTY = "Critical Value Change";
    public static final String COLLECTION_PROPERTY = "Collection Change";
    public static final String STATE_PROPERTY = "State Property";
    public static final String TYPES_PROPERTY = "Types Displayed";
    public static final String TRANSFORMATION_PROPERTY = "Transformation Change";
    private JTsGrid grid;
    private List<OutlierEstimation[]> outliers;
    private IPreprocessor preprocessor;
    private PreprocessingModel model;
    private DefaultTransformationType transformation;
    private TsCollection tsCollection;
    private TramoSpecification defaultSpec = TramoSpecification.TR4.clone();
    private TramoSpecification spec = TramoSpecification.TR4.clone();
    private double criticalValue = .0;
    private boolean defaultCritical = true;
    private ProgressHandle progressHandle;
    private SwingWorker<Void, Ts> worker;
    private boolean showAO = true;
    private boolean showLS = true;
    private boolean showTC = true;
    private boolean showSO = false;

    // <editor-fold defaultstate="collapsed" desc="Constructors">
    /**
     * Constructs a new JTsAnomalyGrid
     */
    public JTsAnomalyGrid() {
        super();
        setLayout(new BorderLayout());
        grid = new JTsGrid();
        outliers = new ArrayList<>();
        grid.setUseColorScheme(true);
        grid.setCellRenderer(new AnomalyCellRenderer());

        // Listening to a data change to calculate the new outliers
        grid.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                switch (evt.getPropertyName()) {
                    case JTsGrid.COLLECTION_PROPERTY:
                        onCollectionChange((TsCollection) evt.getNewValue());
                        break;
                    case ITsGrid.ZOOM_PROPERTY:
                        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                        break;
                    case ITsGrid.SELECTION_PROPERTY:
                        firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                        break;
                }
            }
        });

        addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String p = evt.getPropertyName();
                if (p.equals(STATE_PROPERTY)) {
                    onStateChange();
                    firePropertyChange(OutliersTopComponent.STATE_CHANGED, null, worker.getState());
                }
            }
        });

        add(grid, BorderLayout.CENTER);

        preprocessor = spec.build();
        transformation = DefaultTransformationType.None;
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Getters / Setters">
    public void setDefaultCritical(boolean def) {
        defaultCritical = def;
    }

    public boolean isDefaultCritical() {
        return defaultCritical;
    }

    public DefaultTransformationType getTransformation() {
        return transformation;
    }

    public void setTransformation(DefaultTransformationType transformation) {
        DefaultTransformationType old = this.transformation;
        this.transformation = transformation;
        firePropertyChange(TRANSFORMATION_PROPERTY, old, this.transformation);
    }

    public void setZoomPercentage(int percentage) {
        grid.zoom(percentage);
    }

    public int getZoomPercentage() {
        return grid.getZoomRatio();
    }

    public PreprocessingModel getModelOfSelection() {
        if (getSelection().length > 0) {
            return preprocessor.process(getSelection()[0].getTsData(), null);
        } else {
            return null;
        }
    }

    public Ts[] getSelection() {
        return grid.getSelection();
    }

    public boolean isShowAO() {
        return showAO;
    }

    public void setShowAO(boolean showAO) {
        this.showAO = showAO;
        refreshOutliersDisplayed();
    }

    public boolean isShowLS() {
        return showLS;
    }

    public void setShowLS(boolean showLS) {
        this.showLS = showLS;
        refreshOutliersDisplayed();
    }

    public boolean isShowTC() {
        return showTC;
    }

    public void setShowTC(boolean showTC) {
        this.showTC = showTC;
        refreshOutliersDisplayed();
    }

    public boolean isShowSO() {
        return showSO;
    }

    public void setShowSO(boolean showSO) {
        this.showSO = showSO;
        refreshOutliersDisplayed();
    }

    public void setDefaultSpec(TramoSpecification spec) {
        TramoSpecification old = this.spec;
        this.defaultSpec = spec.clone();
        this.spec = spec.clone();
        firePropertyChange(SPEC_CHANGE_PROPERTY, old, this.spec);
    }

    public TramoSpecification getDefaultSpec() {
        return defaultSpec;
    }

    public void setCriticalValue(double value) {
        try {
            double old = criticalValue;
            criticalValue = value;
            //refreshOutliersDisplayed();
            firePropertyChange(CRITICAL_VALUE_PROPERTY, old, criticalValue);
        } catch (TramoException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public double getCriticalValue() {
        return criticalValue;
    }

    public int getSelectionIndex() {
        if (tsCollection == null) {
            return -1;
        }

        Ts[] ts = grid.getSelection();
        if (ts.length > 0) {
            return tsCollection.indexOf(ts[0]);
        }

        return -1;
    }

    public SwingWorker.StateValue getState() {
        return worker != null ? worker.getState() : SwingWorker.StateValue.PENDING;
    }

    public TsCollection getTsCollection() {
        return grid.getTsCollection();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Event Handlers">
    private void refreshOutliersDisplayed() {
        outliers.clear();
        grid.setSelection(null);
        OutlierType[] types = spec.getOutliers().getTypes();
        spec.getOutliers().clearTypes();
        if (showAO) {
            spec.getOutliers().add(OutlierType.AO);
        }
        if (showLS) {
            spec.getOutliers().add(OutlierType.LS);
        }
        if (showTC) {
            spec.getOutliers().add(OutlierType.TC);
        }
        if (showSO) {
            spec.getOutliers().add(OutlierType.SO);
        }
        spec.getOutliers().setCriticalValue(defaultCritical ? 0.0 : criticalValue);
        spec.getTransform().setFunction(transformation);
        preprocessor = spec.build();
        firePropertyChange(TYPES_PROPERTY, types, spec.getOutliers().getTypes());
    }

    private void onCollectionChange(TsCollection col) {
        TsCollection old = tsCollection;
        tsCollection = col;

        for (int i = 0; i < col.getCount(); i++) {
            if (col.get(i) == null || col.get(i).getTsData() == null) {
                tsCollection.remove(col.get(i));
            }
        }

        refreshOutliersDisplayed();
        firePropertyChange(COLLECTION_PROPERTY, old, tsCollection);
    }

    protected void onStateChange() {
        switch (getState()) {
            case DONE:
                if (progressHandle != null) {
                    progressHandle.finish();
                }
                break;
            case PENDING:
                break;
            case STARTED:
                progressHandle = ProgressHandleFactory.createHandle("Calculating Outliers...", new Cancellable() {
                    @Override
                    public boolean cancel() {
                        return worker.cancel(true);
                    }
                });
                progressHandle.start(100);
                break;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Thread Management">
    public void calculateOutliers() {
        refreshOutliersDisplayed();
        stop();
        start(false);
    }

    private class SwingWorkerImpl extends SwingWorker<Void, Ts> {

        private int progressCount = 0;

        @Override
        protected void done() {
            super.done();
            grid.repaint();
        }

        @Override
        protected Void doInBackground() throws Exception {
            outliers.clear();
            grid.repaint();
            outliers = new ArrayList<>();

            for (int i = 0; i < tsCollection.getCount(); i++) {
                if (isCancelled()) {
                    progressHandle.finish();
                    return null;
                }
                if (tsCollection.get(i).getTsData().getLength() == 0) {
                    outliers.add(i, null);
                } else {
                    model = preprocessor.process(tsCollection.get(i).getTsData(), null);
                    OutlierEstimation[] o = model.outliersEstimation(true, false);
                    if (o != null && o.length > 0) {
                        outliers.add(i, o);
                    } else {
                        outliers.add(i, null);
                    }
                }
                publish(tsCollection.get(i));

            }
            return null;
        }

        @Override
        protected void process(List<Ts> chunks) {
            grid.fireTableDataChanged();
            progressCount += chunks.size();
            if (progressHandle != null && !chunks.isEmpty()) {
                progressHandle.progress(100 * progressCount / tsCollection.getCount());
            }
        }
    }

    public boolean start(boolean local) {
        worker = new JTsAnomalyGrid.SwingWorkerImpl();
        worker.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                firePropertyChange(STATE_PROPERTY, null, worker.getState());
            }
        });
        worker.execute();
        return true;
    }

    public boolean stop() {
        return worker != null ? worker.cancel(true) : false;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Cell Renderer">
    /*
     * Renderer coloring the cells containing outliers
     */
    private class AnomalyCellRenderer extends DefaultTableCellRenderer {

        private JToolTip tooltip;
        private DecimalFormat df = new DecimalFormat("0.0000");

        public AnomalyCellRenderer() {
            setHorizontalAlignment(TRAILING);
            setOpaque(true);

            tooltip = super.createToolTip();
        }

        @Override
        public JToolTip createToolTip() {
            tooltip.setBackground(getBackground());
            tooltip.setForeground(getForeground());
            return tooltip;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setForeground(null);
            setBackground(null);
            setToolTipText(null);
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof TsGridObs) {
                TsGridObs obs = (TsGridObs) value;

                switch (obs.getInfo()) {
                    case Empty:
                        setText("");
                        break;
                    case Missing:
                        setText(".");
                        break;
                    case Valid:
                        /*
                         * Try to find a match between the outlier and the current cell
                         * using the TsPeriods
                         */
                        setText(String.valueOf(obs.getValue()));
                        if (outliers.size() <= obs.getSeriesIndex() || outliers.get(obs.getSeriesIndex()) == null) {
                            setToolTipText("<html>Period : " + obs.getPeriod().toString() + "<br>"
                                    + "Value : " + df.format(obs.getValue()));
                        } else {
                            OutlierEstimation[] est = outliers.get(obs.getSeriesIndex());
                            boolean found = false;
                            int i = 0;
                            OutlierEstimation outlier = null;
                            while (!found && i < est.length) {
                                if (est[i].getPosition().equals(obs.getPeriod())) {
                                    found = true;
                                    outlier = est[i];
                                }
                                i++;
                            }

                            setText(String.valueOf(obs.getValue()));
                            if (found) {
                                setToolTipText("<html>Period : " + obs.getPeriod().toString() + "<br>"
                                        + "Value : " + df.format(obs.getValue()) + "<br>"
                                        + "Outlier Value : " + df.format(outlier.getValue()) + "<br>"
                                        + "Std Err : " + df.format(outlier.getStdev()) + "<br>"
                                        + "TStat : " + df.format(outlier.getTStat()) + "<br>"
                                        + "Outlier type : " + outlier.getCode().toString());
                                setBackground(ColorChooser.getColor(outlier.getCode()));
                                setForeground(ColorChooser.getForeColor(outlier.getCode()));
                            } else {
                                setToolTipText("<html>Period : " + obs.getPeriod().toString() + "<br>"
                                        + "Value : " + df.format(obs.getValue()));
                            }
                        }
                }
            }

            return this;
        }
    }
// </editor-fold>
}
