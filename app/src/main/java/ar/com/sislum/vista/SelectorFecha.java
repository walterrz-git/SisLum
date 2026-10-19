package ar.com.sislum.vista;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import java.time.LocalDate;
import java.util.Locale;

// Calendario compartido por las pantallas.
public final class SelectorFecha extends DatePicker {
    public SelectorFecha() {
        super(new DatePickerSettings(Locale.forLanguageTag("es-AR")));
        getSettings().setFormatForDatesCommonEra("dd/MM/uuuu");
        getSettings().setAllowEmptyDates(false);
        getSettings().setAllowKeyboardEditing(false);
        getComponentToggleCalendarButton().setToolTipText("Elegir fecha");
        getComponentDateTextField().setPreferredSize(new java.awt.Dimension(120, 28));
        getComponentToggleCalendarButton().setPreferredSize(new java.awt.Dimension(32, 28));
        setPreferredSize(new java.awt.Dimension(160, 28));
        setDate(LocalDate.now());
        setOpaque(false);
    }

    public LocalDate leerFecha() {
        LocalDate fecha = getDate();
        if (fecha == null) { throw new IllegalArgumentException("Seleccioná una fecha en el calendario."); }
        return fecha;
    }
}
