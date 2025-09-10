package Model;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Clase que representa a un paciente en el sistema genómico.
 * Aquí se guardan sus datos básicos, información clínica y detalles del archivo FASTA cargado.
 */

    // Atrbiutos

public class Patient {
    private int patientID;              
    private String fullName;
    private String documentID;          
    private int age;
    private String sex;               
    private String contactEmail;
    private LocalDateTime registrationDate;
    private String clinicalNotes;
    private String checksumFasta;      
    
    private long fileSizeBytes;

    public Patient(String fullName, String documentID, int age, String sex,
                   String contactEmail, LocalDateTime registrationDate,
                   String clinicalNotes, String checksumFasta, long fileSizeBytes) {
        this.fullName = fullName;
        this.documentID = documentID;
        this.age = age;
        this.sex = sex;
        this.contactEmail = contactEmail;
        this.registrationDate = registrationDate;
        this.clinicalNotes = clinicalNotes;
        this.checksumFasta = checksumFasta;
        this.fileSizeBytes = fileSizeBytes;
    }

    // Getters y setters
    public int getPatientID() {
        return patientID;
    }

    public void setPatientID(int patientID) {
        this.patientID = patientID;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDocumentID() {
        return documentID;
    }

    public int getAge() {
        return age;
    }

    public String getSex() {
        return sex;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public String getChecksumFasta() {
        return checksumFasta;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public void setClinicalNotes(String clinicalNotes) {
        this.clinicalNotes = clinicalNotes;
    }

    public void setChecksumFasta(String checksumFasta) {
        this.checksumFasta = checksumFasta;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }
    
    

    // Validaciones como el correo y el sexo
    public boolean isValidEmail() {
        String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return Pattern.matches(regex, this.contactEmail);
    }

    public boolean isValidSex() {
        return this.sex != null && (this.sex.equalsIgnoreCase("M") || this.sex.equalsIgnoreCase("F"));
    }

    @Override
    public String toString() {
        return "Patient{" +
                ",ID: " + patientID +
                ",fullName: '" + fullName + '\'' +
                ",documentID: '" + documentID + '\'' +
                ",age: " + age +
                ",sex:" + sex + '\'' +
                ",contactEmail: '" + contactEmail + '\'' +
                ",registrationDate: " + registrationDate +
                ",clinicalNotes: '" + clinicalNotes + '\'' +
                ",checksumFasta: '" + checksumFasta + '\'' +
                ",fileSizeBytes: " + fileSizeBytes +
                '}';
    }
}
