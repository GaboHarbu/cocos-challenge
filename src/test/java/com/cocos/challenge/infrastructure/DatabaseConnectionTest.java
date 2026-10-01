package com.cocos.challenge.infrastructure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@SpringBootTest
@Disabled("Diagnostic test to run explicitly against live NeonDB database")
public class DatabaseConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    public void testNeonDbConnectionAndSchema() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            System.out.println(">>> Connected to database: " + conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion());

            // List tables
            ResultSet rsTables = stmt.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
            );
            System.out.println(">>> Tables in public schema:");
            while (rsTables.next()) {
                String table = rsTables.getString(1);
                System.out.println("  - " + table);
            }

            // Inspect columns for users, instruments, orders, marketdata
            String[] tables = {"users", "instruments", "orders", "marketdata"};
            for (String t : tables) {
                System.out.println(">>> Columns for " + t + ":");
                ResultSet rsCols = stmt.executeQuery(
                        "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = '" + t + "' ORDER BY ordinal_position"
                );
                while (rsCols.next()) {
                    System.out.println("    " + rsCols.getString("column_name") + " (" + rsCols.getString("data_type") + ")");
                }

                ResultSet rsCount = stmt.executeQuery("SELECT count(*) FROM " + t);
                if (rsCount.next()) {
                    System.out.println("    Row count: " + rsCount.getLong(1));
                }
            }

            System.out.println(">>> Sample Instruments of type MONEDA or ARS:");
            ResultSet rsMoney = stmt.executeQuery("SELECT * FROM instruments WHERE type = 'MONEDA' OR ticker = 'ARS'");
            while (rsMoney.next()) {
                System.out.println("  Money instrument: id=" + rsMoney.getInt("id") + ", ticker=" + rsMoney.getString("ticker") + ", name=" + rsMoney.getString("name") + ", type=" + rsMoney.getString("type"));
            }

            System.out.println(">>> Existing Orders:");
            ResultSet rsOrders = stmt.executeQuery("SELECT * FROM orders ORDER BY id");
            while (rsOrders.next()) {
                System.out.println("  Order: id=" + rsOrders.getInt("id") +
                        ", userId=" + rsOrders.getInt("userid") +
                        ", instrumentId=" + rsOrders.getInt("instrumentid") +
                        ", side=" + rsOrders.getString("side") +
                        ", size=" + rsOrders.getInt("size") +
                        ", price=" + rsOrders.getBigDecimal("price") +
                        ", type=" + rsOrders.getString("type") +
                        ", status=" + rsOrders.getString("status") +
                        ", datetime=" + rsOrders.getTimestamp("datetime"));
            }

            System.out.println(">>> Users:");
            ResultSet rsUsers = stmt.executeQuery("SELECT * FROM users ORDER BY id");
            while (rsUsers.next()) {
                System.out.println("  User: id=" + rsUsers.getInt("id") + ", email=" + rsUsers.getString("email") + ", accountNumber=" + rsUsers.getString("accountnumber"));
            }
        }
    }
}
