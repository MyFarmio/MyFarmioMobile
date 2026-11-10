package com.myfarmio.app.ui.common;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Display formatting only; does not rewrite stored values or status rules. */
public final class RecordText {
    private RecordText() {}
    public static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }
    public static String or(Map<String, Object> row, String key, String fallback) {
        String text = value(row, key);
        return text.isEmpty() ? fallback : text;
    }
    public static double number(Map<String, Object> row, String key) {
        try { double n = Double.parseDouble(value(row, key)); return Double.isFinite(n) ? n : 0; }
        catch (NumberFormatException e) { return 0; }
    }
    public static String amount(double number) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("es", "AR"));
        format.setMaximumFractionDigits(1);
        return format.format(number);
    }
    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
    public static String label(String key) {
        switch (key) {
            case "healthy": return "Sano";
            case "treatment": return "En tratamiento";
            case "open": return "Vacía";
            case "pregnant": return "Preñada";
            case "served": return "Servida";
            case "dry": return "Seca";
            case "calving": return "Parición";
            case "pending": return "Pendiente";
            case "paid": return "Pagado";
            case "collected": return "Cobrado";
            case "scheduled": return "Programado";
            case "overdue": return "Vencido";
            case "income": return "Ingreso";
            case "expense": return "Gasto";
            case "attention": return "Requiere atención";
            case "quarantine": return "En cuarentena";
            case "deceased": return "Fallecido";
            case "sold": return "Vendido";
            case "lactating": return "En lactancia";
            case "breeding": return "En servicio";
            case "not_applicable": return "No corresponde";
            case "ok": return "Disponible";
            case "planned": return "Planificada";
            case "in_progress": return "En progreso";
            case "in_review": return "En revisión";
            case "completed": case "done": return "Completada";
            case "cancelled": return "Cancelada";
            case "low": return "Baja";
            case "medium": return "Media";
            case "high": return "Alta";
            case "urgent": return "Crítica";
            case "optimal": return "Óptimo";
            case "active": return "Activo";
            case "todo": return "Pendiente";
            case "draft": return "Borrador";
            case "archived": return "Archivado";
            case "assigned": return "Asignada";
            case "waiting": return "En espera";
            case "monitoring": case "watch": case "warning": case "vigilar": return "En seguimiento";
            case "issue": case "problem": return "Con problema";
            case "harvest": case "harvesting": return "Cosecha";
            case "resting": case "rest": return "Descanso";
            case "inactive": return "Inactivo";
            case "delayed": return "Atrasado";
            case "stable": case "estable": return "Estable";
            case "critical": case "sick": case "critico": return "Crítico";
            case "otros": return "Otros";
            case "ventas": return "Ventas";
            case "venta_granos": return "Venta de granos";
            case "venta_hacienda": return "Venta de hacienda";
            case "servicios_prestados": return "Servicios prestados";
            case "alquileres": return "Alquileres";
            case "otros_ingresos": return "Otros ingresos";
            case "insumos": return "Insumos";
            case "semillas": return "Semillas";
            case "fertilizantes": return "Fertilizantes";
            case "agroquimicos": return "Agroquímicos";
            case "maquinaria": return "Maquinaria";
            case "sanidad": return "Sanidad";
            case "alimentacion_animal": return "Alimentación animal";
            case "combustible": return "Combustible";
            case "servicios": return "Servicios";
            case "servicios_contratados": return "Servicios contratados";
            case "transporte": return "Transporte";
            case "mano_obra": return "Mano de obra";
            case "mantenimiento": return "Mantenimiento";
            case "impuestos": return "Impuestos";
            case "salarios": return "Salarios";
            case "general": return "General";
            case "agricultura": return "Agricultura";
            case "ganaderia": return "Ganadería";
            case "administracion": return "Administración";
            case "transferencia": return "Transferencia";
            case "efectivo": return "Efectivo";
            case "tarjeta": return "Tarjeta";
            case "cheque": return "Cheque";
            case "mercado_pago": return "Mercado Pago";
            case "cuenta_corriente": return "Cuenta corriente";
            case "canje": return "Canje";
            case "otro": return "Otro";
            default: return key.isEmpty() ? "Sin estado" : key.replace('_', ' ');
        }
    }
    public static boolean attention(String status) {
        return status.equals("issue") || status.equals("problem") || status.equals("monitoring")
            || status.equals("critical") || status.equals("sick") || status.equals("urgent")
            || status.equals("watch") || status.equals("warning") || status.equals("critico") || status.equals("vigilar");
    }
    public static LocalDate date(String raw) {
        try { return LocalDate.parse(raw.length() >= 10 ? raw.substring(0, 10) : raw); }
        catch (Exception ignored) { return null; }
    }
    public static String dateLabel(String raw) {
        LocalDate date = date(raw);
        return date == null ? (raw.isEmpty() ? "Sin fecha" : raw)
            : date.format(DateTimeFormatter.ofPattern("d MMM yyyy", new Locale("es", "AR")));
    }
    public static boolean overdue(Map<String, Object> row, LocalDate today) {
        String status = value(row, "status");
        LocalDate due = date(value(row, "due_date"));
        return due != null && due.isBefore(today) && !status.equals("completed")
            && !status.equals("done") && !status.equals("cancelled");
    }
    public static boolean matches(Map<String, Object> row, String query, String... keys) {
        StringBuilder haystack = new StringBuilder();
        for (String key : keys) haystack.append(value(row, key)).append(' ');
        return normalize(haystack.toString()).contains(normalize(query));
    }
}
