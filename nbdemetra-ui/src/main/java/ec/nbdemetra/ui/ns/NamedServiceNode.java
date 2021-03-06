/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.nbdemetra.ui.ns;

import ec.nbdemetra.ui.Config;
import ec.nbdemetra.ui.IConfigurable;
import java.awt.Image;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Sheet;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Philippe Charles
 */
public class NamedServiceNode extends AbstractNode {

    public NamedServiceNode(INamedService namedService) {
        super(Children.LEAF, Lookups.singleton(namedService));
        setName(namedService.getName());
        setDisplayName(namedService.getDisplayName());
    }

    protected INamedService getNamedService() {
        return getLookup().lookup(INamedService.class);
    }

    @Override
    public Image getIcon(int type) {
        Image image = getNamedService().getIcon(type, false);
        return image != null ? image : super.getIcon(type);
    }

    @Override
    public Image getOpenedIcon(int type) {
        Image image = getNamedService().getIcon(type, true);
        return image != null ? image : super.getIcon(type);
    }

    @Override
    protected Sheet createSheet() {
        return getNamedService().createSheet();
    }
    //
    Config latestConfig;

    public void applyConfig() {
        if (latestConfig != null) {
            getLookup().lookup(IConfigurable.class).setConfig(latestConfig);
            latestConfig = null;
        }
    }

    @Override
    public Action getPreferredAction() {
        final IConfigurable service = getLookup().lookup(IConfigurable.class);
        return service != null ? new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                latestConfig = service.editConfig(latestConfig != null ? latestConfig : service.getConfig());
            }
        } : super.getPreferredAction();
    }
}
