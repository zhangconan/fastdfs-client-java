/**
 * Copyright (C) 2008 Happy Fish / YuQing
 *
 * FastDFS Java Client may be copied only under the terms of the GNU Lesser
 * General Public License (LGPL).
 * Please visit the FastDFS Home Page http://www.csource.org/ for more detail.
 */

package org.csource.fastdfs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.util.Arrays;
import java.net.Socket;

import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.common.Base64;

/**
 * Storage client for 2 fields file id: group name and filename
 *
 * @author Happy Fish / YuQing
 * @version Version 1.24
 */
public class StorageClient {
    /**
     * Upload file by file buff
     *
     * @author Happy Fish / YuQing
     * @version Version 1.12
     */
    public static class UploadBuff implements UploadCallback {
        private byte[] fileBuff;
        private int offset;
        private int length;

        /**
         * constructor
         *
         * @param fileBuff the file buff for uploading
         */
        public UploadBuff(byte[] fileBuff, int offset, int length) {
            super();
            this.fileBuff = fileBuff;
            this.offset = offset;
            this.length = length;
        }

        /**
         * send file content callback function, be called only once when the file uploaded
         *
         * @param out output stream for writing file content
         * @return 0 success, return none zero(errno) if fail
         */
        public int send(OutputStream out) throws IOException {
            out.write(this.fileBuff, this.offset, this.length);

            return 0;
        }
    }

    public final static Base64 base64 = new Base64('-', '_', '.', 0);
    protected TrackerServer trackerServer;
    protected StorageServer storageServer;
    protected byte errno;

    /**
     * constructor using global settings in class ClientGlobal
     */
    public StorageClient() {
        this.trackerServer = null;
        this.storageServer = null;
    }

    /**
     * constructor with tracker server and storage server
     *
     * @param trackerServer the tracker server, can be null
     * @param storageServer the storage server, can be null
     */
    public StorageClient(TrackerServer trackerServer, StorageServer storageServer) {
        this.trackerServer = trackerServer;
        this.storageServer = storageServer;
    }

    /**
     * get the error code of last call
     *
     * @return the error code of last call
     */
    public byte getErrorCode() {
        return this.errno;
    }

