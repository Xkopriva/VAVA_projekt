package sk.bais.students;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) trieda pre entitu Student.
 *
 * Obsahuje operacie:
 *  - list()      → SELECT * FROM students
 *  - read()   → SELECT ... WHERE id = ?
 *  - create()    → INSERT
 *  - update()    → UPDATE
 *  - delete()    → DELETE
 */
public class StudentDAO {

    // -------------------------------------------------------------------------
    // SQL prikazy - zatial vygenerovane, ako placeholder
    // -------------------------------------------------------------------------

    private static final String SQL_LIST =
            "SELECT id, bais_id, first_name, last_name, email, year FROM students ORDER BY id";

    private static final String SQL_READ =
            "SELECT id, bais_id, first_name, last_name, email, year FROM students WHERE id = ?";

    private static final String SQL_INSERT =
            "INSERT INTO students (bais_id, first_name, last_name, email, year) VALUES (?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE students SET bais_id = ?, first_name = ?, last_name = ?, email = ?, year = ? WHERE id = ?";

    private static final String SQL_DELETE =
            "DELETE FROM students WHERE id = ?";

    // -------------------------------------------------------------------------
    // LIST – vrati vsetkych studentov z DB
    // -------------------------------------------------------------------------

    /**
     * Nacita vsetkych studentov z databazy
     *
     * @return zoznam vsetkych studentov (moze byt prazdny, nikdy null)
     * @throws SQLException pri chybe komunikacie s DB
     */
    public List<Student> list() throws SQLException {
        List<Student> students = new ArrayList<>();
        // TODO: pozriet ked sa upravi SQL, ci toto treba menit
        try (Connection dbConnection = DatabaseConnection.getConnection();
             PreparedStatement statement = dbConnection.prepareStatement(SQL_LIST);
             ResultSet result = statement.executeQuery()) {
            // iterujem cez rows
            while (result.next()) {
                students.add(mapRowToObject(result));
            }
        }

        return students;
    }

    // -------------------------------------------------------------------------
    // READ – vrati jedneho studenta podla ID
    // -------------------------------------------------------------------------

    /**
     * Najde studenta podla jeho ID
     *
     * @param id ID studenta
     * @return Optional so studentom, alebo prazdny Optional ak neexistuje student
     * @throws SQLException pri chybe komunikacie s DB
     */
    public Optional<Student> read(int id) throws SQLException {
        try (Connection dbConnection = DatabaseConnection.getConnection();
            PreparedStatement statement = dbConnection.prepareStatement(SQL_READ)) {
            // Zadam id do SQL_READ querry
            statement.setInt(1, id);
            // Querry sa vykona a bud vrati, alebo nevrati studenta
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapRowToObject(result));
                }
            }
        }

        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // CREATE – vlozi noveho studenta do DB
    // -------------------------------------------------------------------------

    /**
     * Vlozi noveho studenta do databazy
     * ID generuje databaza (SERIAL)
     *
     * @param student student na vlozenie (pole id sa ignoruje)
     * @return vlozeny student vratan vygenerovanym ID
     * @throws SQLException pri chybe komunikacie s DB
     */
    public Student create(Student student) throws SQLException {
        try (Connection dbConnection = DatabaseConnection.getConnection();
            PreparedStatement statement = dbConnection.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            // Zadam data do querry
            statement.setInt(1, student.getBaisId());
            statement.setString(2, student.getFirstName());
            statement.setString(3, student.getLastName());
            statement.setString(4, student.getEmail());
            statement.setInt(5, student.getYear());
            // Querry sa vykona
            int affectedRows = statement.executeUpdate();
            // Vrati 0 ak nevykonal, inak vykonal = vytvoril studenta
            if (affectedRows == 0) {
                throw new SQLException("Vytvorenie studenta zlyhalo, ziadne riadky neboli ovplyvnene.");
            }

            // Ziskame vygenerovane ID z DB
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    student.setId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Vytvorenie studenta zlyhalo, ID nebolo ziskane.");
                }
            }
        }

        return student;
    }

    // -------------------------------------------------------------------------
    // UPDATE – aktualizuje existujuceho studenta v DB
    // -------------------------------------------------------------------------

    /**
     * Aktualizuje udaje existujuceho studenta
     *
     * @param student student s aktualizovanymi hodnotami (musi mat platne ID)
     * @return true ak bol zaznam aktualizovany, false ak student s danym ID neexistuje
     * @throws SQLException pri chybe komunikacie s DB
     */
    public boolean update(Student student) throws SQLException {
        try (Connection dbConnection = DatabaseConnection.getConnection();
            PreparedStatement statement = dbConnection.prepareStatement(SQL_UPDATE)) {
            // Zadam data do querry
            statement.setInt(1, student.getBaisId());
            statement.setString(2, student.getFirstName());
            statement.setString(3, student.getLastName());
            statement.setString(4, student.getEmail());
            statement.setInt(5, student.getYear());
            statement.setInt(6, student.getId());
            
            // Vrati 0 ak nevykonal, inak vykonal = aktualizoval studenta
            return statement.executeUpdate() > 0;
        }
    }

    // -------------------------------------------------------------------------
    // DELETE – vymaze studenta podla ID z DB
    // -------------------------------------------------------------------------

    /**
     * Vymaze studenta z databazy podla ID
     *
     * @param id ID studenta na vymazanie
     * @return true ak bol zaznam vymazany, false ak student s danym ID neexistuje
     * @throws SQLException pri chybe komunikacie s DB
     */
    public boolean delete(int id) throws SQLException {
        try (Connection dbConnection = DatabaseConnection.getConnection();
            PreparedStatement statement = dbConnection.prepareStatement(SQL_DELETE)) {
            // Zadam data do querry
            statement.setInt(1, id);
            // Vrati 0 ak nevykonal, inak vykonal = vymazal studenta
            return statement.executeUpdate() > 0;
        }
    }

    /**
     * Helper - Namapuje aktualny riadok ResultSet na objekt Student
     */
    private Student mapRowToObject(ResultSet result) throws SQLException {
        return new Student(
                result.getInt("id"),
                result.getInt("bais_id"),
                result.getString("first_name"),
                result.getString("last_name"),
                result.getString("email"),
                result.getInt("year")
        );
    }
}