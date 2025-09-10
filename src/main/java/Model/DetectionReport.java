package Model; 
import java.time.LocalDateTime;

/**
 * Clase que representa un reporte de detección de enfermedad para un paciente.
 * Contiene información como paciente, enfermedad detectada, severidad,
 * fecha de detección y una breve descripción.
 */
public class DetectionReport {
    
    // Atributos de la clase
   
    private String patientId;     
    private String diseaseId;  
    private int severity;        
    private LocalDateTime detectedAt; 
    private String description;  


    /**
     * Crea un nuevo reporte de detección para un paciente.
     * @param patientId ID del paciente
     * @param diseaseId ID de la enfermedad detectada
     * @param severity Severidad de la enfermedad
     * @param description Breve descripción del caso
     */
    public DetectionReport(String patientId, String diseaseId, int severity, String description) {
        this.patientId = patientId;
        this.diseaseId = diseaseId;
        this.severity = severity;
        this.description = description;
        this.detectedAt = LocalDateTime.now(); // Se asigna la fecha/hora automáticamente
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

    // toString()

    /**
     * Representación en formato texto del reporte.
     * Útil para guardarlo en archivos CSV, logs o depuración.
     */
    @Override
    public String toString() {
        return patientId + "," +
               diseaseId + "," +
               severity + "," +
               detectedAt + "," +
               description;
    }
}
