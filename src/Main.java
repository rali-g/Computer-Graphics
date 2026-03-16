import server.CrossPlotServer;

public class Main {
    public static void main(String[] args) {
        CrossPlotServer server = new CrossPlotServer();
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Грешка при стартиране: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
