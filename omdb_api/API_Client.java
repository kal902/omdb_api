package omdb_api_client;

import java.io.BufferedReader;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.HttpURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Kaleab
 */
class API_Client{
    /* Todo: use multiple apikeys. this may solve the error thrown with the api server when multiple requests is made in a small time
     * interval. free api keys have a time restriction that will forbid them from making many requests at a time.
     * or let the user pass an api key.
     */
    private String default_apikey = "b5797cc3";
	private String api_url = "http://www.omdbapi.com/?s=NAME";
	private BufferedReader in;
    public static final int TYPE_SERIES = 1;
    public static final int TYPE_MOVIE = 0;

	public Result search(String movname) throws UnknownHostException, IOException{
        if(!is_apikey_set()){ // use default api key if user doesnt set one.
            set_api_Key(default_apikey);
        }
        movie_name = mov_name;
        String full_url = api_url.replace("NAME", mov_name);
        
        URL url = new URL(full_url);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
            
        int responsecode = conn.getResponseCode();
        //System.out.println(responsecode);
        if(responsecode==200){
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder json_string = new StringBuilder();
                
            String temp;
            while((temp=in.readLine())!=null){
                json_string.append(temp);
             }
            in.close();
            conn.disconnect();
            //System.out.println(json_string.toString());
            return new Result(JSONObject(json_string.toString()));
                
        }else{
            return new Result();
        }
    }

    public boolean set_type(int type){
        switch(type){
            case TYPE_MOVIE:
                api_url = api_url+"&type=movie";
                break;
            case TYPE_SERIES:
                api_url = api_url+"&type=series";
                break;
            default:
                return false;
        }
        return true;
    }

    public void set_api_Key(String key){
        String arg = "&apikey="+key;
        this.api_url+=arg;
    }

    private boolean is_apikey_set(){
        if(this.api_url.contains("apikey")) return true;
        return false;
    }

	class Result{
		private JSONObject json_response;

		public Result(){
            // means a response code other than 200 is returned from the api server.
			this.json_response = null;
		}

		public Result(JSONObject api_response){
            this.json_response = api_response;
		}

        public boolean is_result_available(){
            if(this.json_response){
                return true;
            }else{
                return false;
            }
        }

        public boolean download_image(int index, File output_file){
            // @param index: index of the result in the json file
            // @param output_file: a File object to write the image to.
            String image_uri;
            try{ // the index parameter might be invalid, if an error occured then there is no valid image link available;
                image_uri = get_image_link(index); // get_image_link() will throw JSONException.
            }catch(org.json.JSONException ex){
                System.out.println("JSONException occured while downloading image");
                return false;
            }
            BufferedInputStream in;
            FileOutputStream fout;
            try{
                in = new BufferedInputStream(new URL(image_uri)openStream());
                fout = new FileOutputStream(output_file);

                byte databuffer[] = new byte[1024];
                int bytesRead;
                while((bytesRead = in.read(databuffer, 0, 1024)) != -1){
                    fout.write(databuffer, 0, bytesRead);
                }
                fout.flush();
                fout.close();
                in.close();
                return true;
            }catch (MalformedURLException ex){
                System.out.println("MalformedURLException occured while downloading image");
            }catch(IOException ex){
                // might be thrown if output_file contains a directory that is not available, and creating the directory
                // may resolve the error.
                System.out.println("IOException occured while downloading image");
            }
            return false;
        }

        public String get_image_link(int index) throws org.json.JSONException{
            JSONArray search_result = json_response.getJSONArray("Search");//get value of "Search"
            JSONObject result = (JSONObject)search_result.get(index);
            return result.getString("Poster");
        }

        public String get_movie_name(int index) throws org.json.JSONException{
            // return movie title from the json result, if index is out of bound JSONException is thrown.
            JSONArray search_result = json_response.getJSONArray("Search");//get value of "Search"
            JSONObject result = (JSONObject)search_result.get(index); // throws JSONException if index is out of bound.
            return result.getString("Title");
        }

        public String type(){
            // Todo: check if the api json response a 'type' key:value
            return "";
        }

        public String genre(){
            // Todo: check if the api json response contains 'genre' key:value, if not found,
            // try to find/scrape the movie genre from an other site.
            return "";
        }

        public int result_count(){
            int count = 0;
            if(this.json_response){
                try{
                    count = this.json_response.getInt("totalResults");
                }catch(org.json.JSONException ex){
                    System.out.println("could not find key 'totalResults' in jsonobject! json might be corrupted");
                }
            }
            return count;
        }

	}
}