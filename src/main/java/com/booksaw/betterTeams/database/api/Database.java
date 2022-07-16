package com.booksaw.betterTeams.database.api;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import com.booksaw.betterTeams.Main;

/**
 * API Class to managing databases
 * 
 * @author booksaw
 *
 */
public class Database {

	/**
	 * Stores the information to create a connection to the database
	 */
	Connection connection;

	/**
	 * Used to setup a connection from the provided data
	 * 
	 * @param section The configuration section which contains the database
	 *                information
	 */
	public void setupConnectionFromConfiguration(ConfigurationSection section) {

		String host = section.getString("host", "localhost");
		int port = section.getInt("port", 3306);
		String database = section.getString("database", "spigot");
		String user = section.getString("user", "root");
		String password = section.getString("password", "password");

		setupConnection(host, port, database, user, password);

	}

	/**
	 * Used to setup a connection from the provided data
	 * 
	 * @param host     The database host
	 * @param port     The database port
	 * @param database The database name
	 * @param user     The database user credential
	 * @param password The users password
	 */
	public void setupConnection(String host, int port, String database, String user, String password) {

		Bukkit.getLogger().info("[BetterTeams] Attempting to connect to database");

		try {
			connection = DriverManager.getConnection(
					"jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false", user,
					password);

			testDataSource();

		} catch (Exception e) {
			database = null;
			for (int i = 0; i < 3; i++) {
				Bukkit.getLogger().severe("[BetterTeams] ");
			}
			Bukkit.getLogger()
					.severe("[BetterTeams] Connection to the database could not be established, disabling plugin");

			Bukkit.getLogger().severe(
					"[BetterTeams] To use BetterTeams either change the storage type (config.yml/storageType) or correct the database credentials");

			for (int i = 0; i < 3; i++) {
				Bukkit.getLogger().severe("[BetterTeams] ");
			}

			e.printStackTrace();

			Main.plugin.getServer().getPluginManager().disablePlugin(Main.plugin);
			return;
		}

		Bukkit.getLogger().info("[BetterTeams] Connection with the database established");

	}

	/**
	 * Used to test if the connection is valid
	 * 
	 * @throws SQLException If a connection cannot be established, this error will
	 *                      be thrown
	 */
	private void testDataSource() throws Exception {
		if (connection == null || connection.isClosed()) {
			throw new Exception("SQL connection is not setup correctly");
		}
	}

	/**
	 * Used to close the connection to the database
	 */
	public void closeConnection() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch (Exception e) {
			Bukkit.getLogger().severe("[BetterTeams] There was an error closing the database connection");
			e.printStackTrace();
		}
	}

	/**
	 * Used to execute an SQL statement
	 * 
	 * @param statement    The SQL statement to execute
	 * @param placeholders The placeholders for the statement
	 */
	public void executeStatement(String statement, String... placeholders) {

		for (int i = 0; i < placeholders.length; i++) {
			statement = statement.replaceFirst("\\?", placeholders[i]);
		}

		statement = statement.replace("'false'", "false");
		statement = statement.replace("'true'", "true");

		try (PreparedStatement ps = connection.prepareStatement(statement)) {
//			System.out.println("executing: " + ps.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			Bukkit.getLogger().severe("Something went wrong while executing SQL");
			Bukkit.getLogger().severe("SQL: " + statement);
			e.printStackTrace();
		}
	}

	/**
	 * Used to execute an sql query
	 * 
	 * @param query        The query to execute
	 * @param placeholders The placeholders within that query
	 * @return The results of the query
	 */
	public ResultSet executeQuery(String query, String... placeholders) {
		for (int i = 0; i < placeholders.length; i++) {
			query = query.replaceFirst("\\?", placeholders[i]);
		}

		try {
			PreparedStatement ps = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
//			System.out.println("executing: " + ps.toString());
			return ps.executeQuery();
		} catch (SQLException e) {
			Bukkit.getLogger().severe("Something went wrong while executing SQL");
			Bukkit.getLogger().severe("SQL: " + query);
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * Used to create a table if the table does not currently exist
	 * 
	 * @param tableName The name of the table
	 * @param tableInfo The column information about the table
	 */
	public void createTableIfNotExists(String tableName, String tableInfo) {

		executeStatement("CREATE TABLE IF NOT EXISTS " + tableName + "(" + tableInfo + ");");

	}

}
