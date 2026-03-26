import java.net.ServerSocket;
public class TestSocket {
    public static void main(String[] args) {
        try {
            ServerSocket s = new ServerSocket(0);
            System.out.println("Port: " + s.getLocalPort());
            s.close();
            System.out.println("Success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
