package org.csource.fastdfs.test;

import java.io.File;
import java.io.InputStream;

import org.csource.fastdfs.FastDFSClient;
import org.apache.commons.io.FileUtils;

/**
 * 
 * @描述: FastDFS测试 .
 * @作者: WuShuicheng .
 * @创建时间: 2015-3-29,下午8:11:36 .
 * @版本号: V1.0 .
 */
public class FastDFSTest {
	
	/**
	 * 上传测试.
	 * @throws Exception
	 */
	@org.junit.Test
	public void upload() throws Exception {
		System.out.println(System.getProperty("user.dir"));
		String filePath = "src\\main\\resources\\image01.jpg";
		File file = new File(filePath);
		String fileId = FastDFSClient.uploadFile(file, filePath);
		System.out.println("Upload local file " + filePath + " ok, fileid=" + fileId);
		// fileId:	group1/M00/00/00/wKgEfVUYPieAd6a0AAP3btxj__E335.jpg
		//group1/M00/00/00/wKi0hVjqXGeAcyFfAAGSt-FxG-0872.jpg
		//group1/M00/02/8F/rBH7Y1jqXJqATLrfAAGSt-FxG-0932.jpg
		// url:	http://192.168.4.125:8888/group1/M00/00/00/wKgEfVUYPieAd6a0AAP3btxj__E335.jpg
	}
	
	/**
	 * 下载测试.
	 * @throws Exception
	 */
	public static void download() throws Exception {
		String fileId = "group1/M00/00/00/wKgEfVUYPieAd6a0AAP3btxj__E335.jpg";
		InputStream inputStream = FastDFSClient.downloadFile(fileId);
		File destFile = new File("E:/WorkSpaceSpr10.6/edu-demo-fdfs/TestFile/DownloadTest.jpg");
		FileUtils.copyInputStreamToFile(inputStream, destFile);
	}

	/**
	 * 删除测试
	 * @throws Exception
	 */
	public static void delete() throws Exception {
		String fileId = "group1/M00/00/00/wKgEfVUYPieAd6a0AAP3btxj__E335.jpg";
		int result = FastDFSClient.deleteFile(fileId);
		System.out.println(result == 0 ? "删除成功" : "删除失败:" + result);
	}


	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//upload();
		//download();
		delete();

	}

}