package API;

import java.sql.*;
import java.util.*;

/**
 * Implementation of the CA-to-PU API.
 * Reads from Cosymed's local SQLite database and returns
 * a simplified retail catalogue (no wholesale costs exposed).
 */
public class CA_PU_implementation implements CA_PU_interface {

    private static final String DB_URL = "jdbc:sqlite:ipos_ca.db";

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found.");
        }
        Connection conn = DriverManager.getConnection(DB_URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    private double loadVATRate(Connection conn) throws SQLException {
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(
                "SELECT config_value FROM system_config WHERE config_key = 'vat_rate'");
        double rate = 0.0;
        if (rs.next()) {
            try {
                rate = Double.parseDouble(rs.getString("config_value"));
            } catch (NumberFormatException ignored) {}
        }
        rs.close();
        stmt.close();
        return rate;
    }

    private double calcRetailPrice(double bulkCost, double markupRate, double vatRate) {
        return bulkCost * (1 + markupRate / 100.0) * (1 + vatRate / 100.0);
    }

    private Map<String, String> buildItemMap(ResultSet rs, double vatRate) throws SQLException {
        double bulkCost = rs.getDouble("bulk_cost");
        double markupRate = rs.getDouble("markup_rate");
        int availability = rs.getInt("availability");
        double retailPrice = calcRetailPrice(bulkCost, markupRate, vatRate);

        Map<String, String> item = new LinkedHashMap<>();
        item.put("itemId",      rs.getString("item_id"));
        item.put("description", rs.getString("description"));
        item.put("quantity",    String.valueOf(availability));
        item.put("unitCost",    String.format("%.2f", retailPrice));
        item.put("totalCost",   String.format("%.2f", retailPrice * availability));
        return item;
    }

    @Override
    public List<Map<String, String>> getCatalogue() throws Exception {
        List<Map<String, String>> results = new ArrayList<>();

        try (Connection conn = getConnection()) {
            double vatRate = loadVATRate(conn);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM stock_items WHERE availability > 0 ORDER BY item_id");

            while (rs.next()) {
                results.add(buildItemMap(rs, vatRate));
            }
            rs.close();
            stmt.close();
        }
        return results;
    }

    @Override
    public List<Map<String, String>> searchCatalogue(String keyword) throws Exception {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("Search keyword must not be blank.");
        }

        List<Map<String, String>> results = new ArrayList<>();

        try (Connection conn = getConnection()) {
            double vatRate = loadVATRate(conn);

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM stock_items WHERE availability > 0 " +
                            "AND (LOWER(item_id) LIKE ? OR LOWER(description) LIKE ?) ORDER BY item_id");
            String term = "%" + keyword.toLowerCase() + "%";
            ps.setString(1, term);
            ps.setString(2, term);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                results.add(buildItemMap(rs, vatRate));
            }
            rs.close();
            ps.close();
        }
        return results;
    }

    @Override
    public Map<String, String> getItemDetails(String itemId) throws Exception {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank.");
        }

        try (Connection conn = getConnection()) {
            double vatRate = loadVATRate(conn);

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM stock_items WHERE item_id = ?");
            ps.setString(1, itemId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, String> item = buildItemMap(rs, vatRate);
                rs.close();
                ps.close();
                return item;
            }
            rs.close();
            ps.close();
        }
        return null;
    }

    @Override
    public boolean checkAvailability(String itemId, int quantity) throws Exception {
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive.");
        }

        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT availability FROM stock_items WHERE item_id = ?");
            ps.setString(1, itemId);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean available = rs.getInt("availability") >= quantity;
                rs.close();
                ps.close();
                return available;
            }
            rs.close();
            ps.close();
        }
        return false;
    }

    @Override
    public String getVATRate() throws Exception {
        try (Connection conn = getConnection()) {
            return String.valueOf(loadVATRate(conn));
        }
    }
}
