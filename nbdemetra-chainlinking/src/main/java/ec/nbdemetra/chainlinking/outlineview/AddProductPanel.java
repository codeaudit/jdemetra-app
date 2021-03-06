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
package ec.nbdemetra.chainlinking.outlineview;

import ec.tstoolkit.timeseries.simplets.chainlinking.AChainLinking.Product;
import ec.util.various.swing.FontAwesome;
import java.awt.Color;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Panel used to add a new product entry in the Chain Linking input data
 *
 * @author Mats Maggi
 */
public class AddProductPanel extends javax.swing.JPanel {

    public static final String PROCESS_PRODUCTS = "processProducts";

    /**
     * Creates new form AddProductPanel
     */
    public AddProductPanel() {
        initComponents();
        addProductButton.setIcon(FontAwesome.FA_PLUS.getIcon(Color.GREEN.darker(), 11f));
        removeButton.setIcon(FontAwesome.FA_MINUS.getIcon(Color.RED, 11f));

        addProductTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    if (addProductTable.getSelectedRowCount() != 0) {
                        removeButton.setEnabled(true);
                    } else {
                        removeButton.setEnabled(false);
                    }
                }
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        jScrollPane1 = new javax.swing.JScrollPane();
        addProductTable = new ec.nbdemetra.chainlinking.outlineview.AddProductTable();
        jPanel1 = new javax.swing.JPanel();
        addProductButton = new javax.swing.JButton();
        processButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(AddProductPanel.class, "AddProductPanel.border.title_1"))); // NOI18N
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));
        add(filler1);

        jScrollPane1.setViewportView(addProductTable);

        add(jScrollPane1);

        jPanel1.setMaximumSize(new java.awt.Dimension(2147483647, 23));
        jPanel1.setPreferredSize(new java.awt.Dimension(214, 23));
        jPanel1.setLayout(new java.awt.BorderLayout());

        org.openide.awt.Mnemonics.setLocalizedText(addProductButton, org.openide.util.NbBundle.getMessage(AddProductPanel.class, "AddProductPanel.addProductButton.text")); // NOI18N
        addProductButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addProductButtonActionPerformed(evt);
            }
        });
        jPanel1.add(addProductButton, java.awt.BorderLayout.WEST);

        org.openide.awt.Mnemonics.setLocalizedText(processButton, org.openide.util.NbBundle.getMessage(AddProductPanel.class, "AddProductPanel.processButton.text")); // NOI18N
        processButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processButtonActionPerformed(evt);
            }
        });
        jPanel1.add(processButton, java.awt.BorderLayout.CENTER);

        org.openide.awt.Mnemonics.setLocalizedText(removeButton, org.openide.util.NbBundle.getMessage(AddProductPanel.class, "AddProductPanel.removeButton.text")); // NOI18N
        removeButton.setEnabled(false);
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });
        jPanel1.add(removeButton, java.awt.BorderLayout.EAST);

        add(jPanel1);
    }// </editor-fold>//GEN-END:initComponents

    private void addProductButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addProductButtonActionPerformed
        addProductTable.addProduct(new Product("", null, null, null));
    }//GEN-LAST:event_addProductButtonActionPerformed

    private void processButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processButtonActionPerformed
        try {
            calculateNbNull();
            firePropertyChange(PROCESS_PRODUCTS, null, addProductTable.getProducts());
            ((ProductTableModel) addProductTable.getModel()).fireTableDataChanged();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error !", JOptionPane.ERROR_MESSAGE);;
        }
    }//GEN-LAST:event_processButtonActionPerformed

    private void calculateNbNull() {
        List<Product> prds = addProductTable.getProducts();
        for (int i = 0; i < prds.size(); i++) {
            if (prds.get(i).getName().isEmpty()) {
                throw new IllegalArgumentException("Product names can't be empty !");
            } else {
                int nb = 0;
                if (prds.get(i).getQuantities() == null) {
                    nb++;
                }
                if (prds.get(i).getPrice() == null) {
                    nb++;
                }
                if (prds.get(i).getValue() == null) {
                    nb++;
                }

                if (nb > 1) {
                    throw new IllegalArgumentException("Some input data are missing !");
                }
            }
        }
    }

    private void removeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeButtonActionPerformed
        if (addProductTable.getSelectedRowCount() > 0) {
            addProductTable.removeProduct(addProductTable.getSelectedRow());
        }
    }//GEN-LAST:event_removeButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addProductButton;
    private ec.nbdemetra.chainlinking.outlineview.AddProductTable addProductTable;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton processButton;
    private javax.swing.JButton removeButton;
    // End of variables declaration//GEN-END:variables
}
