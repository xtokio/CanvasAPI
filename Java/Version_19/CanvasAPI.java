import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.*;

public class CanvasAPI
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        String user_id = "lgomezre";
        byte[] profile_image = get_photo();
        upload_profile_image(user_id,profile_image);
    }

    private static byte[] get_photo() throws IOException
    {
        Path profile_image = Paths.get("resize.png");
        byte[] file_bytes = Files.readAllBytes(profile_image);
        return file_bytes;
    }

    private static void upload_profile_image(String user_id,byte[] profile_image) throws IOException, InterruptedException
    {
        // MUST PROVIDE YOUR OWN base_url AND access_token VALUES
        String base_url = ""; // https://canvas.instructure.com/api/v1/users/sis_user_id:
        String token = ""; // Bearer 14072~XXXXXXXXXX
        String profile_image_name = user_id + ".jpg";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base_url+user_id+"/files"))
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString("{\n    \"name\": \""+profile_image_name+"\",\n    \"content_type\": \"image/jpeg\",\n    \"size\": "+profile_image.length+",\n    \"parent_folder_path\": \"profile pictures\", \n    \"as_user_id\": \"sis_user_id:"+user_id+"\"\n}"))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject response_json = new JSONObject(response.body());
        String upload_url = response_json.getString("upload_url");

        // Upload file
        String boundary = new BigInteger(10, new Random()).toString();

        // Result request body
        List<byte[]> byteArrays = new ArrayList<>();
        // Separator with boundary
        byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=").getBytes(StandardCharsets.UTF_8);
        // Opening boundary
        byteArrays.add(separator);
        String mimeType = "image/jpeg";
        byteArrays.add(("\"file\"; filename=\"" + profile_image_name + "\"\r\nContent-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(profile_image);
        byteArrays.add("\r\n".getBytes(StandardCharsets.UTF_8));
        // Closing boundary
        byteArrays.add(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

        HttpRequest request_file_custom =HttpRequest.newBuilder()
                .uri(URI.create(upload_url))
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                .method("POST",HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
                .build();
        HttpResponse<String> response_file_custom = HttpClient.newHttpClient().send(request_file_custom, HttpResponse.BodyHandlers.ofString());

        JSONObject response_file_custom_json = new JSONObject(response_file_custom.body());
        Integer response_file_custom_id = response_file_custom_json.getInt("id");

        // Use File id to get Token from list of Avatars
        HttpRequest request_avatars = HttpRequest.newBuilder()
                .uri(URI.create(base_url+user_id+"/avatars"))
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .method("GET", HttpRequest.BodyPublishers.ofString("{\n  \"sis_user_id\":\""+user_id+"\"\n}"))
                .build();
        HttpResponse<String> request_avatars_response = HttpClient.newHttpClient().send(request_avatars, HttpResponse.BodyHandlers.ofString());

        // Find Token based on the ID
        String avatar_token = "";
        JSONArray jsonAvatarData = new JSONArray(request_avatars_response.body());

        for (int i=0; i<jsonAvatarData.length(); i++)
        {
            JSONObject item = jsonAvatarData.getJSONObject(i);
            Integer id = 0;
            if(item.has("id"))
            {
                id = item.getInt("id");
            }

            if(response_file_custom_id.equals(id))
            {
                avatar_token = item.getString("token");
            }
        }

        // Setup profile picture in Canvas
        HttpRequest request_avatars_set = HttpRequest.newBuilder()
                .uri(URI.create(base_url+user_id))
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .method("PUT", HttpRequest.BodyPublishers.ofString("{\n  \"sis_user_id\":\""+user_id+"\", \"user\":{\"avatar\":{\"token\":\""+avatar_token+"\"}}\n}"))
                .build();
        HttpResponse<String> request_avatars_response_set = HttpClient.newHttpClient().send(request_avatars_set, HttpResponse.BodyHandlers.ofString());
        System.out.println(request_avatars_response_set.body());
    }
}
