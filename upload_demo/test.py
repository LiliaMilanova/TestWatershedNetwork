#!/usr/bin/env python
import requests
url = 'http://129.63.16.66:8000/image/'
headers = {'Content-type': 'multipart/form-data'}
files = {'image': open("./ic_launcher.png", 'rb')}
r = requests.post(url, files=files, headers=headers)
# check Response
print r.status_code
