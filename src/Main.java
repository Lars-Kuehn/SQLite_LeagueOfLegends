import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.sql.*;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    Connection connection;
    QueryResult queryResult;
    DatabaseConnector databaseConnector;

    public Main() {
        String jdbcUrl = "jdbc:sqlite:leagueoflegends.db";

        connection = null;

        try {
            connection = DriverManager.getConnection(jdbcUrl);
            System.out.println("Connected");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        databaseConnector = new DatabaseConnector("12345", 4747, "leagueoflegends.db", "lars", "hallo");
        init();
    }

    public void init() {
        Scanner outerScanner = new Scanner(System.in);
        System.out.println("[1] Get selected table | [2] Get selected champion, [3] Update database, [4] Order by Playrate, [5] Order by Winrate, [6] Order by Banrate, [7] Get Champion by Role");

        switch (outerScanner.nextInt()) {
            case 1:
                Scanner innerScanner = new Scanner(System.in);
                System.out.println("What table do you want to print out?");

                databaseConnector.executeStatement("SELECT * FROM " + innerScanner.nextLine());
                queryResult = databaseConnector.getCurrentQueryResult();
                getData();

                break;

            case 2:
                innerScanner = new Scanner(System.in);
                System.out.println("Which champion do you want to print out?");

                databaseConnector.executeStatement("SELECT * FROM Champions WHERE name = '" + innerScanner.nextLine() + "'");
                queryResult = databaseConnector.getCurrentQueryResult();
                getData();

                break;

            case 3:
                databaseConnector.executeStatement("DELETE FROM Champions");
                System.out.println("Deleted table successfully");
                System.out.println("Updating table..");
                createTable();
                System.out.println("Updated successfully");

                break;

            case 4:
                databaseConnector.executeStatement("SELECT * FROM Champions ORDER BY Popularity desc");
                queryResult = databaseConnector.getCurrentQueryResult();
                getData();

                break;

            case 5:
                databaseConnector.executeStatement("SELECT * FROM Champions ORDER BY Winrate desc");
                queryResult = databaseConnector.getCurrentQueryResult();
                getData();

                break;

            case 6:
                databaseConnector.executeStatement("SELECT * FROM Champions ORDER BY Banrate desc");
                queryResult = databaseConnector.getCurrentQueryResult();
                getData();

                break;

            case 7:
                innerScanner = new Scanner(System.in);
                System.out.println("What role?");

                databaseConnector.executeStatement("SELECT * FROM Champions WHERE role LIKE '%" + innerScanner.nextLine() + "%'");
                queryResult = databaseConnector.getCurrentQueryResult();
                getData();

                break;
        }
        init();
    }

    public String[] getUrl() {
        Document doc = null;
        try {
            doc = Jsoup.connect("https://na.leagueoflegends.com/en-us/champions/").get();
        } catch (IOException e) {
            e.printStackTrace();
        }


        String documentNames = doc.select("[class=style__List-ntddd-2 fqjuPM]").toString();

        char[] charArray = documentNames.toCharArray();
        String temp = "";
        for (int i = 0; i < charArray.length-1; i++) {
            if (charArray[i] == '>') {
                if (charArray[i + 1] != '<' && charArray[i+1] != '\n') {
                    int k = i + 1;
                    while (charArray[k] != '<') {
                        temp += charArray[k];
                        k += 1;
                    }
                    temp += ",";
                }
            }
        }

        temp = temp.replaceAll("[\n]", "");
        temp = temp.replaceAll(" ", "");
        temp = temp.replaceAll("'", "");
        temp = temp.replace(".", "");
        temp = temp.replaceAll("Nunu&amp;Willump", "Nunu");

        String[] finalString = temp.split(",");

        return finalString;
    }

    public void createTable() {
        String[] champs = getUrl();

        for (int i = 0; i < champs.length; i++) {

            Document doc = null;
            try {
                doc = Jsoup.connect("https://www.leagueofgraphs.com/de/champions/builds/" + champs[i].toLowerCase(Locale.ROOT)).get();
            } catch (HttpStatusException h) {
                continue;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException n) {
                continue;
            }

            String name = doc.select("[class=filterHeader noselect]").text();
            name = name.substring(0, name.length() - 3);

            String popularity = doc.select("[id=graphDD1]").text();

            String winrate = doc.select("[id=graphDD2]").text();

            String banrate = doc.select("[id=graphDD3]").text();

            String roles = doc.select("[class=bannerSubtitle]").text();

            insertCreateTable(name, roles, popularity, winrate, banrate);
        }
    }

    public void insertCreateTable(String pName, String pRoles, String pPopularity, String pWinrate, String pBanrate) {
        if (pPopularity != "") {
            final String INSERT_Champions = "INSERT INTO Champions(name, role, popularity, winrate, banrate)  VALUES(?,?,?,?,?)";

            PreparedStatement ps = null;

            try {
                ps = connection.prepareStatement(INSERT_Champions);

                ps.setString(1, pName);
                ps.setString(2, pRoles);
                ps.setString(3, pPopularity);
                ps.setString(4, pWinrate);
                ps.setString(5, pBanrate);

                ps.execute();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

        }
    }

    public void getData(){
        String[][] temp = queryResult.getData();
        for (int i = 0; i < temp.length; i++) {
            for (int j = 0; j < temp[i].length; j++) {
                System.out.print(temp[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void main(String[] args) {
        new Main();
    }
}