    /**
     * upload file to storage server (by file name)
     *
     * @param local_filename local filename to upload
     * @param file_ext_name  file ext name, do not include dot(.), null to extract ext name from the local filename
     * @param meta_list      meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file </li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(String local_filename, String file_ext_name,
                                NameValuePair[] meta_list) throws IOException, MyException {
        final String group_name = null;
        return this.upload_file(group_name, local_filename, file_ext_name, meta_list);
    }

    /**
     * upload file to storage server (by file name)
     *
     * @param group_name     the group name to upload file to, can be empty
     * @param local_filename local filename to upload
     * @param file_ext_name  file ext name, do not include dot(.), null to extract ext name from the local filename
     * @param meta_list      meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file </li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    protected String[] upload_file(String group_name, String local_filename, String file_ext_name,
                                   NameValuePair[] meta_list) throws IOException, MyException {
        final byte cmd = ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_FILE;
        return this.upload_file(cmd, group_name, local_filename, file_ext_name, meta_list);
    }

    /**
     * upload file to storage server (by file name)
     *
     * @param cmd            the command
     * @param group_name     the group name to upload file to, can be empty
     * @param local_filename local filename to upload
     * @param file_ext_name  file ext name, do not include dot(.), null to extract ext name from the local filename
     * @param meta_list      meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file </li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    protected String[] upload_file(byte cmd, String group_name, String local_filename, String file_ext_name,
                                   NameValuePair[] meta_list) throws IOException, MyException {
        File f = new File(local_filename);
        FileInputStream fis = new FileInputStream(f);
        //取后缀名
        if (file_ext_name == null) {
            int nPos = local_filename.lastIndexOf('.');
            if (nPos > 0 && local_filename.length() - nPos <= ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN + 1) {
                file_ext_name = local_filename.substring(nPos + 1);
            }
        }

        try {
            return this.do_upload_file(cmd, group_name, null, null, file_ext_name,
                f.length(), new UploadStream(fis, f.length()), meta_list);
        } finally {
            fis.close();
        }
    }

    /**
     * upload file to storage server (by file buff)
     *
     * @param file_buff     file content/buff
     * @param offset        start offset of the buff
     * @param length        the length of buff to upload
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(byte[] file_buff, int offset, int length, String file_ext_name,
                                NameValuePair[] meta_list) throws IOException, MyException {
        final String group_name = null;
        return this.upload_file(group_name, file_buff, offset, length, file_ext_name, meta_list);
    }

    /**
     * upload file to storage server (by file buff)
     * @param group_name the group name to upload file to, can be empty
     * @param file_buff file content/buff
     * @param offset start offset of the buff
     * @param length the length of buff to upload
     * @param file_ext_name file ext name, do not include dot(.)
     *	@param meta_list meta info array
     * @return 2 elements string array if success:<br>
     *           <ul><li>results[0]: the group name to store the file</li></ul>
     *           <ul><li>results[1]: the new created filename</li></ul>
     *         return null if fail
     */
    /**
     * @param group_name    null
     * @param file_buff     要上传文件的字节流
     * @param offset        0
     * @param length        file_buff.length
     * @param file_ext_name 不带.的后缀名
     * @param meta_list
     * @return
     * @throws IOException
     * @throws MyException
     */
    public String[] upload_file(String group_name, byte[] file_buff, int offset, int length,
                                String file_ext_name, NameValuePair[] meta_list) throws IOException, MyException {
        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_FILE, group_name, null, null, file_ext_name,
            length, new UploadBuff(file_buff, offset, length), meta_list);
    }

    /**
     * upload file to storage server (by file buff)
     *
     * @param file_buff     file content/buff
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(byte[] file_buff, String file_ext_name,
                                NameValuePair[] meta_list) throws IOException, MyException {
        final String group_name = null;
        return this.upload_file(group_name, file_buff, 0, file_buff.length, file_ext_name, meta_list);
    }

    /**
     * upload file to storage server (by file buff)
     *
     * @param group_name    the group name to upload file to, can be empty
     * @param file_buff     file content/buff
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(String group_name, byte[] file_buff,
                                String file_ext_name, NameValuePair[] meta_list) throws IOException, MyException {
        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_FILE, group_name, null, null, file_ext_name,
            file_buff.length, new UploadBuff(file_buff, 0, file_buff.length), meta_list);
    }

    /**
     * upload file to storage server (by callback)
     *
     * @param group_name    the group name to upload file to, can be empty
     * @param file_size     the file size
     * @param callback      the write data callback object
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(String group_name, long file_size, UploadCallback callback,
                                String file_ext_name, NameValuePair[] meta_list) throws IOException, MyException {
        final String master_filename = null;
        final String prefix_name = null;

        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_FILE, group_name, master_filename, prefix_name,
            file_ext_name, file_size, callback, meta_list);
    }

    /**
     * upload file to storage server (by file name, slave file mode)
     *
     * @param group_name      the group name of master file
     * @param master_filename the master file name to generate the slave file
     * @param prefix_name     the prefix name to generate the slave file
     * @param local_filename  local filename to upload
     * @param file_ext_name   file ext name, do not include dot(.), null to extract ext name from the local filename
     * @param meta_list       meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file </li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(String group_name, String master_filename, String prefix_name,
                                String local_filename, String file_ext_name, NameValuePair[] meta_list)
        throws IOException, MyException {
        if ((group_name == null || group_name.length() == 0) ||
            (master_filename == null || master_filename.length() == 0) ||
            (prefix_name == null)) {
            throw new MyException("invalid arguement");
        }

        File f = new File(local_filename);
        FileInputStream fis = new FileInputStream(f);

        if (file_ext_name == null) {
            int nPos = local_filename.lastIndexOf('.');
            if (nPos > 0 && local_filename.length() - nPos <= ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN + 1) {
                file_ext_name = local_filename.substring(nPos + 1);
            }
        }

        try {
            return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_SLAVE_FILE, group_name, master_filename,
                prefix_name,
                file_ext_name, f.length(), new UploadStream(fis, f.length()), meta_list);
        } finally {
            fis.close();
        }
    }

    /**
     * upload file to storage server (by file buff, slave file mode)
     *
     * @param group_name      the group name of master file
     * @param master_filename the master file name to generate the slave file
     * @param prefix_name     the prefix name to generate the slave file
     * @param file_buff       file content/buff
     * @param file_ext_name   file ext name, do not include dot(.)
     * @param meta_list       meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(String group_name, String master_filename, String prefix_name,
                                byte[] file_buff, String file_ext_name, NameValuePair[] meta_list)
        throws IOException, MyException {
        if ((group_name == null || group_name.length() == 0) ||
            (master_filename == null || master_filename.length() == 0) ||
            (prefix_name == null)) {
            throw new MyException("invalid arguement");
        }

        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_SLAVE_FILE, group_name, master_filename,
            prefix_name,
            file_ext_name, file_buff.length, new UploadBuff(file_buff, 0, file_buff.length), meta_list);
    }

    /**
     * upload file to storage server (by file buff, slave file mode)
     *
     * @param group_name      the group name of master file
     * @param master_filename the master file name to generate the slave file
     * @param prefix_name     the prefix name to generate the slave file
     * @param file_buff       file content/buff
     * @param offset          start offset of the buff
     * @param length          the length of buff to upload
     * @param file_ext_name   file ext name, do not include dot(.)
     * @param meta_list       meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(String group_name, String master_filename, String prefix_name,
                                byte[] file_buff, int offset, int length, String file_ext_name,
                                NameValuePair[] meta_list) throws IOException, MyException {
        if ((group_name == null || group_name.length() == 0) ||
            (master_filename == null || master_filename.length() == 0) ||
            (prefix_name == null)) {
            throw new MyException("invalid arguement");
        }

        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_SLAVE_FILE, group_name, master_filename,
            prefix_name,
            file_ext_name, length, new UploadBuff(file_buff, offset, length), meta_list);
    }

    /**
     * upload file to storage server (by callback, slave file mode)
     *
     * @param group_name      the group name to upload file to, can be empty
     * @param master_filename the master file name to generate the slave file
     * @param prefix_name     the prefix name to generate the slave file
     * @param file_size       the file size
     * @param callback        the write data callback object
     * @param file_ext_name   file ext name, do not include dot(.)
     * @param meta_list       meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_file(String group_name, String master_filename,
                                String prefix_name, long file_size, UploadCallback callback,
                                String file_ext_name, NameValuePair[] meta_list) throws IOException, MyException {
        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_SLAVE_FILE, group_name, master_filename,
            prefix_name,
            file_ext_name, file_size, callback, meta_list);
    }

    /**
     * upload appender file to storage server (by file name)
     *
     * @param local_filename local filename to upload
     * @param file_ext_name  file ext name, do not include dot(.), null to extract ext name from the local filename
     * @param meta_list      meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file </li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_appender_file(String local_filename, String file_ext_name,
                                         NameValuePair[] meta_list) throws IOException, MyException {
        final String group_name = null;
        return this.upload_appender_file(group_name, local_filename, file_ext_name, meta_list);
    }

    /**
     * upload appender file to storage server (by file name)
     *
     * @param group_name     the group name to upload file to, can be empty
     * @param local_filename local filename to upload
     * @param file_ext_name  file ext name, do not include dot(.), null to extract ext name from the local filename
     * @param meta_list      meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file </li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    protected String[] upload_appender_file(String group_name, String local_filename, String file_ext_name,
                                            NameValuePair[] meta_list) throws IOException, MyException {
        final byte cmd = ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_APPENDER_FILE;
        return this.upload_file(cmd, group_name, local_filename, file_ext_name, meta_list);
    }

    /**
     * upload appender file to storage server (by file buff)
     *
     * @param file_buff     file content/buff
     * @param offset        start offset of the buff
     * @param length        the length of buff to upload
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_appender_file(byte[] file_buff, int offset, int length, String file_ext_name,
                                         NameValuePair[] meta_list) throws IOException, MyException {
        final String group_name = null;
        return this.upload_appender_file(group_name, file_buff, offset, length, file_ext_name, meta_list);
    }

    /**
     * upload appender file to storage server (by file buff)
     *
     * @param group_name    the group name to upload file to, can be empty
     * @param file_buff     file content/buff
     * @param offset        start offset of the buff
     * @param length        the length of buff to upload
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_appender_file(String group_name, byte[] file_buff, int offset, int length,
                                         String file_ext_name, NameValuePair[] meta_list)
        throws IOException, MyException {
        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_APPENDER_FILE, group_name, null, null,
            file_ext_name,
            length, new UploadBuff(file_buff, offset, length), meta_list);
    }

    /**
     * upload appender file to storage server (by file buff)
     *
     * @param file_buff     file content/buff
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_appender_file(byte[] file_buff, String file_ext_name,
                                         NameValuePair[] meta_list) throws IOException, MyException {
        final String group_name = null;
        return this.upload_appender_file(group_name, file_buff, 0, file_buff.length, file_ext_name, meta_list);
    }

    /**
     * upload appender file to storage server (by file buff)
     *
     * @param group_name    the group name to upload file to, can be empty
     * @param file_buff     file content/buff
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_appender_file(String group_name, byte[] file_buff,
                                         String file_ext_name, NameValuePair[] meta_list)
        throws IOException, MyException {
        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_APPENDER_FILE, group_name, null, null,
            file_ext_name,
            file_buff.length, new UploadBuff(file_buff, 0, file_buff.length), meta_list);
    }

    /**
     * upload appender file to storage server (by callback)
     *
     * @param group_name    the group name to upload file to, can be empty
     * @param file_size     the file size
     * @param callback      the write data callback object
     * @param file_ext_name file ext name, do not include dot(.)
     * @param meta_list     meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li>results[0]: the group name to store the file</li></ul>
     * <ul><li>results[1]: the new created filename</li></ul>
     * return null if fail
     */
    public String[] upload_appender_file(String group_name, long file_size, UploadCallback callback,
                                         String file_ext_name, NameValuePair[] meta_list)
        throws IOException, MyException {
        final String master_filename = null;
        final String prefix_name = null;

        return this.do_upload_file(ProtoCommon.STORAGE_PROTO_CMD_UPLOAD_APPENDER_FILE, group_name, master_filename,
            prefix_name,
            file_ext_name, file_size, callback, meta_list);
    }

    /**
     * append file to storage server (by file name)
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param local_filename    local filename to append
     * @return 0 for success, != 0 for error (error no)
     */
    public int append_file(String group_name, String appender_filename, String local_filename)
        throws IOException, MyException {
        File f = new File(local_filename);
        FileInputStream fis = new FileInputStream(f);

        try {
            return this.do_append_file(group_name, appender_filename, f.length(), new UploadStream(fis, f.length()));
        } finally {
            fis.close();
        }
    }

    /**
     * append file to storage server (by file buff)
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param file_buff         file content/buff
     * @return 0 for success, != 0 for error (error no)
     */
    public int append_file(String group_name, String appender_filename, byte[] file_buff)
        throws IOException, MyException {
        return this.do_append_file(group_name, appender_filename, file_buff.length,
            new UploadBuff(file_buff, 0, file_buff.length));
    }

    /**
     * append file to storage server (by file buff)
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param file_buff         file content/buff
     * @param offset            start offset of the buff
     * @param length            the length of buff to append
     * @return 0 for success, != 0 for error (error no)
     */
    public int append_file(String group_name, String appender_filename,
                           byte[] file_buff, int offset, int length) throws IOException, MyException {
        return this.do_append_file(group_name, appender_filename, length, new UploadBuff(file_buff, offset, length));
    }

    /**
     * append file to storage server (by callback)
     *
     * @param group_name        the group name to append file to
     * @param appender_filename the appender filename
     * @param file_size         the file size
     * @param callback          the write data callback object
     * @return 0 for success, != 0 for error (error no)
     */
    public int append_file(String group_name, String appender_filename,
                           long file_size, UploadCallback callback) throws IOException, MyException {
        return this.do_append_file(group_name, appender_filename, file_size, callback);
    }

    /**
     * modify appender file to storage server (by file name)
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param file_offset       the offset of appender file
     * @param local_filename    local filename to append
     * @return 0 for success, != 0 for error (error no)
     */
    public int modify_file(String group_name, String appender_filename,
                           long file_offset, String local_filename) throws IOException, MyException {
        File f = new File(local_filename);
        FileInputStream fis = new FileInputStream(f);

        try {
            return this.do_modify_file(group_name, appender_filename, file_offset,
                f.length(), new UploadStream(fis, f.length()));
        } finally {
            fis.close();
        }
    }

    /**
     * modify appender file to storage server (by file buff)
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param file_offset       the offset of appender file
     * @param file_buff         file content/buff
     * @return 0 for success, != 0 for error (error no)
     */
    public int modify_file(String group_name, String appender_filename,
                           long file_offset, byte[] file_buff) throws IOException, MyException {
        return this.do_modify_file(group_name, appender_filename, file_offset,
            file_buff.length, new UploadBuff(file_buff, 0, file_buff.length));
    }

    /**
     * modify appender file to storage server (by file buff)
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param file_offset       the offset of appender file
     * @param file_buff         file content/buff
     * @param buffer_offset     start offset of the buff
     * @param buffer_length     the length of buff to modify
     * @return 0 for success, != 0 for error (error no)
     */
    public int modify_file(String group_name, String appender_filename,
                           long file_offset, byte[] file_buff, int buffer_offset, int buffer_length)
        throws IOException, MyException {
        return this.do_modify_file(group_name, appender_filename, file_offset,
            buffer_length, new UploadBuff(file_buff, buffer_offset, buffer_length));
    }

    /**
     * modify appender file to storage server (by callback)
     *
     * @param group_name        the group name to modify file to
     * @param appender_filename the appender filename
     * @param file_offset       the offset of appender file
     * @param modify_size       the modify size
     * @param callback          the write data callback object
     * @return 0 for success, != 0 for error (error no)
     */
    public int modify_file(String group_name, String appender_filename,
                           long file_offset, long modify_size, UploadCallback callback)
        throws IOException, MyException {
        return this.do_modify_file(group_name, appender_filename, file_offset,
            modify_size, callback);
    }

    /**
     * upload file to storage server
     *
     * @param cmd             the command code
     * @param group_name      the group name to upload file to, can be empty
     * @param master_filename the master file name to generate the slave file
     * @param prefix_name     the prefix name to generate the slave file
     * @param file_ext_name   file ext name, do not include dot(.)
     * @param file_size       the file size
     * @param callback        the write data callback object
     * @param meta_list       meta info array
     * @return 2 elements string array if success:<br>
     * <ul><li> results[0]: the group name to store the file</li></ul>
     * <ul><li> results[1]: the new created filename</li></ul>
     * return null if fail
     */
    protected String[] do_upload_file(byte cmd, String group_name, String master_filename,
                                      String prefix_name, String file_ext_name, long file_size, UploadCallback callback,
                                      NameValuePair[] meta_list) throws IOException, MyException {
        //STORAGE_PROTO_CMD_UPLOAD_FILE
        //头信息
        byte[] header;
        byte[] ext_name_bs;
        String new_group_name;
        String remote_filename;
        //是不是新的连接
        boolean bNewConnection;
        //存储器Socket
        Socket storageSocket;
        //字节数组
        byte[] sizeBytes;
        //主文件名 转换为的字节数组
        byte[] hexLenBytes;
        //主文件的字节数组
        byte[] masterFilenameBytes;
        boolean bUploadSlave;
        //偏移量
        int offset;
        long body_len;
        //是否上传从文件
        bUploadSlave = ((group_name != null && group_name.length() > 0) &&
            (master_filename != null && master_filename.length() > 0) &&
            (prefix_name != null));
        if (bUploadSlave) {
            bNewConnection = this.newUpdatableStorageConnection(group_name, master_filename);
        } else {
            bNewConnection = this.newWritableStorageConnection(group_name);
        }

        try {
            storageSocket = this.storageServer.getSocket();
            //后缀名的长度6个字节
            ext_name_bs = new byte[ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN];
            //用0填满后缀名的字节数组
            Arrays.fill(ext_name_bs, (byte)0);
            if (file_ext_name != null && file_ext_name.length() > 0) {
                byte[] bs = file_ext_name.getBytes(ClientGlobal.g_charset);
                int ext_name_len = bs.length;
                if (ext_name_len > ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN) {
                    ext_name_len = ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN;
                }
                //将后缀名的字节数组传入到ext_name_bs字节数组中
                System.arraycopy(bs, 0, ext_name_bs, 0, ext_name_len);
            }
            //如果上传的是从文件
            if (bUploadSlave) {
                //获取主文件的字节数组
                masterFilenameBytes = master_filename.getBytes(ClientGlobal.g_charset);
                //size字节数组为16个字节元素
                sizeBytes = new byte[2 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE];
                //消息体的长度 16+16+6+主文件的字节数组长度+文件的长度
                body_len = sizeBytes.length + ProtoCommon.FDFS_FILE_PREFIX_MAX_LEN
                    + ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN
                    + masterFilenameBytes.length + file_size;
                //将主文件名的长度转换为字节数组 BigEndian
                hexLenBytes = ProtoCommon.long2buff(master_filename.length());
                //sizeBytes前八个字节长度为 主文件名
                System.arraycopy(hexLenBytes, 0, sizeBytes, 0, hexLenBytes.length);
                offset = hexLenBytes.length;
            } else {
                masterFilenameBytes = null;
                //sizeBytes为9个字节长度的数组
                sizeBytes = new byte[1 + 1 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE];
                //消息体的长度 9+6+文件长度
                body_len = sizeBytes.length + ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN + file_size;
                //sizeBytes的第一个字节为存储服务器上路径索引
                sizeBytes[0] = (byte)this.storageServer.getStorePathIndex();
                //偏移量
                offset = 1;
            }
            //将文件长度转换为字节数组 BigEndian
            hexLenBytes = ProtoCommon.long2buff(file_size);
            //sizeBytes的后几位存储的值为 文件长度  sizeBytes存储的内容为：主文件名的长度+文件的长度
            System.arraycopy(hexLenBytes, 0, sizeBytes, offset, hexLenBytes.length);
            //获取存储服务器的输出流
            OutputStream out = storageSocket.getOutputStream();
            //头字节数组信息(十长度)：前八位：要发送的消息的长度 第九位：命令 第十位：状态
            header = ProtoCommon.packHeader(cmd, body_len, (byte)0);
            //整个包的长度：头信息+包体的长度-文件的长度
            byte[] wholePkg = new byte[(int)(header.length + body_len - file_size)];
            //填充整个包的数据
            System.arraycopy(header, 0, wholePkg, 0, header.length);
            System.arraycopy(sizeBytes, 0, wholePkg, header.length, sizeBytes.length);
            //偏移量
            offset = header.length + sizeBytes.length;
            if (bUploadSlave) {
                //如果上传的是从文件
                //获取前缀文件名
                byte[] prefix_name_bs = new byte[ProtoCommon.FDFS_FILE_PREFIX_MAX_LEN];
                //前缀文件名转换为字节数组
                byte[] bs = prefix_name.getBytes(ClientGlobal.g_charset);
                int prefix_name_len = bs.length;
                Arrays.fill(prefix_name_bs, (byte)0);
                //只有前16位
                if (prefix_name_len > ProtoCommon.FDFS_FILE_PREFIX_MAX_LEN) {
                    prefix_name_len = ProtoCommon.FDFS_FILE_PREFIX_MAX_LEN;
                }
                if (prefix_name_len > 0) {
                    System.arraycopy(bs, 0, prefix_name_bs, 0, prefix_name_len);
                }
                //wholePkg = header+sizeBytes+prefix_name_bs
                System.arraycopy(prefix_name_bs, 0, wholePkg, offset, prefix_name_bs.length);
                //偏移量
                offset += prefix_name_bs.length;
            }
            //wholePkg = header+sizeBytes+prefix_name_bs+ext_name_bs
            System.arraycopy(ext_name_bs, 0, wholePkg, offset, ext_name_bs.length);
            //改变偏移量
            offset += ext_name_bs.length;
            //如果上传的是从文件名
            if (bUploadSlave) {
                //wholePkg = header+sizeBytes+[prefix_name_bs]+ext_name_bs+[masterFilenameBytes]
                System.arraycopy(masterFilenameBytes, 0, wholePkg, offset, masterFilenameBytes.length);
                offset += masterFilenameBytes.length;
            }

            out.write(wholePkg);
            //发送消息
            if ((this.errno = (byte)callback.send(out)) != 0) {
                return null;
            }
            //接收返回消息
            ProtoCommon.RecvPackageInfo pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, -1);
            this.errno = pkgInfo.errno;
            if (pkgInfo.errno != 0) {
                return null;
            }

            if (pkgInfo.body.length <= ProtoCommon.FDFS_GROUP_NAME_MAX_LEN) {
                throw new MyException(
                    "body length: " + pkgInfo.body.length + " <= " + ProtoCommon.FDFS_GROUP_NAME_MAX_LEN);
            }
            //获取group名字
            new_group_name = new String(pkgInfo.body, 0, ProtoCommon.FDFS_GROUP_NAME_MAX_LEN).trim();
            //获取远程文件名
            remote_filename = new String(pkgInfo.body, ProtoCommon.FDFS_GROUP_NAME_MAX_LEN,
                pkgInfo.body.length - ProtoCommon.FDFS_GROUP_NAME_MAX_LEN);
            String[] results = new String[2];
            results[0] = new_group_name;
            results[1] = remote_filename;

            if (meta_list == null || meta_list.length == 0) {
                return results;
            }

            int result = 0;
            try {
                //重写meta
                result = this.set_metadata(new_group_name, remote_filename,
                    meta_list, ProtoCommon.STORAGE_SET_METADATA_FLAG_OVERWRITE);
            } catch (IOException ex) {
                result = 5;
                throw ex;
            } finally {
                if (result != 0) {
                    this.errno = (byte)result;
                    this.delete_file(new_group_name, remote_filename);
                    return null;
                }
            }

            return results;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * append file to storage server
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param file_size         the file size
     * @param callback          the write data callback object
     * @return return true for success, false for fail
     */
    protected int do_append_file(String group_name, String appender_filename,
                                 long file_size, UploadCallback callback) throws IOException, MyException {
        byte[] header;
        boolean bNewConnection;
        Socket storageSocket;
        byte[] hexLenBytes;
        byte[] appenderFilenameBytes;
        int offset;
        long body_len;

        if ((group_name == null || group_name.length() == 0) ||
            (appender_filename == null || appender_filename.length() == 0)) {
            this.errno = ProtoCommon.ERR_NO_EINVAL;
            return this.errno;
        }

        bNewConnection = this.newUpdatableStorageConnection(group_name, appender_filename);

        try {
            storageSocket = this.storageServer.getSocket();

            appenderFilenameBytes = appender_filename.getBytes(ClientGlobal.g_charset);
            body_len = 2 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE + appenderFilenameBytes.length + file_size;

            header = ProtoCommon.packHeader(ProtoCommon.STORAGE_PROTO_CMD_APPEND_FILE, body_len, (byte)0);
            byte[] wholePkg = new byte[(int)(header.length + body_len - file_size)];
            System.arraycopy(header, 0, wholePkg, 0, header.length);
            offset = header.length;

            hexLenBytes = ProtoCommon.long2buff(appender_filename.length());
            System.arraycopy(hexLenBytes, 0, wholePkg, offset, hexLenBytes.length);
            offset += hexLenBytes.length;

            hexLenBytes = ProtoCommon.long2buff(file_size);
            System.arraycopy(hexLenBytes, 0, wholePkg, offset, hexLenBytes.length);
            offset += hexLenBytes.length;

            OutputStream out = storageSocket.getOutputStream();

            System.arraycopy(appenderFilenameBytes, 0, wholePkg, offset, appenderFilenameBytes.length);
            offset += appenderFilenameBytes.length;

            out.write(wholePkg);
            if ((this.errno = (byte)callback.send(out)) != 0) {
                return this.errno;
            }

            ProtoCommon.RecvPackageInfo pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, 0);
            this.errno = pkgInfo.errno;
            if (pkgInfo.errno != 0) {
                return this.errno;
            }

            return 0;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * modify appender file to storage server
     *
     * @param group_name        the group name of appender file
     * @param appender_filename the appender filename
     * @param file_offset       the offset of appender file
     * @param modify_size       the modify size
     * @param callback          the write data callback object
     * @return return true for success, false for fail
     */
    protected int do_modify_file(String group_name, String appender_filename,
                                 long file_offset, long modify_size, UploadCallback callback)
        throws IOException, MyException {
        byte[] header;
        boolean bNewConnection;
        Socket storageSocket;
        byte[] hexLenBytes;
        byte[] appenderFilenameBytes;
        int offset;
        long body_len;

        if ((group_name == null || group_name.length() == 0) ||
            (appender_filename == null || appender_filename.length() == 0)) {
            this.errno = ProtoCommon.ERR_NO_EINVAL;
            return this.errno;
        }

        bNewConnection = this.newUpdatableStorageConnection(group_name, appender_filename);

        try {
            storageSocket = this.storageServer.getSocket();

            appenderFilenameBytes = appender_filename.getBytes(ClientGlobal.g_charset);
            body_len = 3 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE + appenderFilenameBytes.length + modify_size;

            header = ProtoCommon.packHeader(ProtoCommon.STORAGE_PROTO_CMD_MODIFY_FILE, body_len, (byte)0);
            byte[] wholePkg = new byte[(int)(header.length + body_len - modify_size)];
            System.arraycopy(header, 0, wholePkg, 0, header.length);
            offset = header.length;

            hexLenBytes = ProtoCommon.long2buff(appender_filename.length());
            System.arraycopy(hexLenBytes, 0, wholePkg, offset, hexLenBytes.length);
            offset += hexLenBytes.length;

            hexLenBytes = ProtoCommon.long2buff(file_offset);
            System.arraycopy(hexLenBytes, 0, wholePkg, offset, hexLenBytes.length);
            offset += hexLenBytes.length;

            hexLenBytes = ProtoCommon.long2buff(modify_size);
            System.arraycopy(hexLenBytes, 0, wholePkg, offset, hexLenBytes.length);
            offset += hexLenBytes.length;

            OutputStream out = storageSocket.getOutputStream();

            System.arraycopy(appenderFilenameBytes, 0, wholePkg, offset, appenderFilenameBytes.length);
            offset += appenderFilenameBytes.length;

            out.write(wholePkg);
            if ((this.errno = (byte)callback.send(out)) != 0) {
                return this.errno;
            }

            ProtoCommon.RecvPackageInfo pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, 0);
            this.errno = pkgInfo.errno;
            if (pkgInfo.errno != 0) {
                return this.errno;
            }

            return 0;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * delete file from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @return 0 for success, none zero for fail (error code)
     */
    public int delete_file(String group_name, String remote_filename) throws IOException, MyException {
        boolean bNewConnection = this.newUpdatableStorageConnection(group_name, remote_filename);
        Socket storageSocket = this.storageServer.getSocket();

        try {
            this.send_package(ProtoCommon.STORAGE_PROTO_CMD_DELETE_FILE, group_name, remote_filename);
            ProtoCommon.RecvPackageInfo pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, 0);

            this.errno = pkgInfo.errno;
            return pkgInfo.errno;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * truncate appender file to size 0 from storage server
     *
     * @param group_name        the group name of storage server
     * @param appender_filename the appender filename
     * @return 0 for success, none zero for fail (error code)
     */
    public int truncate_file(String group_name, String appender_filename) throws IOException, MyException {
        final long truncated_file_size = 0;
        return this.truncate_file(group_name, appender_filename, truncated_file_size);
    }

    /**
     * truncate appender file from storage server
     *
     * @param group_name          the group name of storage server
     * @param appender_filename   the appender filename
     * @param truncated_file_size truncated file size
     * @return 0 for success, none zero for fail (error code)
     */
    public int truncate_file(String group_name, String appender_filename,
                             long truncated_file_size) throws IOException, MyException {
        byte[] header;
        boolean bNewConnection;
        Socket storageSocket;
        byte[] hexLenBytes;
        byte[] appenderFilenameBytes;
        int offset;
        int body_len;

        if ((group_name == null || group_name.length() == 0) ||
            (appender_filename == null || appender_filename.length() == 0)) {
            this.errno = ProtoCommon.ERR_NO_EINVAL;
            return this.errno;
        }

        bNewConnection = this.newUpdatableStorageConnection(group_name, appender_filename);

        try {
            storageSocket = this.storageServer.getSocket();

            appenderFilenameBytes = appender_filename.getBytes(ClientGlobal.g_charset);
            body_len = 2 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE + appenderFilenameBytes.length;

            header = ProtoCommon.packHeader(ProtoCommon.STORAGE_PROTO_CMD_TRUNCATE_FILE, body_len, (byte)0);
            byte[] wholePkg = new byte[header.length + body_len];
            System.arraycopy(header, 0, wholePkg, 0, header.length);
            offset = header.length;

            hexLenBytes = ProtoCommon.long2buff(appender_filename.length());
            System.arraycopy(hexLenBytes, 0, wholePkg, offset, hexLenBytes.length);
            offset += hexLenBytes.length;

            hexLenBytes = ProtoCommon.long2buff(truncated_file_size);
            System.arraycopy(hexLenBytes, 0, wholePkg, offset, hexLenBytes.length);
            offset += hexLenBytes.length;

            OutputStream out = storageSocket.getOutputStream();

            System.arraycopy(appenderFilenameBytes, 0, wholePkg, offset, appenderFilenameBytes.length);
            offset += appenderFilenameBytes.length;

            out.write(wholePkg);
            ProtoCommon.RecvPackageInfo pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, 0);
            this.errno = pkgInfo.errno;
            return pkgInfo.errno;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * download file from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @return file content/buff, return null if fail
     */
    public byte[] download_file(String group_name, String remote_filename) throws IOException, MyException {
        final long file_offset = 0;
        final long download_bytes = 0;

        return this.download_file(group_name, remote_filename, file_offset, download_bytes);
    }

    /**
     * download file from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @param file_offset     the start offset of the file
     * @param download_bytes  download bytes, 0 for remain bytes from offset
     * @return file content/buff, return null if fail
     */
    public byte[] download_file(String group_name, String remote_filename, long file_offset, long download_bytes)
        throws IOException, MyException {
        boolean bNewConnection = this.newReadableStorageConnection(group_name, remote_filename);
        Socket storageSocket = this.storageServer.getSocket();

        try {
            ProtoCommon.RecvPackageInfo pkgInfo;

            this.send_download_package(group_name, remote_filename, file_offset, download_bytes);
            //读取body的内容
            pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, -1);

            this.errno = pkgInfo.errno;
            if (pkgInfo.errno != 0) {
                return null;
            }

            return pkgInfo.body;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * download file from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @param local_filename  filename on local
     * @return 0 success, return none zero errno if fail
     */
    public int download_file(String group_name, String remote_filename,
                             String local_filename) throws IOException, MyException {
        final long file_offset = 0;
        final long download_bytes = 0;
        return this.download_file(group_name, remote_filename,
            file_offset, download_bytes, local_filename);
    }

    /**
     * download file from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @param file_offset     the start offset of the file
     * @param download_bytes  download bytes, 0 for remain bytes from offset
     * @param local_filename  filename on local
     * @return 0 success, return none zero errno if fail
     */
    public int download_file(String group_name, String remote_filename,
                             long file_offset, long download_bytes,
                             String local_filename) throws IOException, MyException {
        boolean bNewConnection = this.newReadableStorageConnection(group_name, remote_filename);
        Socket storageSocket = this.storageServer.getSocket();
        try {
            ProtoCommon.RecvHeaderInfo header;
            FileOutputStream out = new FileOutputStream(local_filename);
            try {
                this.errno = 0;
                this.send_download_package(group_name, remote_filename, file_offset, download_bytes);

                InputStream in = storageSocket.getInputStream();
                header = ProtoCommon.recvHeader(in, ProtoCommon.STORAGE_PROTO_CMD_RESP, -1);
                this.errno = header.errno;
                if (header.errno != 0) {
                    return header.errno;
                }

                byte[] buff = new byte[256 * 1024];
                long remainBytes = header.body_len;
                int bytes;

                //System.out.println("expect_body_len=" + header.body_len);

                while (remainBytes > 0) {
                    if ((bytes = in.read(buff, 0, remainBytes > buff.length ? buff.length : (int)remainBytes)) < 0) {
                        throw new IOException(
                            "recv package size " + (header.body_len - remainBytes) + " != " + header.body_len);
                    }

                    out.write(buff, 0, bytes);
                    remainBytes -= bytes;

                    //System.out.println("totalBytes=" + (header.body_len - remainBytes));
                }

                return 0;
            } catch (IOException ex) {
                if (this.errno == 0) {
                    this.errno = ProtoCommon.ERR_NO_EIO;
                }

                throw ex;
            } finally {
                out.close();
                if (this.errno != 0) {
                    (new File(local_filename)).delete();
                }
            }
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * download file from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @param callback        call callback.recv() when data arrive
     * @return 0 success, return none zero errno if fail
     */
    public int download_file(String group_name, String remote_filename,
                             DownloadCallback callback) throws IOException, MyException {
        final long file_offset = 0;
        final long download_bytes = 0;
        return this.download_file(group_name, remote_filename,
            file_offset, download_bytes, callback);
    }

    /**
     * download file from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @param file_offset     the start offset of the file
     * @param download_bytes  download bytes, 0 for remain bytes from offset
     * @param callback        call callback.recv() when data arrive
     * @return 0 success, return none zero errno if fail
     */
    public int download_file(String group_name, String remote_filename,
                             long file_offset, long download_bytes,
                             DownloadCallback callback) throws IOException, MyException {
        int result;
        boolean bNewConnection = this.newReadableStorageConnection(group_name, remote_filename);
        Socket storageSocket = this.storageServer.getSocket();

        try {
            ProtoCommon.RecvHeaderInfo header;
            this.send_download_package(group_name, remote_filename, file_offset, download_bytes);

            InputStream in = storageSocket.getInputStream();
            header = ProtoCommon.recvHeader(in, ProtoCommon.STORAGE_PROTO_CMD_RESP, -1);
            this.errno = header.errno;
            if (header.errno != 0) {
                return header.errno;
            }

            byte[] buff = new byte[2 * 1024];
            long remainBytes = header.body_len;
            int bytes;

            //System.out.println("expect_body_len=" + header.body_len);

            while (remainBytes > 0) {
                if ((bytes = in.read(buff, 0, remainBytes > buff.length ? buff.length : (int)remainBytes)) < 0) {
                    throw new IOException(
                        "recv package size " + (header.body_len - remainBytes) + " != " + header.body_len);
                }

                if ((result = callback.recv(header.body_len, buff, bytes)) != 0) {
                    this.errno = (byte)result;
                    return result;
                }

                remainBytes -= bytes;
                //System.out.println("totalBytes=" + (header.body_len - remainBytes));
            }

            return 0;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * get all metadata items from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @return meta info array, return null if fail
     */
    public NameValuePair[] get_metadata(String group_name, String remote_filename) throws IOException, MyException {
        boolean bNewConnection = this.newUpdatableStorageConnection(group_name, remote_filename);
        Socket storageSocket = this.storageServer.getSocket();

        try {
            ProtoCommon.RecvPackageInfo pkgInfo;

            this.send_package(ProtoCommon.STORAGE_PROTO_CMD_GET_METADATA, group_name, remote_filename);
            pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, -1);

            this.errno = pkgInfo.errno;
            if (pkgInfo.errno != 0) {
                return null;
            }

            return ProtoCommon.split_metadata(new String(pkgInfo.body, ClientGlobal.g_charset));
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * set metadata items to storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @param meta_list       meta item array
     * @param op_flag         flag, can be one of following values: <br>
     *                        <ul><li> ProtoCommon.STORAGE_SET_METADATA_FLAG_OVERWRITE: overwrite all old
     *                        metadata items</li></ul>
     *                        <ul><li> ProtoCommon.STORAGE_SET_METADATA_FLAG_MERGE: merge, insert when
     *                        the metadata item not exist, otherwise update it</li></ul>
     * @return 0 for success, !=0 fail (error code)
     */
    public int set_metadata(String group_name, String remote_filename,
                            NameValuePair[] meta_list, byte op_flag) throws IOException, MyException {
        boolean bNewConnection = this.newUpdatableStorageConnection(group_name, remote_filename);
        Socket storageSocket = this.storageServer.getSocket();

        try {
            //头信息
            byte[] header;
            //组名的字节数组
            byte[] groupBytes;
            //文件名的字节数组
            byte[] filenameBytes;
            //meta的字节数组
            byte[] meta_buff;
            byte[] bs;
            //组的长度
            int groupLen;
            //字节数组的长度
            byte[] sizeBytes;
            ProtoCommon.RecvPackageInfo pkgInfo;

            if (meta_list == null) {
                meta_buff = new byte[0];
            } else {
                //如果传了meta的信息
                meta_buff = ProtoCommon.pack_metadata(meta_list).getBytes(ClientGlobal.g_charset);
            }

            filenameBytes = remote_filename.getBytes(ClientGlobal.g_charset);
            sizeBytes = new byte[2 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE];
            Arrays.fill(sizeBytes, (byte)0);
            //转换为大头
            bs = ProtoCommon.long2buff(filenameBytes.length);
            //size数组的前八位是 文件名的长度
            System.arraycopy(bs, 0, sizeBytes, 0, bs.length);
            bs = ProtoCommon.long2buff(meta_buff.length);
            //接下来的八位是 meta的信息
            System.arraycopy(bs, 0, sizeBytes, ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE, bs.length);
            //16个字节长度的数组
            groupBytes = new byte[ProtoCommon.FDFS_GROUP_NAME_MAX_LEN];
            bs = group_name.getBytes(ClientGlobal.g_charset);

            Arrays.fill(groupBytes, (byte)0);
            if (bs.length <= groupBytes.length) {
                groupLen = bs.length;
            } else {
                groupLen = groupBytes.length;
            }
            //组的字节数组信息
            System.arraycopy(bs, 0, groupBytes, 0, groupLen);
            //头的长度13+16+1+groupBytes+filenameBytes+meta_buff+0
            header = ProtoCommon.packHeader(ProtoCommon.STORAGE_PROTO_CMD_SET_METADATA,
                2 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE + 1 + groupBytes.length
                    + filenameBytes.length + meta_buff.length, (byte)0);
            OutputStream out = storageSocket.getOutputStream();
            //整个包的信息 10+16+1+16+
            byte[] wholePkg = new byte[header.length + sizeBytes.length + 1 + groupBytes.length + filenameBytes.length];
            System.arraycopy(header, 0, wholePkg, 0, header.length);
            System.arraycopy(sizeBytes, 0, wholePkg, header.length, sizeBytes.length);
            wholePkg[header.length + sizeBytes.length] = op_flag;
            System.arraycopy(groupBytes, 0, wholePkg, header.length + sizeBytes.length + 1, groupBytes.length);
            System.arraycopy(filenameBytes, 0, wholePkg, header.length + sizeBytes.length + 1 + groupBytes.length,
                filenameBytes.length);
            out.write(wholePkg);
            if (meta_buff.length > 0) {
                out.write(meta_buff);
            }
            //返回的信息
            pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP, 0);

            this.errno = pkgInfo.errno;
            return pkgInfo.errno;
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * get file info decoded from the filename, fetch from the storage if necessary
     *
     * @param group_name      the group name
     * @param remote_filename the filename
     * @return FileInfo object for success, return null for fail
     */
    public FileInfo get_file_info(String group_name, String remote_filename) throws IOException, MyException {
        if (remote_filename.length() < ProtoCommon.FDFS_FILE_PATH_LEN + ProtoCommon.FDFS_FILENAME_BASE64_LENGTH
            + ProtoCommon.FDFS_FILE_EXT_NAME_MAX_LEN + 1) {
            this.errno = ProtoCommon.ERR_NO_EINVAL;
            return null;
        }

        byte[] buff = base64.decodeAuto(remote_filename.substring(ProtoCommon.FDFS_FILE_PATH_LEN,
            ProtoCommon.FDFS_FILE_PATH_LEN + ProtoCommon.FDFS_FILENAME_BASE64_LENGTH));

        long file_size = ProtoCommon.buff2long(buff, 4 * 2);
        if (((remote_filename.length() > ProtoCommon.TRUNK_LOGIC_FILENAME_LENGTH) ||
            ((remote_filename.length() > ProtoCommon.NORMAL_LOGIC_FILENAME_LENGTH) && (
                (file_size & ProtoCommon.TRUNK_FILE_MARK_SIZE) == 0))) ||
            ((file_size & ProtoCommon.APPENDER_FILE_SIZE) != 0)) { //slave file or appender file
            FileInfo fi = this.query_file_info(group_name, remote_filename);
            if (fi == null) {
                return null;
            }
            return fi;
        }

        FileInfo fileInfo = new FileInfo(file_size, 0, 0, ProtoCommon.getIpAddress(buff, 0));
        fileInfo.setCreateTimestamp(ProtoCommon.buff2int(buff, 4));
        if ((file_size >> 63) != 0) {
            file_size &= 0xFFFFFFFFL;  //low 32 bits is file size
            fileInfo.setFileSize(file_size);
        }
        fileInfo.setCrc32(ProtoCommon.buff2int(buff, 4 * 4));

        return fileInfo;
    }

    /**
     * get file info from storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @return FileInfo object for success, return null for fail
     */
    public FileInfo query_file_info(String group_name, String remote_filename) throws IOException, MyException {
        boolean bNewConnection = this.newUpdatableStorageConnection(group_name, remote_filename);
        Socket storageSocket = this.storageServer.getSocket();

        try {
            byte[] header;
            byte[] groupBytes;
            byte[] filenameBytes;
            byte[] bs;
            int groupLen;
            ProtoCommon.RecvPackageInfo pkgInfo;

            filenameBytes = remote_filename.getBytes(ClientGlobal.g_charset);
            groupBytes = new byte[ProtoCommon.FDFS_GROUP_NAME_MAX_LEN];
            bs = group_name.getBytes(ClientGlobal.g_charset);

            Arrays.fill(groupBytes, (byte)0);
            if (bs.length <= groupBytes.length) {
                groupLen = bs.length;
            } else {
                groupLen = groupBytes.length;
            }
            System.arraycopy(bs, 0, groupBytes, 0, groupLen);

            header = ProtoCommon.packHeader(ProtoCommon.STORAGE_PROTO_CMD_QUERY_FILE_INFO,
                +groupBytes.length + filenameBytes.length, (byte)0);
            OutputStream out = storageSocket.getOutputStream();
            byte[] wholePkg = new byte[header.length + groupBytes.length + filenameBytes.length];
            System.arraycopy(header, 0, wholePkg, 0, header.length);
            System.arraycopy(groupBytes, 0, wholePkg, header.length, groupBytes.length);
            System.arraycopy(filenameBytes, 0, wholePkg, header.length + groupBytes.length, filenameBytes.length);
            out.write(wholePkg);

            pkgInfo = ProtoCommon.recvPackage(storageSocket.getInputStream(),
                ProtoCommon.STORAGE_PROTO_CMD_RESP,
                3 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE +
                    ProtoCommon.FDFS_IPADDR_SIZE);

            this.errno = pkgInfo.errno;
            if (pkgInfo.errno != 0) {
                return null;
            }

            long file_size = ProtoCommon.buff2long(pkgInfo.body, 0);
            int create_timestamp = (int)ProtoCommon.buff2long(pkgInfo.body, ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE);
            int crc32 = (int)ProtoCommon.buff2long(pkgInfo.body, 2 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE);
            String source_ip_addr = (new String(pkgInfo.body, 3 * ProtoCommon.FDFS_PROTO_PKG_LEN_SIZE,
                ProtoCommon.FDFS_IPADDR_SIZE)).trim();
            return new FileInfo(file_size, create_timestamp, crc32, source_ip_addr);
        } catch (IOException ex) {
            if (!bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }

            throw ex;
        } finally {
            if (bNewConnection) {
                try {
                    this.storageServer.close();
                } catch (IOException ex1) {
                    ex1.printStackTrace();
                } finally {
                    this.storageServer = null;
                }
            }
        }
    }

    /**
     * check storage socket, if null create a new connection
     *
     * @param group_name the group name to upload file to, can be empty
     * @return true if create a new connection
     */
    protected boolean newWritableStorageConnection(String group_name) throws IOException, MyException {
        if (this.storageServer != null) {
            return false;
        } else {
            TrackerClient tracker = new TrackerClient();
            this.storageServer = tracker.getStoreStorage(this.trackerServer, group_name);
            if (this.storageServer == null) {
                throw new MyException("getStoreStorage fail, errno code: " + tracker.getErrorCode());
            }
            return true;
        }
    }

    /**
     * check storage socket, if null create a new connection
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @return true if create a new connection
     */
    protected boolean newReadableStorageConnection(String group_name, String remote_filename)
        throws IOException, MyException {
        if (this.storageServer != null) {
            return false;
        } else {
            TrackerClient tracker = new TrackerClient();
            this.storageServer = tracker.getFetchStorage(this.trackerServer, group_name, remote_filename);
            if (this.storageServer == null) {
                throw new MyException("getStoreStorage fail, errno code: " + tracker.getErrorCode());
            }
            return true;
        }
    }

    /**
     * check storage socket, if null create a new connection
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @return true if create a new connection
     */
    protected boolean newUpdatableStorageConnection(String group_name, String remote_filename)
        throws IOException, MyException {
        if (this.storageServer != null) {
            return false;
        } else {
            TrackerClient tracker = new TrackerClient();
            this.storageServer = tracker.getUpdateStorage(this.trackerServer, group_name, remote_filename);
            if (this.storageServer == null) {
                throw new MyException("getStoreStorage fail, errno code: " + tracker.getErrorCode());
            }
            return true;
        }
    }

    /**
     * send package to storage server
     *
     * @param cmd             which command to send
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     */
    protected void send_package(byte cmd, String group_name, String remote_filename) throws IOException {
        byte[] header;
        byte[] groupBytes;
        byte[] filenameBytes;
        byte[] bs;
        int groupLen;
        //groupBytes 16个字节
        groupBytes = new byte[ProtoCommon.FDFS_GROUP_NAME_MAX_LEN];
        bs = group_name.getBytes(ClientGlobal.g_charset);
        //文件名M01/00/
        filenameBytes = remote_filename.getBytes(ClientGlobal.g_charset);

        Arrays.fill(groupBytes, (byte)0);
        if (bs.length <= groupBytes.length) {
            groupLen = bs.length;
        } else {
            groupLen = groupBytes.length;
        }
        System.arraycopy(bs, 0, groupBytes, 0, groupLen);
        // body_len = 16+filenameBytes.length
        header = ProtoCommon.packHeader(cmd, groupBytes.length + filenameBytes.length, (byte)0);
        //10+16+filenameBytes.length
        byte[] wholePkg = new byte[header.length + groupBytes.length + filenameBytes.length];
        System.arraycopy(header, 0, wholePkg, 0, header.length);
        System.arraycopy(groupBytes, 0, wholePkg, header.length, groupBytes.length);
        System.arraycopy(filenameBytes, 0, wholePkg, header.length + groupBytes.length, filenameBytes.length);
        this.storageServer.getSocket().getOutputStream().write(wholePkg);
    }

    /**
     * send package to storage server
     *
     * @param group_name      the group name of storage server
     * @param remote_filename filename on storage server
     * @param file_offset     the start offset of the file
     * @param download_bytes  download bytes
     */
    protected void send_download_package(String group_name, String remote_filename, long file_offset,
                                         long download_bytes) throws IOException {
        //header为长度为10的字节数组
        byte[] header;
        //开始的字节数组
        byte[] bsOffset;
        //下载字节数组的大小
        byte[] bsDownBytes;
        //最大group长度的字节数组
        byte[] groupBytes;
        //文件名字的字节数组 /M00/00/00/ssssss.文件
        byte[] filenameBytes;
        //传进来的group字节数组
        byte[] bs;
        //group的长度
        int groupLen;
        //大头转换 长度为8的byte数组
        bsOffset = ProtoCommon.long2buff(file_offset);
        //大头转换 长度为8的byte数组
        bsDownBytes = ProtoCommon.long2buff(download_bytes);
        //长度为16的数组
        groupBytes = new byte[ProtoCommon.FDFS_GROUP_NAME_MAX_LEN];
        //将组名转换为字节数组 根据配置文件中设置的格式 group1
        bs = group_name.getBytes(ClientGlobal.g_charset);
        //将文件名转换为字节数组 /M00/00/00/ssssss.文件
        filenameBytes = remote_filename.getBytes(ClientGlobal.g_charset);
        //将group数组全部填充为0
        Arrays.fill(groupBytes, (byte)0);
        //如果group名字的数组小于最大group名的长度
        if (bs.length <= groupBytes.length) {
            //将组的长度 设置为group的字节数组长度
            groupLen = bs.length;
        } else {
            //组的长度最大为 最大组的字节长度
            groupLen = groupBytes.length;
        }
        //将group字节数组中的值copy到 最大长度的group字节数组中
        System.arraycopy(bs, 0, groupBytes, 0, groupLen);
        //header的0~7是是pkg的len 8是命令 9是状态码
        header = ProtoCommon.packHeader(ProtoCommon.STORAGE_PROTO_CMD_DOWNLOAD_FILE,
            bsOffset.length + bsDownBytes.length + groupBytes.length + filenameBytes.length, (byte)0);
        //整个pkg的长度：header的字节数长度+开始的字节数组长度+要下载的字节数组长度+最大组的长度+文件名的长度
        //可以确定的是header.length = 10 bsOffset.length = 8 bsDownBytes.length = 8 groupBytes.length = 16
        // 不确定的是 filenameBytes.length
        byte[] wholePkg = new byte[header.length + bsOffset.length + bsDownBytes.length + groupBytes.length
            + filenameBytes.length];
        //把header中的内容copy到整个pkg数组中
        System.arraycopy(header, 0, wholePkg, 0, header.length);
        //把bsOffset中的内容copy到整个pkg数组中
        System.arraycopy(bsOffset, 0, wholePkg, header.length, bsOffset.length);
        //把bsDownbytes中的内容copy到整个pkg数组中
        System.arraycopy(bsDownBytes, 0, wholePkg, header.length + bsOffset.length, bsDownBytes.length);
        //把groupBytes中的内容copy到整个pkg数组中
        System.arraycopy(groupBytes, 0, wholePkg, header.length + bsOffset.length + bsDownBytes.length,
            groupBytes.length);
        //把filenameBytes中的内容copy到整个pkg数组中
        System.arraycopy(filenameBytes, 0, wholePkg,
            header.length + bsOffset.length + bsDownBytes.length + groupBytes.length, filenameBytes.length);
        //把请求内容发送到存储服务器
        this.storageServer.getSocket().getOutputStream().write(wholePkg);
    }
}
