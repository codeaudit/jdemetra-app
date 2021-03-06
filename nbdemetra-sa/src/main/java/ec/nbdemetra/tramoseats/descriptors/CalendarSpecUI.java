/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ec.nbdemetra.tramoseats.descriptors;

import ec.tstoolkit.modelling.arima.tramo.TramoSpecification;
import ec.tstoolkit.descriptors.EnhancedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pcuser
 */
public class CalendarSpecUI extends BaseTramoSpecUI{

    @Override
    public String toString(){
        return "";
    }

    public CalendarSpecUI(TramoSpecification spec, boolean ro){
        super(spec, ro);
    }

    public TradingDaysSpecUI getTradingDays(){
        return new TradingDaysSpecUI(core, ro_);
    }

    public void setTradingDays(TradingDaysSpecUI spec){

    }

    public EasterSpecUI getEaster(){
        return new EasterSpecUI(core, ro_);
    }

    public void setEaster(EasterSpecUI spec){

    }

    @Override
     public List<EnhancedPropertyDescriptor> getProperties() {
        // regression
        ArrayList<EnhancedPropertyDescriptor> descs=new ArrayList<>();
        EnhancedPropertyDescriptor desc=tdDesc();
        if (desc != null)
            descs.add(desc);
        desc=easterDesc();
        if (desc != null)
            descs.add(desc);
        return descs;
    }


    ///////////////////////////////////////////////////////////////////////////

    private static final int TD_ID=1, EASTER_ID=2;

    private EnhancedPropertyDescriptor tdDesc(){
        try {
            PropertyDescriptor desc = new PropertyDescriptor("tradingDays", this.getClass());
            EnhancedPropertyDescriptor edesc=new EnhancedPropertyDescriptor(desc, TD_ID);
            edesc.setReadOnly(ro_);
            return edesc;
        } catch (IntrospectionException ex) {
            return null;
        }
    }

    private EnhancedPropertyDescriptor easterDesc(){
        try {
            PropertyDescriptor desc = new PropertyDescriptor("easter", this.getClass());
            EnhancedPropertyDescriptor edesc=new EnhancedPropertyDescriptor(desc, EASTER_ID);
            edesc.setReadOnly(ro_);
            return edesc;
        } catch (IntrospectionException ex) {
            return null;
        }
    }

    public String getDisplayName() {
        return "Calendar";
    }
}
