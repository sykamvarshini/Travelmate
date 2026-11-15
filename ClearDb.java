import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ClearDb {
    public static void main(String[] args) {
        try {
            String dbUser = System.getenv("DB_USERNAME") != null ? System.getenv("DB_USERNAME") : System.getProperty("DB_USERNAME", "root");
            String dbPass = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : System.getProperty("DB_PASSWORD");
            if (dbPass == null) {
                java.io.File envFile = new java.io.File(".env");
                if (envFile.exists()) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("DB_PASSWORD=")) {
                                dbPass = line.substring("DB_PASSWORD=".length()).trim();
                                if (dbPass.startsWith("\"") && dbPass.endsWith("\"")) dbPass = dbPass.substring(1, dbPass.length()-1);
                            }
                        }
                    }
                }
            }
            if (dbPass == null) {
                System.err.println("DB_PASSWORD environment variable or .env file is required.");
                return;
            }

            Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/travelmate_ai", dbUser, dbPass);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("ALTER TABLE trips MODIFY COLUMN destination_id BIGINT NULL");
            System.out.println("Table altered successfully.");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
