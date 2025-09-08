package Model;
import java.time.LocalDateTime;

public class DetectionReport {
    private String patientId;     // id del paciente
    private String diseaseId;     // id de la enfermedad detectada
    private int severity;         // severidad de la enfermedad
    private LocalDateTime detectedAt; // fecha/hora de detección
    private String description;   // breve descripción o comentario

    public DetectionReport(String patientId, String diseaseId, int severity, String description) {
        this.patientId = patientId;
        this.diseaseId = diseaseId;
        this.severity = severity;
        this.description = description;
        this.detectedAt = LocalDateTime.now(); // se marca al crear el reporte
    }

    // Getters
    public String getPatientId() {
        return patientId;
    }

    public String getDiseaseId() {
        return diseaseId;
    }

    public int getSeverity() {
        return severity;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public String getDescription() {
        return description;
    }

    // Representación en texto (útil para CSV o logs)
    @Override
    public String toString() {
        return patientId + "," +
               diseaseId + "," +
               severity + "," +
               detectedAt + "," +
               description;
    }
}
