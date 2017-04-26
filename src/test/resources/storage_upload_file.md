#### STORAGE_PROTO_CMD_UPLOAD_FILE 上传文件   
 
|位置|内容| 
|----|----|
|0-9 |header(STORAGE_PROTO_CMD_UPLOAD_FILE,body_len,0)|
|10  |storePathIndex(0)|
|11-18|file_size|
|19-24|FDFS_FILE_EXT_NAME_MAX_LEN|
|25-  |文件内容|
|返回：|       |
|0-7|pkg_len|
|8|STORAGE_PROTO_CMD_RESP|
|9|0|
|10-25|group_name|
|26-  |remote_filename|