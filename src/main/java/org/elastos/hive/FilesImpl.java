package org.elastos.hive;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.elastos.hive.connection.ConnectionManager;
import org.elastos.hive.exception.FileNotFoundException;
import org.elastos.hive.exception.HiveException;
import org.elastos.hive.files.FileInfo;
import org.elastos.hive.files.FilesList;
import org.elastos.hive.files.UploadOutputStream;
import org.elastos.hive.utils.JsonUtil;
import org.elastos.hive.utils.ResponseHelper;

import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

class FilesImpl implements Files {
	private AuthHelper authHelper;
	private ConnectionManager connectionManager;

	FilesImpl(AuthHelper authHelper) {
		this.authHelper = authHelper;
		this.connectionManager = authHelper.getConnectionManager();
	}

	@Override
	public <T> CompletableFuture<T> upload(String path, Class<T> resultType) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> {
			try {
				return uploadImpl(path, resultType);
			} catch (HiveException e) {
				throw new CompletionException(e);
			}
		});
	}

	private <T> T uploadImpl(String path, Class<T> resultType) throws HiveException {
		try {
			HttpURLConnection connection = this.connectionManager.openURLConnection("/files/upload/" + path);
			OutputStream outputStream = connection.getOutputStream();

			if(resultType.isAssignableFrom(OutputStream.class)) {
				UploadOutputStream uploader = new UploadOutputStream(connection, outputStream);
				return resultType.cast(uploader);
			} else if (resultType.isAssignableFrom(OutputStreamWriter.class)) {
				OutputStreamWriter writer = new OutputStreamWriter(outputStream);
				return resultType.cast(writer);
			} else {
				throw new HiveException("Not supported result type");
			}
		} catch (IOException e) {
			throw new HiveException(e.getLocalizedMessage());
		}
	}

	@Override
	public <T> CompletableFuture<T> download(String path, Class<T> resultType) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> downloadImpl(path, resultType));
	}

	private <T> T downloadImpl(String remoteFile, Class<T> resultType) {
		try {
			Response<ResponseBody> response;

			response = this.connectionManager.getFileApi()
					.downloader(remoteFile)
					.execute();
			int code = response.code();
			if(404 == code) {
				throw new FileNotFoundException(FileNotFoundException.EXCEPTION);
			}

			authHelper.checkResponseWithRetry(response);

			if(resultType.isAssignableFrom(Reader.class)) {
				Reader reader = ResponseHelper.getToReader(response);
				return resultType.cast(reader);
			}
			if (resultType.isAssignableFrom(InputStream.class)){
				InputStream inputStream = ResponseHelper.getInputStream(response);
				return resultType.cast(inputStream);
			}

			throw new HiveException("Not supported result type");
		} catch (HiveException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public CompletableFuture<Boolean> delete(String remoteFile) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> {
			try {
				return deleteImpl(remoteFile);
			} catch (HiveException e) {
				throw new CompletionException(e);
			}
		});
	}

	private Boolean deleteImpl(String remoteFile) throws HiveException {
		try {
			Map<String, String> map = new HashMap<>();
			map.put("path", remoteFile);

			String json = JsonUtil.serialize(map);
			Response<ResponseBody> response;

			response = this.connectionManager.getFileApi()
					.deleteFolder(createJsonRequestBody(json))
					.execute();
			authHelper.checkResponseWithRetry(response);
			return true;
		} catch (Exception e) {
			throw new HiveException(e.getLocalizedMessage());
		}
	}

	@Override
	public CompletableFuture<Boolean> move(String source, String dest) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> {
			try {
				return moveImpl(source, dest);
			} catch (HiveException e) {
				throw new CompletionException(e);
			}
		});
	}

	private Boolean moveImpl(String source, String dest) throws HiveException {
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("src_path", source);
			map.put("dst_path", dest);

			String json = JsonUtil.serialize(map);
			Response<ResponseBody> response;

			response = this.connectionManager.getFileApi()
					.move(createJsonRequestBody(json))
					.execute();
			authHelper.checkResponseWithRetry(response);
			return true;
		} catch (Exception e) {
			throw new HiveException(e.getLocalizedMessage());
		}
	}

	@Override
	public CompletableFuture<Boolean> copy(String source, String dest) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> {
			try {
				return copyImpl(source, dest);
			} catch (HiveException e) {
				throw new CompletionException(e);
			}
		});
	}

	private Boolean copyImpl(String source, String dest) throws HiveException {
		try {
			Map<String, Object> map = new HashMap<>();
			map.put("src_path", source);
			map.put("dst_path", dest);

			String json = JsonUtil.serialize(map);
			Response<ResponseBody> response;

			response = this.connectionManager.getFileApi()
					.copy(createJsonRequestBody(json))
					.execute();
			authHelper.checkResponseWithRetry(response);
			return true;
		} catch (Exception e) {
			throw new HiveException(e.getLocalizedMessage());
		}
	}

	@Override
	public CompletableFuture<String> hash(String remoteFile) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> {
			try {
				return hashImp(remoteFile);
			} catch (HiveException e) {
				throw new CompletionException(e);
			}
		});
	}

	private String hashImp(String remoteFile) throws HiveException {
		try {
			Response response = this.connectionManager.getFileApi()
					.hash(remoteFile)
					.execute();
			authHelper.checkResponseWithRetry(response);
			JsonNode ret = ResponseHelper.getValue(response, JsonNode.class);
			return ret.get("SHA256").toString();
		} catch (Exception e) {
			throw new HiveException(e.getLocalizedMessage());
		}
	}

	@Override
	public CompletableFuture<List<FileInfo>> list(String folder) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> {
			try {
				return listImpl(folder);
			} catch (HiveException e) {
				throw new CompletionException(e);
			}
		});
	}

	private List<FileInfo> listImpl(String folder) throws HiveException {
		try {
			Response<FilesList> response = this.connectionManager.getFileApi()
					.files(folder).execute();

			authHelper.checkResponseWithRetry(response);
			return response.body().getFiles();
		} catch (Exception e) {
			throw new HiveException(e.getLocalizedMessage());
		}
	}

	@Override
	public CompletableFuture<FileInfo> stat(String path) {
		return authHelper.checkValid().thenApplyAsync(aVoid -> {
			try {
				return statImpl(path);
			} catch (HiveException e) {
				throw new CompletionException(e);
			}
		});
	}

	private FileInfo statImpl(String path) throws HiveException {
		try {
			Response<FileInfo> response = this.connectionManager.getFileApi()
					.getProperties(path).execute();

			authHelper.checkResponseWithRetry(response);
			return response.body();
		} catch (Exception e) {
			throw new HiveException(e.getLocalizedMessage());
		}
	}

	private RequestBody createJsonRequestBody(String json) {
		return RequestBody.create(MediaType.parse("Content-Type, application/json"), json);
	}
}
