package org.example;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.net.URL;
import java.net.MalformedURLException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.io.PrintWriter;
import java.io.FileWriter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        String baseUrl = "https://www.nytimes.com/";

        List<String> urls = new ArrayList<>();

        Document doc = Jsoup
                .connect(baseUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                .get();

        // Getting the urls of the topics.
        Elements nextElements = doc.select(".css-1wjnrbv");
        for (Element nextElement : nextElements) {
            String url = nextElement.select("a").attr("href");
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }

        // Slicing the urls to use that.
        List<String> sliced_urls = new ArrayList<>(urls.subList(0,16));

        // Getting the topic names.
        List<String> topics = new ArrayList<>();
        for (String str :  sliced_urls) {
            String[] bits = str.split("/");
            String lasOne = bits[bits.length - 1];
            if (!topics.contains(lasOne)) {
                topics.add(lasOne);
            }
        }

        // Storing the topics' urls.
        List<String> topic_urls = new ArrayList<>();
        for (String str :  sliced_urls) {
            String new_url = baseUrl + str;
            topic_urls.add(new_url);
        }


        for (String topicUrl : topic_urls) {
            System.out.println("Scraping Topic: " + topicUrl);

            // Getting the links of the articles.
            List<String> articleLinks = new ArrayList<>();
            Document doc2 = Jsoup
                    .connect(topicUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                    .get();

            Elements next_elements = doc2.select(".css-1l4spti");
            for (Element next_element : next_elements) {
                String articleLink = next_element.select("a").attr("href");
                if (!articleLinks.contains(articleLink)) {
                    articleLinks.add(articleLink);
                }
            }

            // Modifying the article links to use that.
            List<String> scraped_urls = new ArrayList<>();
            for (String scraped_url : articleLinks) {
                String new_url = baseUrl + scraped_url;
                scraped_urls.add(new_url);
            }

            // Getting the texts of the articles.
            List<String> texts = new ArrayList<>();
            for (String scraped_url : scraped_urls) {
                Document doc3 = Jsoup
                        .connect(scraped_url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                        .get();

                Elements next_elements2 = doc3.select(".css-at9mc1.evys1bk0");
                String empty = "";
                for (Element element : next_elements2) {
                    String text = element.select("p").text();
                    if (!texts.contains(text)){
                        empty += text;
                        empty += " ";
                    }
                }
                texts.add(empty);
            }

            // Writing the data to the database.
            try {
                String dbURL = "jdbc:postgresql://localhost:5432/news";
                String username = "postgres";
                String password = "1234";

                Connection connection = DriverManager.getConnection(dbURL, username, password);
                String insertQuery = "INSERT INTO news (topic,text,url) VALUES (?, ?, ?)";
                PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
                for (int i = 0; i < articleLinks.size(); i++) {
                    String text = texts.get(i);
                    String url = scraped_urls.get(i);
                    try{
                        URL urlObj = new URL(url);
                        String path = urlObj.getPath();

                        if(path.startsWith("/")){
                            path = path.substring(1);
                        }
                        String[] pathParts = path.split("/");

                        String topic2_part="";
                        if(pathParts.length>=6){
                            if(pathParts[5].endsWith(".html")){
                                topic2_part = pathParts[4];
                            } else {
                                topic2_part = pathParts[5];
                            }
                        } else if (pathParts.length>=7) {
                            topic2_part = pathParts[6];
                        }
                        preparedStatement.setString(1, topic2_part);
                        preparedStatement.setString(2, text);
                        preparedStatement.setString(3, url);
                        preparedStatement.executeUpdate();
                    }
                    catch (MalformedURLException e){
                        System.out.println(e.getMessage());
                    }
                }
                preparedStatement.close();
                connection.close();
            } catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}