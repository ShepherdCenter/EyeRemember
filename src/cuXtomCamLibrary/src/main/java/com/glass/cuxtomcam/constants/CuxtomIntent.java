package com.glass.cuxtomcam.constants;

/**
 * This class provides the intent extras that are used by CuXtomCam
 * 
 * @author Sheraz Ahmad Khilji <br>
 *         Developed by Virtual Force <br>
 *         {@link http://www.virtual-force.com/}<br>
 * <br>
 *         on Apr 1, 2014 at 10:09:01 AM
 */
public class CuxtomIntent {
	/**
	 * Private Constructor
	 */
	private CuxtomIntent() {
	}

	/**
	 * Use this as Intent extra KEY in {@link OnActivityResult()} when you want
	 * to check which type of file was saved by CuXtomCam.
	 */
	public static final String FILE_TYPE = "file_type";
	/**
	 * Use this as Intent extra KEY in {@link OnActivityResult()} when you want
	 * to extract the complete path of the picture or video that has been taken.
	 */
	public static final String FILE_PATH = "file_path";
	/**
	 * Use this as Intent extra KEY when you want enable zoom functionality in
	 * the app using <b>SWIPE LEFT</b> and <b> SWIPE RIGHT</b> Gestures.<br>
	 * The value for this key would be of type <b>boolean</b>.<br>
	 * <br>
	 * By default the zoom functionality is enabled in the app. So use this
	 * key/value pair if you want to change that behaviour
	 */
	public static final String ENABLE_ZOOM = "enable_zoom";
	/**
	 * Use this as Intent extra KEY when you want to specify name of the file
	 * that will be saved. The extension is generated by default so you should
	 * specify just the filename and (.jpeg or .mp4) will automatically be
	 * appended at the end of the file name.<br>
	 * The value for this key would be of type <b>String</b>.<br>
	 * <br>
	 * Not sending this key/value pair would generate a random filename
	 */
	public static final String FILE_NAME = "file_name";
	/**
	 * Use this as Intent extra KEY when you want to specify the complete path
	 * where the video or picture will be saved. To specify filename use the key
	 * {@link FILE_NAME}.<br>
	 * The value for this key would be of type <b>String</b>.<br>
	 * <br>
	 * Not sending this key/value pair would save the files in Pictures
	 * directory CuXtomCamera Folder
	 */
	public static final String FOLDER_PATH = "folder_path";
	/**
	 * Use this as Intent extra KEY when you want to specify duration of video
	 * recording.<br>
	 * The value for this key would be of type <b>int</b>.<br>
	 * <br>
	 * Not sending this key/value pair would set the video duration to 1 hour
	 */
	public static final String VIDEO_DURATION = "video_duration";
	/**
	 * Use this as Intent extra KEY when you want to open Camera in picture or
	 * video recording mode. <br>
	 * The value for this key would be either {@link PHOTO_MODE} or
	 * {@link VIDEO_MODE}.<br>
	 * <br>
	 * Not sending this key/value pair would open the camera in
	 * {@link PHOTO_MODE}.
	 */
	public static final String CAMERA_MODE = "camera_mode";

	public enum CAMERA_MODE {
		;
		/**
		 * 
		 * Use this as Intent extra VALUE when you want to open Camera in Photo
		 * mode. Key used For this value will be {@link CAMERA_MODE}
		 */
		public static final int PHOTO_MODE = 1;
		/**
		 * 
		 * Use this as Intent extra VALUE when you want to open Camera in Video
		 * Recording mode. Key used For this value will be {@link CAMERA_MODE}
		 */
		public static final int VIDEO_MODE = 2;
	}

	public enum FILE_TYPE {
		;
		/**
		 * The path returned by {@link FILE_PATH} leads to a picture
		 */
		public static final int PHOTO = 11;
		/**
		 * 
		 The path returned by {@link FILE_PATH} leads to a Video
		 */
		public static final int VIDEO = 22;
	}

}
