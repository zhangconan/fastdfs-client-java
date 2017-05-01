#### STORAGE_PROTO_CMD_UPLOAD_SLAVE_FILE 上传从文件   
 
|位置|内容| 
|----|----|
|0-7 |body_len|
|8|STORAGE_PROTO_CMD_UPLOAD_SLAVE_FILE|
|9|0|
|10-17|主文件名长度master_filename.length()|
|18-25|文件大小file_size|
|26-41|FDFS_FILE_PREFIX_MAX_LEN|
|42-47|后缀名ext_name_bs|
|48-|主文件名master_filename|
|返回：|       |
|0-7|pkg_len|
|8|STORAGE_PROTO_CMD_RESP|
|9|0|
|10-25|group_name|
|26-  |remote_filename|

body_len = 16 + 16 + 6 +  master_filenameBytes.length + file_size
                    
                  