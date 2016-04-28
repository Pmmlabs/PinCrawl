package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * A simple app to download any Pinterest user's pins to a local directory.
 */
public class Main {

    private static final int TIMEOUT = 10000;
    private static String _username;
    private static String rootDir = "PinCrawl Results";

    /**
     * Verify arguments, and handle some errors
     *
     * @param args arguments (needs a string for username or abort)
     */
    public static void main(final String[] args)  {
        System.out.println("Welcome to PinCrawl, this may take a while...");

        // get username
        if (args.length > 0) {
            _username = args[0];
        } else {
            System.out.println("Enter username:");
            Scanner s = new Scanner(System.in);
            _username = s.next();
            if (_username.length() == 0) {
                System.out.println("ERROR: please enter a user name, aborting.");
                return;
            }
        }

        _username = _username.trim();
        if (_username.contains(" ")) {
            System.out.println("ERROR: username contains space character");
            return;
        }
        try {
            process();
        } catch (IOException e) {
            System.out.println("ERROR: IOException, probably a messed up URL.");
        }
    }

    /**
     * All main logic
     *
     * @throws  IOException if bad URL
     */
    private static void process() throws IOException {
        // validate username and connect to their page
        Document doc;
        try {
            doc = Jsoup.connect("https://www.pinterest.com/resource/UserResource/get/")
                    .data("data", "{\"options\":{\"username\":\"" + _username + "\",\"page_size\":250},\"module\":{\"name\":\"UserProfileContent\",\"options\":{\"tab\":\"boards\"}}}")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .timeout(TIMEOUT).get();
        } catch (HttpStatusException e) {
            System.out.println("ERROR: not a valid user name, aborting.");
            return;
        }
        doc.select("a, div").remove();
        JSONObject userObj = new JSONObject(doc.body().html().replaceAll("\n", ""));
        JSONArray boardsArr = userObj.getJSONArray("resource_data_cache").getJSONObject(1).getJSONArray("data");
        // reserve path:
        /*JSONArray boardsArr2 = userObj.getJSONObject("module").getJSONObject("tree").getJSONArray("children").getJSONObject(0).getJSONArray("children").getJSONObject(0).getJSONArray("children").getJSONObject(0).getJSONArray("children");
        JSONArray boardsArr3 = userObj.getJSONObject("module").getJSONObject("tree").getJSONArray("children").getJSONObject(0).getJSONArray("children").getJSONObject(0).getJSONArray("children").getJSONObject(0).getJSONArray("data");*/

        // make root directory
        rootDir += " for " + _username;
        String sdf = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        rootDir += " " + sdf;
        if(!makeDir(rootDir))
            return;
        System.out.println("Downloading all pins to '" + rootDir + "'...");

        for (Object board : boardsArr) {
            JSONObject boardObj = (JSONObject)board;

            // parse and format board name and make its directory
            // new, get name from Module User boardRepTitle hasText thumb title inside, instead of hover
            // hate having to use all these loops, wasn't getting selector and .attr working properly and give up
            // cause I was tired, so loops it is
            String boardName = boardObj.getString("name");
            if(boardName == null || boardName.isEmpty()) {
                System.out.println("ERROR: couldn't find name of board, it's the developer's fault. Aborting.");
                return;
            }
            //boardName = URLEncoder.encode(boardName, "UTF-8");
            boardName = boardName.replace('+',' ');
            // plus extra length safety now
            if(boardName.length() > 256) {
                boardName = boardName.substring(0, 256);
            }
            // old, got title and description
            //String boardName = boardLink.attr("title");
            //boardName = boardName.substring(10, boardName.length()); // remove "more from" part
            //boardName = URLEncoder.encode(boardName, "UTF-8");
            //boardName = boardName.replace('+',' ');
            if(!makeDir(rootDir + "\\" + boardName))
                return;

            System.out.println("...Downloading '" + boardName + "'...");
            // connect to board via url and get all page urls
            Document boardDoc = Jsoup.connect("https://www.pinterest.com/resource/BoardFeedResource/get/")
                    .data("data", "{\"options\":{\"board_id\":\""+boardObj.getString("id")+"\",\"page_size\":250}}")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .ignoreContentType(true)
                    .maxBodySize(0)
                    .timeout(TIMEOUT).get();

            boardDoc.select("a").remove();
            JSONObject obj = new JSONObject(boardDoc.body().html().replaceAll("\n",""));
            JSONArray arr = obj.getJSONObject("resource_response").getJSONArray("data");

            for (int i = 0; i < arr.length(); i++) {
                saveImage(arr.getJSONObject(i).getJSONObject("images").getJSONObject("orig").getString("url"), rootDir + "\\" + boardName, i);
            }
        }

        System.out.println("All pins downloaded, to " + System.getProperty("user.dir")
                + "\\"  + rootDir + "\\");
        System.out.println("Thanks for using PinCrawl!");
    }

    /**
     * Makes a directory with the filename provided, fails if it already exists
     * TODO: allow arguments for overwrite, subtractive, and additive changes
     *
     * @param name name of the file
     */
    public static boolean makeDir(String name) {
        File file = new File(name);
        if (!file.exists()) {
            if (file.mkdir()) {
                return true;
            } else {
                System.out.println("ERROR: Failed to create directory '" + name + "', aborting.");
            }
        } else {
            System.out.println("ERROR: Directory '" + name + "' already exists, aborting.");
        }
        return false;
    }

    /**
     * Saves an image from the specified URL to the path with the name count
     * TODO: allow gifs, maybe
     *
     * @param srcUrl url of image
     * @param path path to save image (in root\board)
     * @param count count to name image since I don't want to use the long and tedious names on pinterest
     * @throws IOException
     */
    public static void saveImage(String srcUrl, String path, int count) throws IOException {
        URL url = new URL(srcUrl);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(path + "\\" + count + "." +srcUrl.substring(srcUrl.length()-3));
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }
}
