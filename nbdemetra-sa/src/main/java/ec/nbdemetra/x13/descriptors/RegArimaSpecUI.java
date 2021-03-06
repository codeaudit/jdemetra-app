/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.nbdemetra.x13.descriptors;

import ec.tstoolkit.descriptors.EnhancedPropertyDescriptor;
import ec.tstoolkit.descriptors.IObjectDescriptor;
import ec.tstoolkit.modelling.arima.x13.RegArimaSpecification;
import ec.tstoolkit.timeseries.simplets.TsDomain;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Jean Palate
 */
public class RegArimaSpecUI implements IObjectDescriptor<RegArimaSpecification> {

    final RegArimaSpecification core;
    final boolean ro_;
    private TsDomain domain_;

    public RegArimaSpecUI(RegArimaSpecification spec, TsDomain domain, boolean ro) {
        if (spec == null)
            throw new AssertionError(EMPTY);
        core = spec;
        domain_ = domain;
        ro_ = ro;
    }

    @Override
    public RegArimaSpecification getCore() {
        return core;
    }

    public BasicSpecUI getBasic() {
        return new BasicSpecUI(core, ro_);
    }

    public RegressionSpecUI getRegression() {
        return new RegressionSpecUI(core, ro_);
    }

    public TransformSpecUI getTransform() {
        return new TransformSpecUI(core, ro_);
    }

    public ArimaSpecUI getArima() {
        return new ArimaSpecUI(core, ro_);
    }

    public OutlierSpecUI getOutliers() {
        return new OutlierSpecUI(core, ro_);
    }

    public EstimateSpecUI getEstimate() {
        return new EstimateSpecUI(core, ro_);
    }

    @Override
    public List<EnhancedPropertyDescriptor> getProperties() {
        ArrayList<EnhancedPropertyDescriptor> descs = new ArrayList<>();
//        EnhancedPropertyDescriptor desc = basicDesc();
//        if (desc != null)
//            descs.add(desc);
        EnhancedPropertyDescriptor desc = estimateDesc();
        if (desc != null) {
            descs.add(desc);
        }
        desc = transformDesc();
        if (desc != null) {
            descs.add(desc);
        }
        desc = regressionDesc();
        if (desc != null) {
            descs.add(desc);
        }
        desc = arimaDesc();
        if (desc != null) {
            descs.add(desc);
        }
        desc = outlierDesc();
        if (desc != null) {
            descs.add(desc);
        }
        return descs;
    }
    private static final int BASIC_ID = 1,
            TRANSFORM_ID = 2,
            REGRESSION_ID = 3,
            AUTOMODEL_ID = 4,
            ARIMA_ID = 5,
            OUTLIER_ID = 6,
            ESTIMATE_ID = 7;

    private EnhancedPropertyDescriptor regressionDesc() {
        try {
            PropertyDescriptor desc = new PropertyDescriptor("regression", this.getClass(), "getRegression", null);
            EnhancedPropertyDescriptor edesc = new EnhancedPropertyDescriptor(desc, REGRESSION_ID);
            desc.setDisplayName("REGRESSION");
            //edesc.setReadOnly(true);
            return edesc;
        }
        catch (IntrospectionException ex) {
            return null;
        }
    }

    private EnhancedPropertyDescriptor transformDesc() {
        try {
            PropertyDescriptor desc = new PropertyDescriptor("transform", this.getClass(), "getTransform", null);
            EnhancedPropertyDescriptor edesc = new EnhancedPropertyDescriptor(desc, TRANSFORM_ID);
            desc.setDisplayName("TRANSFORMATION");
            //edesc.setReadOnly(true);
            return edesc;
        }
        catch (IntrospectionException ex) {
            return null;
        }
    }

    private EnhancedPropertyDescriptor basicDesc() {
        try {
            PropertyDescriptor desc = new PropertyDescriptor("basic", this.getClass(), "getBasic", null);
            EnhancedPropertyDescriptor edesc = new EnhancedPropertyDescriptor(desc, BASIC_ID);
            desc.setDisplayName("SERIES");
            return edesc;
        }
        catch (IntrospectionException ex) {
            return null;
        }
    }

    private EnhancedPropertyDescriptor arimaDesc() {
        try {
            PropertyDescriptor desc = new PropertyDescriptor("arima", this.getClass(), "getArima", null);
            EnhancedPropertyDescriptor edesc = new EnhancedPropertyDescriptor(desc, ARIMA_ID);
            desc.setDisplayName("ARIMA");
            return edesc;
        }
        catch (IntrospectionException ex) {
            return null;
        }
    }

    private EnhancedPropertyDescriptor outlierDesc() {
        try {
            PropertyDescriptor desc = new PropertyDescriptor("outlier", this.getClass(), "getOutliers", null);
            EnhancedPropertyDescriptor edesc = new EnhancedPropertyDescriptor(desc, OUTLIER_ID);
            desc.setDisplayName("OUTLIERS");
            return edesc;
        }
        catch (IntrospectionException ex) {
            return null;
        }
    }

    private EnhancedPropertyDescriptor estimateDesc() {
        try {
            PropertyDescriptor desc = new PropertyDescriptor("estimate", this.getClass(), "getEstimate", null);
            EnhancedPropertyDescriptor edesc = new EnhancedPropertyDescriptor(desc, ESTIMATE_ID);
            desc.setDisplayName("ESTIMATE");
            return edesc;
        }
        catch (IntrospectionException ex) {
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        return "RegArima (X13)";
    }
}
