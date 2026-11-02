import java.sql.*;

public class CheckUsers {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/travelmate_ai", "root", "Ammulu@3004");
        ResultSet rs = conn.createStatement().executeQuery("SELECT id, email, full_name, role FROM users");
        while (rs.next()) {
            System.out.println(rs.getInt("id") + " | " + rs.getString("email") + " | " + rs.getString("full_name") + " | " + rs.getString("role"));
        }
        conn.close();
    }
}
