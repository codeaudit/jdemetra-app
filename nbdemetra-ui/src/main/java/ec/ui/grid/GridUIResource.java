/*
 * Copyright 2015 National Bank of Belgium
 * 
 * Licensed under the EUPL, Version 1.1 or - as soon they will be approved 
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
package ec.ui.grid;

import static ec.util.chart.swing.SwingColorSchemeSupport.withAlpha;
import ec.util.various.swing.LineBorder2;
import static ec.util.various.swing.StandardSwingColor.TABLE_BACKGROUND;
import static ec.util.various.swing.StandardSwingColor.TABLE_FOREGROUND;
import static ec.util.various.swing.StandardSwingColor.TABLE_HEADER_BACKGROUND;
import static ec.util.various.swing.StandardSwingColor.TABLE_HEADER_FOREGROUND;
import static ec.util.various.swing.StandardSwingColor.TABLE_SELECTION_BACKGROUND;
import static ec.util.various.swing.StandardSwingColor.TABLE_SELECTION_FOREGROUND;
import java.awt.Color;
import javax.annotation.Nonnull;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 *
 * @author Philippe Charles
 */
abstract class GridUIResource {

    @Nonnull
    abstract public CellUIResource getHeader(boolean selected, boolean hovered);

    @Nonnull
    abstract public CellUIResource getCell(boolean selected, boolean hovered);

    @Nonnull
    public static GridUIResource getDefault() {
        return GridColorsImpl.INSTANCE;
    }

    //<editor-fold defaultstate="collapsed" desc="Implementation details">
    private static final class GridColorsImpl extends GridUIResource {

        private static final GridColorsImpl INSTANCE = new GridColorsImpl();

        private final CellUIResource header;
        private final CellUIResource headerSelection;
        private final CellUIResource headerFocus;
        private final CellUIResource headerBoth;
        private final CellUIResource cell;
        private final CellUIResource cellSelection;
        private final CellUIResource cellFocus;
        private final CellUIResource cellBoth;

        private GridColorsImpl() {
            Color headerBackground = TABLE_HEADER_BACKGROUND.or(new Color(240, 240, 240));
            Color headerForeground = TABLE_HEADER_FOREGROUND.or(Color.BLACK);
            Color background = TABLE_BACKGROUND.or(Color.WHITE);
            Color foreground = TABLE_FOREGROUND.or(Color.BLACK);
            Color selectionBackground = TABLE_SELECTION_BACKGROUND.or(new Color(51, 153, 255));
            Color selectionForeground = TABLE_SELECTION_FOREGROUND.or(new Color(255, 255, 255));

            Border headerBorder = BorderFactory.createCompoundBorder(
                    new LineBorder2(headerBackground.brighter(), 0, 0, 1, 1),
                    BorderFactory.createEmptyBorder(0, 4, 0, 4));
            Border noBorder = BorderFactory.createEmptyBorder();

            this.header = CellUIResource.of(headerBackground, headerForeground, headerBorder);
            this.headerSelection = CellUIResource.of(selectionBackground.darker(), selectionForeground, headerBorder);
            this.headerFocus = CellUIResource.of(selectionBackground, selectionForeground, headerBorder);
            this.headerBoth = CellUIResource.of(selectionBackground, selectionForeground, headerBorder);

            this.cell = CellUIResource.of(background, foreground, noBorder);
            this.cellSelection = CellUIResource.of(selectionBackground, selectionForeground, noBorder);
            this.cellFocus = CellUIResource.of(withAlpha(selectionBackground, 200), selectionForeground, noBorder);
            this.cellBoth = CellUIResource.of(withAlpha(selectionBackground, 200), selectionForeground, noBorder);
        }

        @Override
        public CellUIResource getHeader(boolean selected, boolean hovered) {
            return selected ? (hovered ? headerBoth : headerSelection) : (hovered ? headerFocus : header);
        }

        @Override
        public CellUIResource getCell(boolean selected, boolean hovered) {
            return selected ? (hovered ? cellBoth : cellSelection) : (hovered ? cellFocus : cell);
        }
    }
    //</editor-fold>
}
