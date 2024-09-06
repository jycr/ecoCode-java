/*
 * ecoCode - Java language - Provides rules to reduce the environmental footprint of your Java programs
 * Copyright © 2023 Green Code Initiative (https://www.ecocode.io)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.greencodeinitiative.java.checks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

class AvoidSQLRequestInLoopCheck {
    AvoidSQLRequestInLoopCheck(AvoidSQLRequestInLoopCheck mc) {
    }

    public void testWithNoLoop() {
        try {
            // create our mysql database connection
            String myDriver = "driver";
            String myUrl = "driver";
            Class.forName(myDriver);
            Connection conn = DriverManager.getConnection(myUrl, "toor", "");

            // our SQL SELECT query.
            // if you only need a few columns, specify them by name instead of using "*"
            String query = "SELECT * FROM users";

            // create the java statement
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);

            // iterate through the java resultset
            while (rs.next()) {
                int id = rs.getInt("id");
                System.out.println(id);
            }
            st.close();
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
        }
    }

    public void testWithForLoop() {
        try {
            // create our mysql database connection
            String myDriver = "driver";
            String myUrl = "driver";
            Class.forName(myDriver);
            Connection conn = DriverManager.getConnection(myUrl, "toor", "");

            // our SQL SELECT query.
            // if you only need a few columns, specify them by name instead of using "*"
            String baseQuery = "SELECT name FROM users where id = ";

            for (int i = 0; i < 20; i++) {

                // create the java statement
                String query = baseQuery.concat("" + i);
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(query); // Noncompliant {{Avoid SQL request in loop}}

                // iterate through the java resultset
                while (rs.next()) {
                    String name = rs.getString("name");
                    System.out.println(name);
                }
                st.close();
            }
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
        }
    }

    public void testWithForEachLoop() {
        try {
            // create our mysql database connection
            String myDriver = "driver";
            String myUrl = "driver";
            Class.forName(myDriver);
            Connection conn = DriverManager.getConnection(myUrl, "toor", "");

            // our SQL SELECT query.
            // if you only need a few columns, specify them by name instead of using "*"
            String query = "SELECT * FROM users";
            int[] intArray = {10, 20, 30, 40, 50};
            for (int i : intArray) {
                System.out.println(i);
                // create the java statement
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(query); // Noncompliant {{Avoid SQL request in loop}}

                // iterate through the java resultset
                while (rs.next()) {
                    int id = rs.getInt("id");
                    System.out.println(id);
                }
                st.close();
            }
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
        }
    }

    public void testWithWhileLoop() {
        try {
            // create our mysql database connection
            String myDriver = "driver";
            String myUrl = "driver";
            Class.forName(myDriver);
            Connection conn = DriverManager.getConnection(myUrl, "toor", "");

            // our SQL SELECT query.
            // if you only need a few columns, specify them by name instead of using "*"
            String query = "SELECT * FROM users";
            int i = 0;
            while (i < -1) {

                // create the java statement
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(query); // Noncompliant {{Avoid SQL request in loop}}

                // iterate through the java resultset
                while (rs.next()) {
                    int id = rs.getInt("id");
                    System.out.println(id);
                }
                st.close();
            }
        } catch (Exception e) {
            System.err.println("Got an exception! ");
            System.err.println(e.getMessage());
		}
	}

}