import java.sql.Connection;
import java.sql.DriverManager;

public class ConexaoDB {
    // Restaurando o banco correto: retguarda
    private static final String URL = "jdbc:mysql://10.0.0.104:3306/retguarda?useSSL=false";
    private static final String USUARIO = "root";
    private static final String SENHA = "infor";

    public static Connection getConexao() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(URL, USUARIO, SENHA);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}