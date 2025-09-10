package Model;

/**
 * Clase que representa una enfermedad en el sistema.
 * Incluye información básica como un ID, nombre, severidad
 * y la secuencia genómica asociada.
 */

public class Disease {
    private String diseaseId;
    private String name;
    private int severity;       // escala 1..10
    private String sequence;    // secuencia genómica (ACGTN...)

    
    /**
     * Crea una nueva enfermedad con sus atributos básicos.
     * @param diseaseId ID único de la enfermedad
     * @param name Nombre de la enfermedad
     * @param severity Nivel de severidad (1-10)
     * @param sequence Secuencia genómica asociada
     */
    
    public Disease(String diseaseId, String name, int severity, String sequence) {
        this.diseaseId = diseaseId;
        this.name = name;
        this.severity = severity;
        this.sequence = sequence;
    }

    // Getters
    public String getDiseaseId() {
        return diseaseId;
    }

    public String getName() {
        return name;
    }

    public int getSeverity() {
        return severity;
    }

    public String getSequence() {
        return sequence;
    }

     // Setters 
    
    public void setDiseaseId(String diseaseId) {
        this.diseaseId = diseaseId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSeverity(int severity) {
        this.severity = severity;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    // toString()
   
    /**
     * Representación en texto de la enfermedad.
     * Incluye el ID, nombre, severidad y la longitud de la secuencia genómica.
     */
    
    @Override
    public String toString() {
        int seqLength = 0;
        if (sequence != null) {
            seqLength = sequence.length();
        }

        return "Disease{" +
                "diseaseId='" + diseaseId + '\'' +
                ", name='" + name + '\'' +
                ", severity=" + severity +
                ", sequenceLength=" + seqLength +
                '}';
    }

}
