import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import okhttp3.*; // Inside Modules folder
import org.json.JSONObject; // Inside Modules folder
import org.json.JSONArray; // Inside Modules folder

public class CanvasAPI {

    public static void main(String[] args) throws IOException, InterruptedException {
        String user_id = "lgomezre";
        byte[] profile_image = get_photo();
        upload_profile_image(user_id,profile_image);
    }

    private static byte[] get_photo() throws IOException
    {
        Path profile_image = FileSystems.getDefault().getPath("resize.png");
        byte[] file_bytes = Files.readAllBytes(profile_image);
        return file_bytes;
    }

    private static void upload_profile_image(String user_id,byte[] profile_image) throws IOException, InterruptedException
    {
        // MUST PROVIDE YOUR OWN base_url AND access_token VALUES
        String base_url = ""; // https://canvas.instructure.com/api/v1/users/sis_user_id:
        String token = ""; // Bearer 14072~XXXXXXXXXX
        MediaType json_type = MediaType.get("application/json; charset=utf-8");
        String profile_image_name = user_id + ".jpg";

        RequestBody body = RequestBody.create("{\n    \"name\": \""+profile_image_name+"\",\n    \"content_type\": \"image/jpeg\",\n    \"size\": "+profile_image.length+",\n    \"parent_folder_path\": \"profile pictures\", \n    \"as_user_id\": \"sis_user_id:"+user_id+"\"\n}", json_type);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .url(base_url+user_id+"/files")
                .post(body)
                .build();

        Response response = client.newCall(request).execute();
        JSONObject response_json = new JSONObject(response.body().string());
        String upload_url = response_json.getString("upload_url");

        // Upload image file
        MediaType mediaType = MediaType.parse("image/jpg");
        RequestBody requestBody = RequestBody.create(profile_image,mediaType);
        MultipartBody multipartBody = new MultipartBody.Builder("---CanvasAPI")
                .setType(MultipartBody.FORM)
                .addFormDataPart("Content-Type", "image/jpg")
                .addFormDataPart("file", null, requestBody)
                .addFormDataPart("filename", profile_image_name)
                .build();

        OkHttpClient client_upload_file = new OkHttpClient();
        Request request_upload_file = new Request.Builder()
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .url(upload_url)
                .post(multipartBody)
                .build();
        Response response_upload_file = client_upload_file.newCall(request_upload_file).execute();
        JSONObject response_file_custom_json = new JSONObject(response_upload_file.body().string());
        Integer response_file_custom_id = response_file_custom_json.getInt("id");

        // Use File id to get Token from list of Avatars
        OkHttpClient client_avatars = new OkHttpClient();
        Request request_avatars = new Request.Builder()
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .url(base_url+user_id+"/avatars").build();

        Response response_avatars = client_avatars.newCall(request_avatars).execute();
        String response_avatars_json = response_avatars.body().string();

        // Find Token based on the ID
        String avatar_token = "";
        JSONArray jsonAvatarData = new JSONArray(response_avatars_json);

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
        OkHttpClient client_set_profile_picture = new OkHttpClient();
        RequestBody body_set_profile_picture = RequestBody.create("{\n  \"sis_user_id\":\""+user_id+"\", \"user\":{\"avatar\":{\"token\":\""+avatar_token+"\"}}\n}",json_type);
        Request request_set_profile_picture = new Request.Builder()
                .header("Accept", "*/*")
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .url(base_url+user_id)
                .put(body_set_profile_picture).build();
        Response response_set_profile_picture = client_set_profile_picture.newCall(request_set_profile_picture).execute();
        System.out.println(response_set_profile_picture.body().string());
    }
}