#### STORAGE_PROTO_CMD_SET_METADATA 设置meta  
|位置|内容|
|----|----|
|0-7|16+1+groupBytes.length+filenameBytes.length+meta_buff.length|
|8|STORAGE_PROTO_CMD_SET_METADATA|
|9|0|
|10-17|filenameBytes.length|
|18-25|meta_buff.length|
|26|STORAGE_SET_METADATA_FLAG_OVERWRITE|
|27-42|group_name|
|43-|file_name|
|-|meta_buff|
|返回:|
|0-7|pkg_len|
|8|STORAGE_PROTO_CMD_RESP|
|9|0|
