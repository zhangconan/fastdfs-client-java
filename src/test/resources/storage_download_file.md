header

|位置|内容|
|----|----|
|0-7 |包总长度|
|8   |命令|
|9   |状态|

STORAGE_PROTO_CMD_DOWNLOAD_FILE下载文件  

|位置|内容|
|----|----|
|0-9 |header(STORAGE_PROTO_CMD_DOWNLOAD_FILE)|
|10-17|offset|
|18-25|downloadfile length|
|26-31|group name|
|32-  |fileName|
|返回:|        |
|0-9  |header(STORAGE_PROTO_CMD_RESP)|







