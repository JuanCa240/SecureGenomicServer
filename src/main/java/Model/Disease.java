package Model;

public class Disease {
    private String diseaseId;
    private String name;
    private int severity;       // escala 1..10
    private String sequence;    // secuencia genómica (ACGTN...)

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

    // Setters (opcional, si no quieres objetos inmutables)
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

    @Override
    public String toString() {
        return "Disease{" +
                "diseaseId='" + diseaseId + '\'' +
                ", name='" + name + '\'' +
                ", severity=" + severity +
                ", sequenceLength=" + (sequence != null ? sequence.length() : 0) +
                '}';
    }
}
