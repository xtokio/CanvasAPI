import os
import requests
import mimetypes

def change_avatar(user_id,image_path):
    # MUST PROVIDE YOUR OWN base_url AND access_token VALUES
    base_url = "" # f"https://canvas.instructure.com/api/v1/users/sis_user_id:{user_id}"
    access_token = "" # "14072~XXXXXXXX"
    header = {'Authorization' : 'Bearer {0}'.format(access_token)}

    # Get upload_url param
    upload_url_params = {
        'name': os.path.basename(image_path),
        'content_type': mimetypes.guess_type(image_path),
        'size': os.path.getsize(image_path),
        'parent_folder_path': 'profile pictures', 
        'as_user_id': f"sis_user_id:{user_id}"
    }
    response_upload_url = requests.post(base_url+"/files",headers=header,data=upload_url_params)
    data = response_upload_url.json()
    # Response file upload
    files = {'file':open(image_path,'rb').read()}
    upload_params = data.get('upload_params')
    upload_url = data.get('upload_url')
    response_upload_file = requests.post(upload_url, data=upload_params, files=files, allow_redirects=False)
    
    avatar_id = 0
    if 'id' in response_upload_file.json():
        avatar_id = response_upload_file.json()['id']

    # Find recently uploaded image and get the user token
    params = {'as_user_id': f"sis_user_id:{user_id}"}
    response_avatars = requests.get(base_url+"/avatars",headers=header,params=params)
    
    avatar_token = ""
    for avatar in response_avatars.json():
        if avatar.get('id') == avatar_id:
            avatar_token = avatar.get('token')
            params['user[avatar][token]'] = avatar_token
            set_avatar_user = requests.put(base_url, headers=header, params=params)
            print(set_avatar_user)
            if set_avatar_user.status_code == 200:
                print(f'Profile image set for user - {user_id}')
            else:
                print('Failed to set profile image for user - {user_id}')

# Change user profile picture
user_id = "lgomezre"
image_path = "resize.png"
change_avatar(user_id,image_path)
