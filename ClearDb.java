import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ClearDb {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/travelmate_ai", "root", "Ammulu@3004");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE trips MODIFY COLUMN destination_id BIGINT NULL");
            System.out.println("Table altered successfully.");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
