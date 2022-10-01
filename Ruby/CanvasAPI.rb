require 'mime/types'
require 'HTTParty'
require 'json'

def change_avatar(user_id,image_path)  
    # MUST PROVIDE YOUR OWN base_url AND access_token VALUES
    base_url = "" # https://canvas.instructure.com/api/v1/users/sis_user_id:#{user_id}
    token = "" # Bearer 14072~XXXXXXXXX
    headers = { 'Content-Type' => 'application/json;','Accept' => '*/*', 'Authorization' => token}

  # Get upload_url param
  response_upload_url = HTTParty.post(base_url+"/files",
    :headers => headers,
    :body => {
      :name => File.basename(image_path),
      :content_type => MIME::Types.type_for(image_path),
      :size => File.size(image_path),
      :parent_folder_path => 'profile pictures', 
      :as_user_id => "sis_user_id:#{user_id}"
    }.to_json
  )
  upload_url = response_upload_url["upload_url"]

  # Response file upload
  response_file_upload = HTTParty.post(upload_url,
    body: { filename: File.basename(image_path), content_type: MIME::Types.type_for(image_path), file: File.open(image_path) }
  )
  avatar_id = response_file_upload["id"]
  avatar_token = nil

  # Find recently uploaded image and get the user token
  response_avatars = HTTParty.get(base_url+"/avatars",headers: headers)
  avatar_token = response_avatars.select {|x| x['id']==avatar_id}.first['token']

  if !avatar_token.nil?
    # Set Avatar Image
    response_set_avatar = HTTParty.put(base_url,
      :headers => headers,
      :body => { :user => { :avatar => { :token => avatar_token } } }.to_json 
    )
    puts "Avatar was set"
    puts response_set_avatar
  end

end

# Set user_id
user_id = "lgomezre"
# Set to image to be changed
image_path = "resize.png"

change_avatar(user_id,image_path)