package sk.BAIS.model;

/**
 * Model trieda reprezentujuca studenta v systeme.
 */
public class Student {

    private int id;  // alternativne moze byt id = bais ID 
    private int baisId;
    private String firstName;
    private String lastName;
    private String email;
    private int year;

    // Prazdny konstruktor pre REST API atd
    public Student() {}

    // Main konstruktor
    public Student(int id, int baisId, String firstName, String lastName, String email, int year) {
        this.id = id;
        this.baisId = baisId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.year = year;
    }

    // Konstruktor bez id (pre INSERT - id generuje DB)
    public Student(String firstName, String lastName, String email, int year) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.year = year;
    }

    // Gettery a Settery

    public int getDBID() { return id; }
    public void setDBID(int id) { this.id = id; }

    public int getBaisId() { return baisId; }
    public void setBaisId(int baisId) { this.baisId = baisId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    @Override
    public String toString() {
        return "Student{id=" + id +
                ", baisId='" + baisId + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", year=" + year + '}';
    }
}