#### STORAGE_PROTO_CMD_DELETE_FILE 上传从文件   
 
|位置|内容| 
|----|----|
|0-7 |body_len|
|8|STORAGE_PROTO_CMD_DELETE_FILE|
|9|0|
|10-25|groupBytes|
|26-|filenameBytes|
|返回：|       |
|0-7|pkg_len|
|8|STORAGE_PROTO_CMD_RESP|
|9|0|