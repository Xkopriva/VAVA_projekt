package sk.bais.dto;

/**
 * Obohacený zápis – spája polia enrollment + predmet.
 * Posiela sa frontendu ako odpoveď na GET_MY_ENROLLMENTS.
 */
public class EnrollmentWithSubjectDTO {

    private int    id;
    private int    studentId;
    private int    subjectId;
    private int    semesterId;
    private int    attemptNumber;
    private String status;

    // Polia predmetu (JOIN)
    private String subjectCode;
    private String subjectName;
    private int    credits;

    public EnrollmentWithSubjectDTO() {}

    public EnrollmentWithSubjectDTO(
            int id, int studentId, int subjectId, int semesterId,
            int attemptNumber, String status,
            String subjectCode, String subjectName, int credits) {
        this.id            = id;
        this.studentId     = studentId;
        this.subjectId     = subjectId;
        this.semesterId    = semesterId;
        this.attemptNumber = attemptNumber;
        this.status        = status;
        this.subjectCode   = subjectCode;
        this.subjectName   = subjectName;
        this.credits       = credits;
    }

    // ── Gettery (Jackson ich potrebuje na serializáciu) ──────────────

    public int    getId()            { return id; }
    public int    getStudentId()     { return studentId; }
    public int    getSubjectId()     { return subjectId; }
    public int    getSemesterId()    { return semesterId; }
    public int    getAttemptNumber() { return attemptNumber; }
    public String getStatus()        { return status; }
    public String getSubjectCode()   { return subjectCode; }
    public String getSubjectName()   { return subjectName; }
    public int    getCredits()       { return credits; }
}
